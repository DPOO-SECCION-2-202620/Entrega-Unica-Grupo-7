package gestores;

import aplicacion.*;
import gestores.GestorAcceso.*;
import java.time.*;
import java.util.*;
import modelo.deportes.Deporte;
import modelo.entrenamientos.Entrenamiento;
import modelo.partidos.Partido;
import modelo.reservas.*;

public class GestorAgendaClub {

    private final EstadoClub club;
    private final Map<String, Integer> maximos = new LinkedHashMap<>();

    public GestorAgendaClub(EstadoClub club) {
        this.club = club;
    }

    public Map<String, Integer> getMaximos() {
        return Map.copyOf(maximos);
    }

    public List<ActividadAgenda> getActividades() {
        List<ActividadAgenda> resultado = new ArrayList<>(club.getReservas().getReservas());
        resultado.addAll(club.getEntrenamientos().getEntrenamientos());
        resultado.addAll(club.getPartidos().getPartidos());
        return List.copyOf(resultado);
    }

    public int getMaximo(String deporte) {
        String id = Deporte.clave(deporte);
        int fisicas = (int) club.getReservas().getInstalaciones().stream().filter(i -> i.getDeporte().es(id)).count();
        return Math.min(fisicas, maximos.getOrDefault(id, fisicas));
    }

    private Deporte deporte(ActividadAgenda a) {
        if (a instanceof Partido p) {
            return p.getDeporte();
        } else if (a instanceof Entrenamiento e) {
            if (e.getDisciplina() == null) {
                return e.getInstalacion().getDeporte();
            } else {
                return e.getDisciplina();
            }
        } else {
            return a.getInstalacion().getDeporte();
        }
    }

    public String validar(ActividadAgenda a) {
        return validar(a, a.getInstalacion(), a.getInicio(), a.getFin(), a.getUsuariosAgenda());
    }

    public String validar(ActividadAgenda a, Instalacion destino, LocalDateTime inicio, LocalDateTime fin,
            Set<String> usuarios) {
        if (a == null || inicio == null || fin == null || !fin.isAfter(inicio)) {
            return "Intervalo de actividad inválido.";
        }
        Deporte dep = deporte(a);
        if (destino != null
                && (club.getReservas().buscarInstalacion(destino.getId()) != destino || destino.getDeporte() != dep)) {
            return "Instalación inexistente o de otra disciplina.";
        }

        if (destino != null) {
            int cantidad;
            if (a instanceof Reserva r) {
                cantidad = r.getCantidadParticipantes();
            } else if (a instanceof modelo.partidos.PartidoAcademia pa) {
                cantidad = pa.getJugadores().size();
            } else if (a instanceof Entrenamiento e) {
                cantidad = e.getConvocados().size();
            } else {
                cantidad = 2 * dep.getMinimoJugadores();
            }

            if (cantidad > destino.getCapacidadMaxima()) {
                return "La instalación no admite la cantidad de participantes de la actividad.";
            }
        }

        if (destino != null && !destino.getAgenda().estaDisponible(inicio, fin, a)) {
            return "No hay disponibilidad en esa instalación.";
        }
        for (ActividadAgenda otra : getActividades()) {
            if (otra != a && !otra.estaCancelada() && otra.getFin().isAfter(otra.getInicio())
                    && inicio.isBefore(otra.getFin()) && fin.isAfter(otra.getInicio())
                    && otra.getUsuariosAgenda().stream().anyMatch(usuarios::contains)) {
                return ("Usuario/entrenador ocupado en otra actividad: " + otra.getId() + ".");
            }
        }
        if (destino != null && excede(dep, inicio, fin, a, destino, getMaximo(dep.getId()))) {
            return "Máximo simultáneo de " + dep.getNombre() + " alcanzado.";
        }
        return null;
    }

    private boolean excede(Deporte dep, LocalDateTime inicio, LocalDateTime fin, ActividadAgenda ignorar,
            Instalacion agregar, int maximo) {
        List<ActividadAgenda> otras = getActividades().stream()
                .filter(a -> a != ignorar && !a.estaCancelada() && a.getInstalacion() != null
                        && a.getInstalacion().getDeporte() == dep && a.getInicio().isBefore(fin)
                        && a.getFin().isAfter(inicio))
                .toList();
        SortedSet<LocalDateTime> limites = new TreeSet<>(List.of(inicio, fin));
        for (ActividadAgenda a : otras) {
            if (a.getInicio().isAfter(inicio)) {
                limites.add(a.getInicio());
            }

            if (a.getFin().isBefore(fin)) {
                limites.add(a.getFin());
            }
        }
        List<LocalDateTime> puntos = new ArrayList<>(limites);
        for (int i = 0; i < puntos.size() - 1; i++) {
            LocalDateTime desde = puntos.get(i), hasta = puntos.get(i + 1);
            Set<Instalacion> ocupadas = new HashSet<>();
            if (agregar != null) {
                ocupadas.add(agregar);
            }
            for (ActividadAgenda a : otras) {
                if (a.getInicio().isBefore(hasta) && a.getFin().isAfter(desde)) {
                    ocupadas.add(a.getInstalacion());
                }
            }
            if (ocupadas.size() > maximo) {
                return true;
            }
        }
        return false;
    }

    public ResultadoOperacion configurarMaximo(Sesion admin, String disciplina, int maximo) {
        Deporte d = club.buscarDeporte(disciplina);
        if (!club.getAcceso().tienePerfil(admin, Perfil.ADMINISTRADOR) || d == null || maximo < 1
                || maximo > club.getReservas().getInstalaciones().stream().filter(i -> i.getDeporte() == d).count()) {
            return ResultadoOperacion.rechazo("Administrador, disciplina o máximo inválidos.");
        }
        for (ActividadAgenda a : getActividades()) {
            if (!a.estaCancelada() && a.getInstalacion() != null && a.getInstalacion().getDeporte() == d
                    && excede(d, a.getInicio(), a.getFin(), null, null, maximo)) {
                return ResultadoOperacion.rechazo("El máximo propuesto contradice actividades ya registradas.");
            }
        }
        maximos.put(d.getId(), maximo);
        return ResultadoOperacion.correcto("Máximo simultáneo configurado.");
    }

    public void cargarMaximo(String disciplina, int maximo) {
        Deporte d = club.buscarDeporte(disciplina);
        if (d == null || maximo < 1 || maximos.putIfAbsent(d.getId(), maximo) != null) {
            throw new IllegalArgumentException("Máximo inválido o duplicado.");
        }
    }

    private boolean puede(Sesion sesion, ActividadAgenda a) {
        if (club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR)) {
            return true;
        }

        if (a instanceof Reserva r) {
            return (club.getOperaciones().esCaja(sesion) || (club.getAcceso().tienePerfil(sesion, Perfil.SOCIO)
                    && r.getJugador().getId().equals(sesion.getUsuarioId())));
        }
        return (club.getAcceso().tienePerfil(sesion, Perfil.ENTRENADOR)
                && (!(a instanceof Entrenamiento e) || e.getEntrenador().getId().equals(sesion.getUsuarioId())));
    }

    private String bloqueo(ActividadAgenda a) {
        if (a.estaCancelada()) {
            return "Actividad ya cancelada.";
        }

        if (a instanceof Partido p && (p.tieneResultado() || p.estaCerrado())) {
            return "Un partido jugado no se reprograma ni cancela.";
        }

        if (a instanceof Entrenamiento e && !e.getRegistroAsistencia().isEmpty()) {
            return "El entrenamiento ya tiene asistencia registrada.";
        }

        if (a instanceof Reserva r && club.getSeguridad().getUsos().containsValue(r.getId())
                && LocalDateTime.now(club.getReloj()).isBefore(r.getFin())) {
            return "Finalice el uso efectivo de la instalación antes de cambiar la reserva.";
        }
        return null;
    }

    public ResultadoOperacion cambiar(Sesion sesion, ActividadAgenda a, Instalacion destino, LocalDateTime inicio,
            LocalDateTime fin, String motivo, boolean cancelar) {
        if (a == null || !getActividades().contains(a) || !puede(sesion, a) || motivo == null || motivo.isBlank()) {
            return ResultadoOperacion.rechazo("Actividad, motivo o permiso inválidos.");
        }
        String bloqueo = bloqueo(a);
        if (bloqueo != null) {
            return ResultadoOperacion.rechazo(bloqueo);
        }

        if (!cancelar) {
            if (!(a instanceof Partido) && destino == null) {
                return ResultadoOperacion.rechazo("La actividad requiere instalación.");
            }

            if (a instanceof Reserva r && (!r.getJugador().estaActivo()
                    || !club.getAcademias().estaInscrito(r.getJugador().getId(), deporte(a).getId()))) {
                return ResultadoOperacion.rechazo("Socio inactivo o sin inscripción a la disciplina.");
            }
            String causa = validar(a, destino, inicio, fin, a.getUsuariosAgenda());
            if (causa != null) {
                return ResultadoOperacion.rechazo(causa);
            }
            long minutos = java.time.Duration.between(inicio, fin).toMinutes();
            if ((a instanceof Partido || a instanceof Entrenamiento)
                    && (minutos < 1 || minutos > Integer.MAX_VALUE || !inicio.plusMinutes(minutos).equals(fin))) {
                return ResultadoOperacion.rechazo("Duración inválida; use minutos enteros.");
            }
        }
        Instalacion anterior = a.getInstalacion();
        LocalDateTime desde = a.getInicio(), hasta = a.getFin();
        if (cancelar) {
            destino = anterior;
            inicio = desde;
            fin = hasta;
        }
        String instalacionAnteriorId;
        if (anterior == null) {
            instalacionAnteriorId = null;
        } else {
            instalacionAnteriorId = anterior.getId();
        }
        String instalacionNuevaId;
        if (destino == null) {
            instalacionNuevaId = null;
        } else {
            instalacionNuevaId = destino.getId();
        }
        CambioAgenda cambio = new CambioAgenda(instalacionAnteriorId, desde, hasta, instalacionNuevaId, inicio, fin,
                motivo, cancelar);
        if (!cancelar && anterior != null && anterior != destino) {
            anterior.getAgenda().retirarActividad(a);
        }

        if (a instanceof Reserva r) {
            r.cargarCambio(cambio);
            if (cancelar) {
                r.restaurarCancelada(true);
            } else {
                r.restaurarHorario(destino, inicio, fin);
            }
        } else if (a instanceof Entrenamiento e) {
            e.cargarCambio(cambio);
            if (cancelar) {
                e.restaurarCancelado(true);
            } else {
                e.restaurarHorario(destino, inicio, (int) Duration.between(inicio, fin).toMinutes());
            }
        } else if (a instanceof Partido p) {
            p.cargarCambio(cambio);
            if (cancelar) {
                p.cancelar();
            } else {
                p.restaurarHorario(destino, inicio, (int) Duration.between(inicio, fin).toMinutes());
            }
        }

        if (!cancelar && destino != null && anterior != destino && !destino.getAgenda().registrarActividad(a)) {
            throw new IllegalStateException("La disponibilidad cambió durante la confirmación.");
        }
        String mensajeResultado;
        if (cancelar) {
            mensajeResultado = "Actividad cancelada; intervalo liberado e historia conservada.";
        } else {
            mensajeResultado = "Horario actualizado sin cambios parciales.";
        }
        return ResultadoOperacion.correcto(mensajeResultado);
    }

    public void validarMaximos() {
        for (var limite : maximos.entrySet()) {
            Deporte d = club.buscarDeporte(limite.getKey());
            if (limite.getValue() > club.getReservas().getInstalaciones().stream().filter(i -> i.getDeporte() == d)
                    .count()) {
                throw new IllegalArgumentException("Máximo sin instalaciones suficientes.");
            }
            for (ActividadAgenda a : getActividades()) {
                if (!a.estaCancelada() && a.getInstalacion() != null && a.getInstalacion().getDeporte() == d
                        && excede(d, a.getInicio(), a.getFin(), null, null, limite.getValue())) {
                    throw new IllegalArgumentException("Agenda excede el máximo simultáneo.");
                }
            }
        }
    }
}
