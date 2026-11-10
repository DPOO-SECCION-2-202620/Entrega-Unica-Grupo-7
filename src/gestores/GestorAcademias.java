package gestores;

import aplicacion.*;
import gestores.GestorAcceso.*;
import java.time.*;
import java.util.*;
import modelo.deportes.*;
import modelo.partidos.*;
import modelo.usuarios.*;

public class GestorAcademias {

    public record Configuracion(int diferenciaMaxima, boolean excepcionEntrenador, int k, double rankingInicial) {
        public Configuracion {
            if ((diferenciaMaxima != 0 && diferenciaMaxima != 1) || k < 1 || k > 100 || !Double.isFinite(rankingInicial)
                    || rankingInicial < 0 || rankingInicial > Integer.MAX_VALUE - 100) {
                throw new IllegalArgumentException("Configuración de academia inválida.");
            }
        }
    }

    private record Clave(String jugador, String deporte) {
    }

    private final EstadoClub club;
    private final Map<Clave, InscripcionDisciplina> inscripciones = new LinkedHashMap<>();
    private Configuracion configuracion = new Configuracion(1, true, 32, 1000);

    public GestorAcademias(EstadoClub club) {
        this.club = club;
    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }

    public void cargarConfiguracion(Configuracion configuracion) {
        this.configuracion = Objects.requireNonNull(configuracion);
    }

    public ResultadoOperacion configurar(Sesion admin, Configuracion configuracion) {
        if (!club.getAcceso().tienePerfil(admin, Perfil.ADMINISTRADOR) || configuracion == null) {
            return ResultadoOperacion.rechazo("Solo el administrador configura academia.");
        }
        this.configuracion = configuracion;
        return ResultadoOperacion.correcto("Regla de academia actualizada para nuevos partidos e inscripciones.");
    }

    public List<InscripcionDisciplina> getInscripciones() {
        return List.copyOf(inscripciones.values());
    }

    public InscripcionDisciplina buscar(String jugador, String deporte) {
        return inscripciones.get(new Clave(jugador, Deporte.clave(deporte)));
    }

    public boolean estaInscrito(String jugador, String deporte) {
        InscripcionDisciplina i = buscar(jugador, deporte);
        return i != null && i.estaActiva();
    }

    private boolean docente(Sesion s) {
        return (club.getAcceso().tienePerfil(s, Perfil.ADMINISTRADOR)
                || club.getAcceso().tienePerfil(s, Perfil.ENTRENADOR));
    }

    public ResultadoOperacion inscribir(Sesion sesion, String jugadorId, String deporteId, NivelAcademia nivel) {
        Jugador j = club.buscarJugador(jugadorId);
        Deporte d = club.buscarDeporte(deporteId);
        boolean propio = club.getAcceso().tienePerfil(sesion, Perfil.SOCIO) && sesion.getUsuarioId().equals(jugadorId);
        if ((!docente(sesion) && !propio) || (propio && !docente(sesion) && nivel != NivelAcademia.PRINCIPIANTE)) {
            return ResultadoOperacion.rechazo(
                    "Solo el responsable asigna niveles superiores; el socio puede iniciar como principiante.");
        }

        if (j == null || !j.estaActivo() || d == null || d.isColectivo() || nivel == null
                || !club.getAcceso().tienePerfil(jugadorId, Perfil.SOCIO)
                || club.getAcceso().tienePerfil(jugadorId, Perfil.ADMINISTRADOR)) {
            return ResultadoOperacion.rechazo("Socio/disciplina individual/nivel inválidos.");
        }

        if (buscar(jugadorId, d.getId()) != null) {
            return ResultadoOperacion.rechazo("Ya existe inscripción en esa disciplina; actualice su estado.");
        }
        double inicial = j.getRankingsPorDisciplina().getOrDefault(d.getId(), configuracion.rankingInicial());
        LocalDate fecha = LocalDate.now(club.getReloj());
        String referencia = "INSCRIPCION:" + d.getId();
        if (!j.getRankingsPorDisciplina().containsKey(d.getId())
                && !j.puedeRegistrarRanking(d.getId(), inicial, fecha, referencia)) {
            return ResultadoOperacion.rechazo("No se pudo iniciar el ranking de esa disciplina.");
        }
        InscripcionDisciplina i = new InscripcionDisciplina(j, d, nivel);
        if (!j.getRankingsPorDisciplina().containsKey(d.getId())) {
            j.actualizarRanking(d.getId(), inicial, fecha, referencia);
        }
        cargarInscripcion(i);
        return ResultadoOperacion.correcto("Inscripción a academia registrada.");
    }

    public ResultadoOperacion actualizar(Sesion sesion, String jugador, String deporte, NivelAcademia nivel,
            boolean activa) {
        InscripcionDisciplina i = buscar(jugador, deporte);
        if (!docente(sesion) || i == null || nivel == null || !i.getJugador().estaActivo()) {
            return ResultadoOperacion.rechazo("Inscripción inexistente o sin permiso docente.");
        }
        i.restaurarNivel(nivel);
        i.restaurarActiva(activa);
        return ResultadoOperacion.correcto("Nivel/estado de academia actualizado; historia conservada.");
    }

    public void cargarInscripcion(InscripcionDisciplina i) {
        if (i == null || club.buscarJugador(i.getJugador().getId()) != i.getJugador()
                || club.buscarDeporte(i.getDisciplina().getId()) != i.getDisciplina()
                || !i.getJugador().getRankingsPorDisciplina().containsKey(i.getDisciplina().getId())
                || inscripciones.putIfAbsent(new Clave(i.getJugador().getId(), i.getDisciplina().getId()), i) != null) {
            throw new IllegalArgumentException("Inscripción de academia inexistente, duplicada o sin ranking.");
        }
        i.getJugador().vincularDisciplina(i);
    }

    public Map<String, Double> consultarRankings(String deporte) {
        Map<String, Double> resultado = new LinkedHashMap<>();
        for (InscripcionDisciplina i : inscripciones.values()) {
            if (i.estaActiva() && i.getDisciplina().es(deporte)) {
                resultado.put(i.getJugador().getId(), i.getRanking());
            }
        }
        return Map.copyOf(resultado);
    }

    public ResultadoOperacion validarParticipantes(PartidoAcademia p) {
        if (p == null || p.getDeporte().isColectivo()) {
            return ResultadoOperacion.rechazo("Partido individual inválido.");
        }
        boolean dobles = p.getModalidad().equalsIgnoreCase("Dobles"),
                sencillos = p.getModalidad().equalsIgnoreCase("Sencillos");
        int lado;
        if (dobles) {
            lado = 2;
        } else {
            lado = 1;
        }

        if ((!dobles && !sencillos) || p.getLadoA().size() != lado || p.getLadoB().size() != lado
                || (p.getDeporte().es("PADEL") && !dobles)
                || p.getJugadores().stream().map(Jugador::getId).distinct().count() != lado * 2) {
            return ResultadoOperacion.rechazo("Modalidad/participantes inválidos; pádel exige cuatro en dobles.");
        }
        List<Integer> niveles = new ArrayList<>();
        for (Jugador j : p.getJugadores()) {
            InscripcionDisciplina i = buscar(j.getId(), p.getDeporte().getId());
            if (club.buscarJugador(j.getId()) != j || !j.estaActivo() || i == null || !i.estaActiva()
                    || !club.getAcceso().tienePerfil(j.getId(), Perfil.SOCIO)
                    || club.getAcceso().tienePerfil(j.getId(), Perfil.ADMINISTRADOR)) {
                return ResultadoOperacion.rechazo(
                        "Participante inactivo, sin perfil deportivo o sin inscripción a la disciplina: " + j.getId());
            }

            if (!(p.permiteExcepcionEntrenador() && club.getAcceso().tienePerfil(j.getId(), Perfil.ENTRENADOR))) {
                niveles.add(i.getNivel().ordinal());
            }
        }
        if (!niveles.isEmpty() && Collections.max(niveles) - Collections.min(niveles) > p.getDiferenciaNiveles()) {
            return ResultadoOperacion
                    .rechazo("Niveles incompatibles; se admiten el mismo nivel o adyacentes según la configuración.");
        }
        return ResultadoOperacion.correcto("Participantes compatibles.");
    }

    public ResultadoOperacion registrar(Sesion sesion, PartidoAcademia p) {
        if (!docente(sesion)) {
            return ResultadoOperacion.rechazo("Solo entrenador/administrador programa partidos de academia.");
        }
        ResultadoOperacion v = validarParticipantes(p);
        if (!v.exito()) {
            return v;
        }

        if (p.getInstalacion() == null) {
            return ResultadoOperacion.rechazo("El partido de academia requiere una instalación del club y duración.");
        }
        String causa = club.getAgenda().validar(p);
        if (causa != null) {
            return ResultadoOperacion.rechazo(causa);
        }

        if (club.getPartidos().registrarPartido(p)) {
            return ResultadoOperacion.correcto("Partido de academia programado.");
        } else {
            return ResultadoOperacion.rechazo("ID repetido o instalación ocupada.");
        }
    }

    public ResultadoOperacion validarJuego(PartidoAcademia p) {
        if (p == null || p.estaCancelado() || p.estaCerrado() || p.tieneResultado()) {
            return ResultadoOperacion.rechazo("Partido inexistente, cancelado o con resultado previo.");
        }
        ResultadoOperacion v = validarParticipantes(p);
        if (!v.exito()) {
            return v;
        }
        for (var anterior : club.getPartidos().getPartidos()) {
            if (anterior instanceof PartidoAcademia a && a != p && a.tieneResultado()
                    && a.getDeporte() == p.getDeporte() && !p.getFechaHora().isAfter(a.getFechaHora())
                    && a.getJugadores().stream().anyMatch(p.getJugadores()::contains)) {
                return ResultadoOperacion.rechazo("Registre los resultados de cada participante en orden cronológico.");
            }
        }
        return ResultadoOperacion.correcto("Partido compatible.");
    }

    public ResultadoOperacion registrarResultado(Sesion sesion, String id, int a, int b) {
        if (!docente(sesion)) {
            return ResultadoOperacion.rechazo("Solo entrenador/administrador confirma resultados de academia.");
        }

        if (!(club.getPartidos().buscarPartido(id) instanceof PartidoAcademia p)) {
            return ResultadoOperacion.rechazo("Partido de academia inexistente.");
        }
        ResultadoOperacion v = validarJuego(p);
        if (!v.exito()) {
            return v;
        }

        if (a < 0 || b < 0 || Math.max(a, b) != p.getSetsParaGanar() || Math.min(a, b) >= p.getSetsParaGanar()) {
            return ResultadoOperacion.rechazo("Marcador de sets inválido.");
        }
        double promedioA = p.getLadoA().stream()
                .mapToDouble(j -> buscar(j.getId(), p.getDeporte().getId()).getRanking()).average().orElseThrow(),
                promedioB = p.getLadoB().stream()
                        .mapToDouble(j -> buscar(j.getId(), p.getDeporte().getId()).getRanking()).average()
                        .orElseThrow();
        double esperado = 1 / (1 + Math.pow(10, (promedioB - promedioA) / 400.0));
        int resultadoLocal;
        if (a > b) {
            resultadoLocal = 1;
        } else {
            resultadoLocal = 0;
        }
        long delta = Math.round(p.getK() * ((resultadoLocal) - esperado));
        List<Jugador> perdedores;
        if (delta >= 0) {
            perdedores = p.getLadoB();
        } else {
            perdedores = p.getLadoA();
        }
        double minimoPerdedor = (perdedores).stream()
                .mapToDouble(j -> buscar(j.getId(), p.getDeporte().getId()).getRanking()).min().orElseThrow();
        if (delta >= 0) {
            delta = Math.min(delta, (long) Math.floor(minimoPerdedor));
        } else {
            delta = -Math.min(-delta, (long) Math.floor(minimoPerdedor));
        }
        Map<Jugador, PartidoAcademia.CambioElo> cambios = new LinkedHashMap<>();
        String referencia = "ELO:" + p.getId();
        LocalDate fecha = p.getFechaHora().toLocalDate();
        for (Jugador j : p.getJugadores()) {
            long cambioRanking;
            if (p.getLadoA().contains(j)) {
                cambioRanking = delta;
            } else {
                cambioRanking = -delta;
            }
            double previo = buscar(j.getId(), p.getDeporte().getId()).getRanking(), nuevo = previo + (cambioRanking);
            if (nuevo > Integer.MAX_VALUE
                    || !j.puedeRegistrarRanking(p.getDeporte().getId(), nuevo, fecha, referencia)) {
                return ResultadoOperacion.rechazo("Ranking o secuencia histórica inválidos; no se aplicó ELO.");
            }

            if (p.getEstadisticas().stream().anyMatch(e -> e.getJugador() == j && e.getEloResultante() != null)) {
                return ResultadoOperacion.rechazo("El participante ya tiene ELO registrado en este partido.");
            }
            cambios.put(j, new PartidoAcademia.CambioElo(previo, nuevo));
        }
        if (!p.restaurarResultado(a, b)) {
            return ResultadoOperacion.rechazo("Resultado ya registrado.");
        }
        for (var cambio : cambios.entrySet()) {
            Jugador j = cambio.getKey();
            j.actualizarRanking(p.getDeporte().getId(), cambio.getValue().nuevo(), fecha, referencia);
            p.cargarCambioElo(j.getId(), cambio.getValue());
            EstadisticaPartido e = p.getEstadisticas().stream().filter(registro -> registro.getJugador() == j)
                    .findFirst().orElse(null);
            boolean nueva = e == null;
            if (nueva) {
                e = new EstadisticaPartido(j, p);
            }
            e.cargarEloResultante((int) Math.round(cambio.getValue().nuevo()));
            int setsGanados;
            if (p.getLadoA().contains(j)) {
                setsGanados = a;
            } else {
                setsGanados = b;
            }
            e.registrarMetrica(TipoEstadistica.SETS, setsGanados);
            if (nueva) {
                p.registrarEstadistica(e);
            }
        }
        p.cerrarActa();
        return ResultadoOperacion.correcto("Resultado y ELO confirmados una sola vez.");
    }

    public void validarHistorial() {
        for (var partido : club.getPartidos().getPartidos()) {
            if (partido instanceof PartidoAcademia p) {
                int lado;
                if (p.getModalidad().equalsIgnoreCase("Dobles")) {
                    lado = 2;
                } else if (p.getModalidad().equalsIgnoreCase("Sencillos")) {
                    lado = 1;
                } else {
                    lado = 0;
                }

                if (lado == 0 || p.getLadoA().size() != lado || p.getLadoB().size() != lado
                        || p.getJugadores().stream().distinct().count() != lado * 2
                        || (p.getDeporte().es("PADEL") && lado != 2) || p.getInstalacion() == null
                        || p.getJugadores().stream().anyMatch(j -> buscar(j.getId(), p.getDeporte().getId()) == null)) {
                    throw new IllegalArgumentException(
                            "Partido de academia sin participantes/inscripción/modalidad/instalación válidos.");
                }

                if (!p.tieneResultado()) {
                    if (!p.getCambiosElo().isEmpty()) {
                        throw new IllegalArgumentException("Partido sin resultado contiene ELO.");
                    }
                    continue;
                }

                if (!p.estaCerrado() || p.getCambiosElo().size() != p.getJugadores().size()) {
                    throw new IllegalArgumentException("Partido de academia sin historial ELO completo.");
                }
                for (Jugador j : p.getJugadores()) {
                    var cambio = p.getCambiosElo().get(j.getId());
                    var estadistica = p.getEstadisticas().stream().filter(e -> e.getJugador() == j).findFirst()
                            .orElse(null);
                    Integer setsDelJugador;
                    if (p.getLadoA().contains(j)) {
                        setsDelJugador = p.getMarcadorA();
                    } else {
                        setsDelJugador = p.getMarcadorB();
                    }

                    if (cambio == null || estadistica == null
                            || !Objects.equals(estadistica.getEloResultante(), (int) Math.round(cambio.nuevo()))
                            || !Objects.equals(estadistica.obtenerMetrica(TipoEstadistica.SETS), setsDelJugador)
                            || j.getMovimientosRanking().stream()
                                    .noneMatch(m -> m.referencia().equals("ELO:" + p.getId())
                                            && m.disciplina().equals(p.getDeporte().getId())
                                            && m.fecha().equals(p.getFechaHora().toLocalDate())
                                            && Math.abs(m.anterior() - cambio.anterior()) < 0.001
                                            && Math.abs(m.nuevo() - cambio.nuevo()) < 0.001)) {
                        throw new IllegalArgumentException("Movimiento/estadística ELO no coincide con el partido.");
                    }
                }
                double promedioA = p.getLadoA().stream().mapToDouble(j -> p.getCambiosElo().get(j.getId()).anterior())
                        .average().orElseThrow(),
                        promedioB = p.getLadoB().stream().mapToDouble(j -> p.getCambiosElo().get(j.getId()).anterior())
                                .average().orElseThrow();
                int resultadoLocalHistorico;
                if (p.getMarcadorA() > p.getMarcadorB()) {
                    resultadoLocalHistorico = 1;
                } else {
                    resultadoLocalHistorico = 0;
                }
                long delta = Math.round(
                        p.getK() * (resultadoLocalHistorico - 1 / (1 + Math.pow(10, (promedioB - promedioA) / 400.0))));
                List<Jugador> perdedoresHistoricos;
                if (delta >= 0) {
                    perdedoresHistoricos = p.getLadoB();
                } else {
                    perdedoresHistoricos = p.getLadoA();
                }
                double minimo = perdedoresHistoricos.stream()
                        .mapToDouble(j -> p.getCambiosElo().get(j.getId()).anterior()).min().orElseThrow();
                if (delta >= 0) {
                    delta = Math.min(delta, (long) Math.floor(minimo));
                } else {
                    delta = -Math.min(-delta, (long) Math.floor(minimo));
                }
                for (Jugador j : p.getJugadores()) {
                    var cambio = p.getCambiosElo().get(j.getId());
                    long cambioEsperado;
                    if (p.getLadoA().contains(j)) {
                        cambioEsperado = delta;
                    } else {
                        cambioEsperado = -delta;
                    }

                    if (Math.abs(cambio.nuevo() - cambio.anterior() - cambioEsperado) > 0.001) {
                        throw new IllegalArgumentException("ELO no corresponde a la fórmula del partido.");
                    }
                }
            }
        }
    }
}
