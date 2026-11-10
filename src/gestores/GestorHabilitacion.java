package gestores;

import aplicacion.*;
import gestores.GestorAcceso.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.partidos.*;
import modelo.usuarios.Jugador;

public class GestorHabilitacion {

    public record Clave(String ambito, String jugadorId) {
    }

    public record Suspension(String id, String ambito, String jugadorId, String equipoOrigenId, String partidoOrigenId,
            String partidoCumplimientoId, String equipoCumplimientoId) {
    }

    public record Excluido(String jugadorId, String nombre, String motivo) {
    }

    public record Disponibilidad(String equipoId, int minimo, List<Jugador> habilitados, List<Excluido> excluidos,
            boolean partidoProgramado) {
        public Disponibilidad {
            habilitados = List.copyOf(habilitados);
            excluidos = List.copyOf(excluidos);
        }

        public int cantidad() {
            return habilitados.size();
        }

        public boolean puedeJugar() {
            return partidoProgramado && minimo > 0 && cantidad() >= minimo;
        }
    }

    private final EstadoClub club;
    private final Map<Clave, Integer> amarillas = new LinkedHashMap<>();
    private final Map<String, Suspension> suspensiones = new LinkedHashMap<>();

    public GestorHabilitacion(EstadoClub club) {
        this.club = club;
    }

    private boolean autorizado(Sesion s) {
        return (club.getAcceso().tienePerfil(s, Perfil.ADMINISTRADOR)
                || club.getAcceso().tienePerfil(s, Perfil.ENTRENADOR));
    }

    public String ambito(Partido p) {
        if (p instanceof PartidoCompetencia c) {
            return "COMP:" + c.getCompetencia().getId();
        } else {
            return "INTERNO:" + p.getDeporte().getId();
        }
    }

    public List<Suspension> getSuspensiones() {
        return List.copyOf(suspensiones.values());
    }

    public Map<Clave, Integer> getAmarillas() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(amarillas));
    }

    public List<Jugador> candidatos(Partido p, Equipo equipo) {
        if (p instanceof PartidoCompetencia c) {
            NominaCompetencia n = club.getCompetencias().buscarNomina(c.getCompetencia().getId(), equipo.getId());
            if (n == null) {
                return List.of();
            } else {
                return n.getJugadoresActuales();
            }
        }
        return equipo.getInscripciones().stream().map(InscripcionEquipo::getJugador).toList();
    }

    private boolean suspendido(Map<String, Suspension> historial, String jugadorId, Partido p) {
        for (Suspension s : historial.values()) {
            if (s.jugadorId().equals(jugadorId) && s.ambito().equals(ambito(p))) {
                Partido origen = club.getPartidos().buscarPartido(s.partidoOrigenId());
                if (origen == null || !p.getFechaHora().isAfter(origen.getFechaHora())) {
                    continue;
                }
                Partido cumplimiento;
                if (s.partidoCumplimientoId() == null) {
                    cumplimiento = null;
                } else {
                    cumplimiento = club.getPartidos().buscarPartido(s.partidoCumplimientoId());
                }

                if (cumplimiento == null || !p.getFechaHora().isAfter(cumplimiento.getFechaHora())) {
                    return true;
                }
            }
        }
        return false;
    }

    public long contarHabilitadosNomina(NominaCompetencia nomina, java.time.LocalDateTime fecha) {
        PartidoCompetencia referencia = new PartidoCompetencia("CONSULTA", fecha, nomina.getCompetencia().getDeporte(),
                nomina.getCompetencia(), nomina.getEquipo(), "Consulta", "Consulta");
        return nomina.getJugadoresActuales().stream()
                .filter(j -> j.estaActivo() && nomina.getEquipo().contieneJugador(j)
                        && !club.getAcceso().tienePerfil(j.getId(), Perfil.ADMINISTRADOR)
                        && !suspendido(suspensiones, j.getId(), referencia))
                .count();
    }

    public String motivo(Partido p, Equipo equipo, Jugador j) {
        if (j == null || club.buscarJugador(j.getId()) != j) {
            return "Jugador inexistente o no canónico.";
        }

        if (p instanceof PartidoCompetencia && !candidatos(p, equipo).contains(j)) {
            return "Jugador fuera de la nómina del torneo.";
        }

        if (!equipo.contieneJugador(j)) {
            return "Jugador retirado o ajeno al equipo.";
        }

        if (!equipo.estaActivo()) {
            return "Equipo dado de baja.";
        }

        if (!j.estaActivo()) {
            return "Jugador dado de baja.";
        }

        if (club.getAcceso().tienePerfil(j.getId(), Perfil.ADMINISTRADOR)) {
            return "Administrador no habilitado para participar.";
        }

        if (suspendido(suspensiones, j.getId(), p)) {
            return ("Suspensión vigente en " + ambito(p) + ".");
        }
        return null;
    }

    public Disponibilidad consultar(Sesion sesion, String partidoId, String equipoId) {
        if (!autorizado(sesion)) {
            throw new IllegalArgumentException("Solo entrenador/administrador consulta habilitación.");
        }
        Partido p = club.getPartidos().buscarPartido(partidoId);
        Equipo equipo = club.getEquipos().buscarEquipo(equipoId);
        if (p == null || equipo == null || !p.getEquiposClub().contains(equipo)) {
            throw new IllegalArgumentException("Partido/equipo inexistentes o incompatibles.");
        }
        return disponibilidad(p, equipo);
    }

    private Disponibilidad disponibilidad(Partido p, Equipo equipo) {
        List<Jugador> disponibles = new ArrayList<>();
        List<Excluido> excluidos = new ArrayList<>();
        for (Jugador j : candidatos(p, equipo)) {
            String causa = motivo(p, equipo, j);
            if (causa == null) {
                disponibles.add(j);
            } else {
                excluidos.add(new Excluido(j.getId(), j.getNombre(), causa));
            }
        }
        return new Disponibilidad(equipo.getId(), p.getDeporte().getMinimoJugadores(), disponibles, excluidos,
                !p.estaCancelado() && !p.estaCerrado() && !p.tieneResultado());
    }

    public ResultadoOperacion alinear(Sesion sesion, String partidoId, String equipoId, List<String> ids) {
        if (!autorizado(sesion)) {
            return ResultadoOperacion.rechazo("Sin permiso para confirmar alineaciones.");
        }
        Partido p = club.getPartidos().buscarPartido(partidoId);
        Equipo equipo = club.getEquipos().buscarEquipo(equipoId);
        if (p == null || equipo == null || !p.getEquiposClub().contains(equipo) || ids == null
                || ids.size() != new HashSet<>(ids).size()) {
            return ResultadoOperacion.rechazo("Partido/equipo/alineación inválidos o IDs repetidos.");
        }

        if (p.tieneResultado() || p.estaCancelado() || p.estaCerrado()) {
            return ResultadoOperacion.rechazo("La alineación ya no puede modificarse.");
        }
        List<Jugador> seleccion = new ArrayList<>();
        for (String id : ids) {
            Jugador j = club.buscarJugador(id);
            String causa = motivo(p, equipo, j);
            if (causa != null) {
                return ResultadoOperacion.rechazo(id + ": " + causa);
            }
            seleccion.add(j);
        }
        int minimo = p.getDeporte().getMinimoJugadores();
        if (minimo == 0 || seleccion.size() < minimo) {
            return ResultadoOperacion.rechazo("Alineación insuficiente: mínimo " + minimo + " habilitados.");
        }
        java.util.Set<String> usuarios = new java.util.HashSet<>(ids);
        for (Equipo otro : p.getEquiposClub()) {
            if (otro != equipo) {
                if (!p.getParticipantes(otro.getId()).isEmpty()) {
                    p.getParticipantes(otro.getId()).forEach(j -> usuarios.add(j.getId()));
                } else {
                    otro.getInscripciones().forEach(i -> usuarios.add(i.getJugador().getId()));
                }
            }
        }
        if (p.getFin().isAfter(p.getInicio())) {
            String causa = club.getAgenda().validar(p, p.getInstalacion(), p.getInicio(), p.getFin(), usuarios);
            if (causa != null) {
                return ResultadoOperacion.rechazo(causa);
            }
        }

        if (p.definirSeleccion(equipoId, seleccion)) {
            return ResultadoOperacion.correcto("Alineación confirmada sin cambios parciales.");
        } else {
            return ResultadoOperacion.rechazo("Alineación rechazada por estado del partido.");
        }
    }

    private ResultadoOperacion ordenValido(Partido p) {
        for (Partido anterior : club.getPartidos().getPartidos()) {
            if (anterior != p && !anterior.esLegado() && !anterior.estaCancelado()
                    && ambito(anterior).equals(ambito(p))) {
                if (anterior.estaCerrado() && (p.getFechaHora().isBefore(anterior.getFechaHora())
                        || (p.getFechaHora().equals(anterior.getFechaHora())
                                && anterior.getEquiposClub().stream().anyMatch(p.getEquiposClub()::contains)))) {
                    return ResultadoOperacion.rechazo(
                            "No se registran actas/resultados retroactivos antes del último cierre aplicable.");
                }

                if (anterior.tieneResultado() && !anterior.estaCerrado()
                        && anterior.getFechaHora().isBefore(p.getFechaHora())) {
                    return ResultadoOperacion.rechazo("Cierre primero el acta pendiente " + anterior.getId() + ".");
                }
            }
        }
        return ResultadoOperacion.correcto("Orden válido.");
    }

    public ResultadoOperacion validarResultado(Partido p) {
        if (p == null || p.getDeporte() == null || p.getDeporte().getMinimoJugadores() == 0
                || p.getEquiposClub().isEmpty() || p.estaCancelado() || p.estaCerrado() || p.tieneResultado()) {
            return ResultadoOperacion.rechazo("Partido cancelado, cerrado, inexistente o con resultado previo.");
        }
        ResultadoOperacion orden = ordenValido(p);
        if (!orden.exito()) {
            return orden;
        }
        for (Equipo equipo : p.getEquiposClub()) {
            if (!equipo.estaActivo()) {
                return ResultadoOperacion.rechazo("Equipo dado de baja: " + equipo.getId());
            }
            Disponibilidad d = disponibilidad(p, equipo);
            if (!d.puedeJugar()) {
                return ResultadoOperacion.rechazo(equipo.getId() + ": " + d.cantidad() + " habilitados; mínimo "
                        + d.minimo() + ". Excluidos: " + d.excluidos());
            }
            List<Jugador> seleccion = p.getParticipantes(equipo.getId());
            if (p instanceof PartidoCompetencia && seleccion.isEmpty()) {
                return ResultadoOperacion.rechazo("Confirme la alineación oficial antes del resultado.");
            }

            if (!seleccion.isEmpty()) {
                if (seleccion.size() < d.minimo()) {
                    return ResultadoOperacion.rechazo("Alineación por debajo del mínimo.");
                }
                for (Jugador j : seleccion) {
                    String causa = motivo(p, equipo, j);
                    if (causa != null) {
                        return ResultadoOperacion.rechazo(j.getId() + ": " + causa);
                    }
                }
            }
        }
        return ResultadoOperacion.correcto("Partido habilitado.");
    }

    public void confirmarPlantillas(Partido p) {
        for (Equipo e : p.getEquiposClub()) {
            List<Jugador> plantilla = candidatos(p, e).stream().filter(e::contieneJugador).toList();
            p.restaurarPlantilla(e.getId(), plantilla);
            if (p.getParticipantes(e.getId()).isEmpty()) {
                p.restaurarSeleccion(e.getId(), disponibilidad(p, e).habilitados());
            }
        }
    }

    public ResultadoOperacion cancelar(Sesion sesion, String id) {
        if (!autorizado(sesion)) {
            return ResultadoOperacion.rechazo("Sin permiso para cancelar partidos.");
        }
        Partido p = club.getPartidos().buscarPartido(id);
        if (p == null) {
            return ResultadoOperacion.rechazo("Partido inexistente.");
        } else {
            return club.getAgenda().cambiar(sesion, p, p.getInstalacion(), p.getInicio(), p.getFin(),
                    "Cancelación de partido", true);
        }
    }

    public ResultadoOperacion cerrar(Sesion sesion, String id) {
        if (!autorizado(sesion)) {
            return ResultadoOperacion.rechazo("Sin permiso para cerrar actas.");
        }
        Partido p = club.getPartidos().buscarPartido(id);
        if (p == null || !p.tieneResultado() || p.estaCerrado() || p.estaCancelado() || p.esLegado()
                || p instanceof PartidoAcademia) {
            return ResultadoOperacion
                    .rechazo("Acta sin resultado, ya cerrada, cancelada o legado sin alineación histórica.");
        }
        ResultadoOperacion orden = ordenValido(p);
        if (!orden.exito()) {
            return orden;
        }
        Map<Clave, Integer> nuevosContadores = new LinkedHashMap<>(amarillas);
        Map<String, Suspension> nuevas = new LinkedHashMap<>(suspensiones);
        try {
            aplicar(p, nuevosContadores, nuevas);
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
        if (!p.cerrarActa()) {
            return ResultadoOperacion.rechazo("Estado de acta cambió.");
        }
        amarillas.clear();
        amarillas.putAll(nuevosContadores);
        suspensiones.clear();
        suspensiones.putAll(nuevas);
        return ResultadoOperacion.correcto("Acta cerrada; disciplina aplicada una sola vez.");
    }

    private String idSuspension(Partido p, String jugador) {
        String origen = p.getId().length() + ":" + p.getId() + jugador.length() + ":" + jugador;
        return UUID.nameUUIDFromBytes(origen.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void aplicar(Partido p, Map<Clave, Integer> contadores, Map<String, Suspension> historial) {
        for (Equipo e : p.getEquiposClub()) {
            List<Jugador> plantilla = p.getPlantillas().get(e.getId()), seleccion = p.getParticipantes(e.getId());
            if (plantilla == null || seleccion.size() < p.getDeporte().getMinimoJugadores()
                    || !plantilla.containsAll(seleccion)) {
                throw new IllegalArgumentException("Falta snapshot íntegro de plantilla/alineación.");
            }
            for (Jugador j : seleccion) {
                if (suspendido(historial, j.getId(), p)) {
                    throw new IllegalArgumentException(j.getId() + ": alineación contiene un suspendido.");
                }
            }
        }
        for (Suspension suspension : new ArrayList<>(historial.values())) {
            if (suspension.partidoCumplimientoId() == null && suspension.ambito().equals(ambito(p))) {
                Partido origen = club.getPartidos().buscarPartido(suspension.partidoOrigenId());
                if (origen == null || !p.getFechaHora().isAfter(origen.getFechaHora())) {
                    continue;
                }
                for (var equipo : p.getPlantillas().entrySet()) {
                    if (equipo.getValue().stream().anyMatch(j -> j.getId().equals(suspension.jugadorId()))) {
                        historial.put(suspension.id(),
                                new Suspension(suspension.id(), suspension.ambito(), suspension.jugadorId(),
                                        suspension.equipoOrigenId(), suspension.partidoOrigenId(), p.getId(),
                                        equipo.getKey()));
                        break;
                    }
                }
            }
        }
        Map<Jugador, List<Sancion>> tarjetas = new LinkedHashMap<>();
        for (Sancion s : p.getSanciones()) {
            if (!p.esParticipante(s.getJugador())) {
                throw new IllegalArgumentException("Tarjeta de jugador no alineado.");
            }
            tarjetas.computeIfAbsent(s.getJugador(), k -> new ArrayList<>()).add(s);
        }
        for (var entrada : tarjetas.entrySet()) {
            Jugador j = entrada.getKey();
            Clave clave = new Clave(ambito(p), j.getId());
            boolean roja = entrada.getValue().stream().anyMatch(s -> s.getTipo() == TipoTarjeta.ROJA);
            int acumuladas = contadores.getOrDefault(clave, 0)
                    + (int) entrada.getValue().stream().filter(s -> s.getTipo() == TipoTarjeta.AMARILLA).count();
            if (roja || acumuladas >= 2) {
                String equipo = p.getSelecciones().entrySet().stream().filter(e -> e.getValue().contains(j))
                        .map(Map.Entry::getKey).findFirst().orElseThrow();
                String id = idSuspension(p, j.getId());
                if (historial.containsKey(id)) {
                    throw new IllegalArgumentException("Sanción ya aplicada.");
                }
                historial.put(id, new Suspension(id, ambito(p), j.getId(), equipo, p.getId(), null, null));
                if (roja) {
                    acumuladas = 0;
                } else {
                    acumuladas = acumuladas - 2;
                }
            }

            if (acumuladas == 0) {
                contadores.remove(clave);
            } else {
                contadores.put(clave, acumuladas);
            }
        }
    }

    public void cargarDisciplina(Map<Clave, Integer> contadores, List<Suspension> historial) {
        if (!amarillas.isEmpty() || !suspensiones.isEmpty()) {
            throw new IllegalArgumentException("Disciplina ya cargada.");
        }
        for (var e : contadores.entrySet()) {
            if (e.getValue() == null || e.getValue() != 1 || club.buscarJugador(e.getKey().jugadorId()) == null) {
                throw new IllegalArgumentException("Contador disciplinario inválido.");
            }
        }
        for (Suspension s : historial) {
            if (s == null || suspensiones.putIfAbsent(s.id(), s) != null) {
                throw new IllegalArgumentException("Suspensión duplicada.");
            }
        }
        amarillas.putAll(contadores);
        validarHistorial();
    }

    public void validarHistorial() {
        Map<Clave, Integer> calculados = new LinkedHashMap<>();
        Map<String, Suspension> historial = new LinkedHashMap<>();
        club.getPartidos().getPartidos().stream().filter(p -> p.estaCerrado() && !(p instanceof PartidoAcademia))
                .sorted(Comparator.comparing(Partido::getFechaHora).thenComparing(Partido::getId))
                .forEach(p -> aplicar(p, calculados, historial));
        if (!amarillas.equals(calculados) || !suspensiones.equals(historial)) {
            throw new IllegalArgumentException("Disciplina no coincide con las actas cerradas.");
        }
    }
}
