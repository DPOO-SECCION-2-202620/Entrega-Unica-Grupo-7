package gestores;

import aplicacion.EstadoClub;
import aplicacion.ResultadoOperacion;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.competencias.EncuentroTorneo;
import modelo.competencias.FormatoFases;
import modelo.competencias.FormatoLiga;
import modelo.competencias.ParticipanteCompetencia;
import modelo.deportes.Equipo;
import modelo.deportes.InscripcionDisciplina;
import modelo.deportes.NivelAcademia;
import modelo.torneos.*;
import modelo.usuarios.Jugador;

public class GestorTorneos {

    private final EstadoClub club;
    private final Map<String, TorneoInterno> torneos = new LinkedHashMap<>();

    public GestorTorneos(EstadoClub club) {
        this.club = club;
    }

    private void exigirAdministrador(Sesion sesion) {
        if (!club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR)) {
            throw new IllegalArgumentException("Solo el administrador gestiona los torneos internos.");
        }
    }

    private TorneoInterno exigirTorneoAbierto(String id) {
        TorneoInterno torneo = torneos.get(id);
        if (torneo == null) {
            throw new IllegalArgumentException("El torneo no existe.");
        }

        if (!torneo.admiteInscripciones(LocalDateTime.now(club.getReloj()))) {
            throw new IllegalArgumentException("El periodo de inscripción terminó.");
        }
        return torneo;
    }

    public List<TorneoInterno> consultar(Sesion sesion) {
        exigirAdministrador(sesion);
        return List.copyOf(torneos.values());
    }

    public List<TorneoInterno> consultarParaPersistencia() {
        return List.copyOf(torneos.values());
    }

    public TorneoInterno buscar(String id) {
        return torneos.get(id);
    }

    public void cargar(TorneoInterno torneo) {
        if (torneo == null || club.buscarDeporte(torneo.getDeporte().getId()) != torneo.getDeporte()
                || torneos.putIfAbsent(torneo.getId(), torneo) != null) {
            throw new IllegalArgumentException("Torneo duplicado o deporte no canónico.");
        }
    }

    public ResultadoOperacion crear(Sesion sesion, String id, String nombre, String deporte, String categoria,
            NivelAcademia nivel, ModalidadTorneo modalidad, int cupos, LocalDateTime inicio, LocalDateTime liberacion,
            TipoTorneo tipo, BigDecimal tarifa) {
        try {
            exigirAdministrador(sesion);
            LocalDateTime ahora = LocalDateTime.now(club.getReloj());
            if (inicio == null || !inicio.isAfter(ahora) || (liberacion != null && liberacion.isBefore(ahora))) {
                throw new IllegalArgumentException(
                        "El inicio debe ser futuro y la liberación no puede estar en el pasado.");
            }
            java.time.LocalDateTime fechaSeleccionada;
            if (liberacion == null) {
                fechaSeleccionada = ahora;
            } else {
                fechaSeleccionada = liberacion;
            }
            TorneoInterno torneo = new TorneoInterno(id, nombre, club.buscarDeporte(deporte), categoria, nivel,
                    modalidad, cupos, inicio, fechaSeleccionada, tipo, tarifa);
            cargar(torneo);
            return ResultadoOperacion.correcto("Torneo registrado con " + torneo.getReservaInicial()
                    + " cupos preferenciales y tarifa " + torneo.getTarifa() + ".");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    private Jugador exigirJugador(TorneoInterno torneo, String id) {
        Jugador jugador = club.buscarJugador(id);
        InscripcionDisciplina inscripcion = club.getAcademias().buscar(id, torneo.getDeporte().getId());
        if (jugador == null || !jugador.estaActivo() || !club.getAcceso().tienePerfil(id, Perfil.SOCIO)
                || club.getAcceso().tienePerfil(id, Perfil.ADMINISTRADOR) || inscripcion == null
                || !inscripcion.estaActiva() || inscripcion.getNivel() != torneo.getNivel()) {
            throw new IllegalArgumentException(
                    "Jugador sin inscripción activa en la disciplina y nivel del torneo: " + id);
        }
        return jugador;
    }

    private CandidatoTorneo jugador(TorneoInterno torneo, String id) {
        Jugador jugador = exigirJugador(torneo, id);
        return new CandidatoTorneo(id, jugador.getNombre(), List.of(id), null,
                jugador.obtenerRankingPorDisciplina(torneo.getDeporte().getId()));
    }

    private CandidatoTorneo pareja(TorneoInterno torneo, List<String> ids) {
        if (ids == null || ids.size() != 2 || ids.get(0) == null || ids.get(1) == null
                || ids.get(0).equals(ids.get(1))) {
            throw new IllegalArgumentException("La pareja debe tener dos jugadores distintos.");
        }
        List<String> ordenados = ids.stream().sorted().toList();
        Jugador primero = exigirJugador(torneo, ordenados.get(0));
        Jugador segundo = exigirJugador(torneo, ordenados.get(1));
        String id = "PAREJA:" + primero.getId().length() + ":" + primero.getId() + segundo.getId().length() + ":"
                + segundo.getId();
        double puntaje = primero.obtenerRankingPorDisciplina(torneo.getDeporte().getId()) / 2
                + segundo.obtenerRankingPorDisciplina(torneo.getDeporte().getId()) / 2;
        return new CandidatoTorneo(id, primero.getNombre() + " / " + segundo.getNombre(), ordenados, null, puntaje);
    }

    private CandidatoTorneo equipo(TorneoInterno torneo, String id, double puntaje) {
        Equipo equipo = club.getEquipos().buscarEquipo(id);
        if (equipo == null || !equipo.estaActivo() || equipo.getDeporte() != torneo.getDeporte()
                || !equipo.getCategoria().equals(torneo.getCategoria())) {
            throw new IllegalArgumentException("Equipo de otro deporte/categoría o inactivo: " + id);
        }
        List<String> jugadores = jugadoresDelEquipo(equipo, torneo);
        if (jugadores.size() < torneo.getDeporte().getMinimoJugadores()) {
            throw new IllegalArgumentException("El equipo no tiene el mínimo de jugadores de la categoría.");
        }
        return new CandidatoTorneo(id, equipo.getNombre(), jugadores, id, puntaje);
    }

    private List<String> jugadoresDelEquipo(Equipo equipo, TorneoInterno torneo) {
        return equipo.getInscripciones().stream().map(i -> i.getJugador())
                .filter(j -> j.estaActivo() && !club.getAcceso().tienePerfil(j.getId(), Perfil.ADMINISTRADOR)
                        && equipo.esCategoriaCompatible(j, torneo.getInicio().toLocalDate()))
                .map(Jugador::getId).sorted().toList();
    }

    public ResultadoOperacion clasificarJugadores(Sesion sesion, String id) {
        try {
            exigirAdministrador(sesion);
            TorneoInterno torneo = exigirTorneoAbierto(id);
            if (torneo.getModalidad() != ModalidadTorneo.SENCILLOS) {
                throw new IllegalArgumentException("El torneo no es de sencillos.");
            }
            List<CandidatoTorneo> candidatos = new ArrayList<>();
            for (InscripcionDisciplina inscripcion : club.getAcademias().getInscripciones()) {
                String jugadorId = inscripcion.getJugador().getId();
                if (inscripcion.estaActiva() && inscripcion.getJugador().estaActivo()
                        && inscripcion.getDisciplina() == torneo.getDeporte()
                        && inscripcion.getNivel() == torneo.getNivel()
                        && club.getAcceso().tienePerfil(jugadorId, Perfil.SOCIO)
                        && !club.getAcceso().tienePerfil(jugadorId, Perfil.ADMINISTRADOR)) {
                    candidatos.add(jugador(torneo, jugadorId));
                }
            }
            torneo.asignarClasificacion("Ranking ELO actual de la disciplina y nivel", candidatos);
            return ResultadoOperacion.correcto("Clasificación registrada; los mejores " + torneo.getReservaInicial()
                    + " pueden solicitar cupo preferencial.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion clasificarParejas(Sesion sesion, String id, List<List<String>> parejas) {
        try {
            exigirAdministrador(sesion);
            TorneoInterno torneo = exigirTorneoAbierto(id);
            if (torneo.getModalidad() != ModalidadTorneo.DOBLES || parejas == null) {
                throw new IllegalArgumentException("Indique las parejas de un torneo de dobles.");
            }
            List<CandidatoTorneo> candidatos = new ArrayList<>();
            for (List<String> ids : parejas) {
                candidatos.add(pareja(torneo, ids));
            }
            torneo.asignarClasificacion("Promedio ELO de las parejas presentadas, en la disciplina y nivel",
                    candidatos);
            return ResultadoOperacion.correcto("Clasificación de parejas registrada.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion clasificarEquipos(Sesion sesion, String id, String criterio,
            Map<String, Double> puntajes) {
        try {
            exigirAdministrador(sesion);
            TorneoInterno torneo = exigirTorneoAbierto(id);
            if (torneo.getModalidad() != ModalidadTorneo.EQUIPOS || puntajes == null || puntajes.isEmpty()) {
                throw new IllegalArgumentException("Indique el desempeño de los equipos y su criterio verificable.");
            }
            List<CandidatoTorneo> candidatos = new ArrayList<>();
            for (var entrada : puntajes.entrySet()) {
                if (entrada.getValue() == null) {
                    throw new IllegalArgumentException("Falta un puntaje de desempeño.");
                }
                candidatos.add(equipo(torneo, entrada.getKey(), entrada.getValue()));
            }
            for (Equipo equipo : club.getEquipos().getEquipos()) {
                if (equipo.estaActivo() && equipo.getDeporte() == torneo.getDeporte()
                        && equipo.getCategoria().equals(torneo.getCategoria())
                        && jugadoresDelEquipo(equipo, torneo).size() >= torneo.getDeporte().getMinimoJugadores()
                        && !puntajes.containsKey(equipo.getId())) {
                    throw new IllegalArgumentException(
                            "Incluya el desempeño de todos los equipos habilitados de la categoría: " + equipo.getId());
                }
            }
            torneo.asignarClasificacion(criterio, candidatos);
            return ResultadoOperacion.correcto("Desempeños y clasificación de equipos registrados.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion clasificarParticipantesExternos(Sesion sesion, String torneoId,
            Map<String, Double> puntajes) {
        try {
            exigirAdministrador(sesion);
            TorneoInterno torneo = exigirTorneoAbierto(torneoId);
            if (torneo.getModalidad() != ModalidadTorneo.SENCILLOS || puntajes == null || puntajes.isEmpty()) {
                throw new IllegalArgumentException("Indique participantes externos de un torneo de sencillos.");
            }
            List<CandidatoTorneo> candidatos = new ArrayList<>();
            for (var entrada : puntajes.entrySet()) {
                String id = entrada.getKey();
                Double puntaje = entrada.getValue();
                if (id == null || id.isBlank() || id.trim().startsWith("EXTERNO:") || club.buscarUsuario(id) != null
                        || club.buscarJugador(id) != null || puntaje == null) {
                    throw new IllegalArgumentException(
                            "El participante externo debe tener ID único y puntaje válido: " + id);
                }
                candidatos.add(new CandidatoTorneo("EXTERNO:" + id.trim(), id.trim(), List.of("EXTERNO:" + id.trim()),
                        null, puntaje));
            }
            torneo.asignarClasificacion("Desempeño externo informado por el administrador", candidatos);
            return ResultadoOperacion.correcto("Clasificación externa registrada para el torneo.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion inscribir(Sesion sesion, String torneoId, List<String> jugadores, String equipoId,
            TipoCupo cupo) {
        try {
            exigirAdministrador(sesion);
            TorneoInterno torneo = exigirTorneoAbierto(torneoId);
            CandidatoTorneo candidato;
            if (torneo.getModalidad() == ModalidadTorneo.EQUIPOS) {
                candidato = equipo(torneo, equipoId, 0);
            } else if (torneo.getModalidad() == ModalidadTorneo.DOBLES) {
                candidato = pareja(torneo, jugadores);
            } else if (torneo.getModalidad() == ModalidadTorneo.SENCILLOS) {
                if (jugadores == null || jugadores.size() != 1) {
                    throw new IllegalArgumentException("Seleccione un jugador para sencillos.");
                }
                String jugadorId = jugadores.get(0);
                if (esParticipanteExterno(jugadorId)) {
                    candidato = candidatoExterno(torneo, jugadorId);
                } else if (esEmpleadoActivo(jugadorId)) {
                    candidato = candidatoEmpleado(torneo, jugadorId);
                } else {
                    candidato = jugador(torneo, jugadorId);
                }
            } else {
                candidato = pareja(torneo, jugadores);
            }
            torneo.registrarInscripcion(candidato, cupo);
            return ResultadoOperacion.correcto("Inscripción registrada en cupo " + cupo + ".");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    private boolean esParticipanteExterno(String id) {
        return id != null && club.buscarUsuario(id) == null;
    }

    private boolean esEmpleadoActivo(String id) {
        return club.getVentas().getEmpleados().stream()
                .anyMatch(e -> e.getId().equals(id) && e.getUsuario().estaActivo()
                        && (club.getAcceso().tienePerfil(id, Perfil.EMPLEADO)
                                || club.getAcceso().tienePerfil(id, Perfil.ENTRENADOR)
                                || club.getAcceso().tienePerfil(id, Perfil.FISIOTERAPEUTA)));
    }

    private CandidatoTorneo candidatoEmpleado(TorneoInterno torneo, String id) {
        if (torneo.getModalidad() != ModalidadTorneo.SENCILLOS) {
            throw new IllegalArgumentException("Los empleados solo participan en torneos individuales.");
        }
        Jugador jugador = club.buscarJugador(id);
        if (jugador == null || !club.getAcceso().tienePerfil(id, Perfil.SOCIO)
                || club.getAcceso().tienePerfil(id, Perfil.ADMINISTRADOR)) {
            throw new IllegalArgumentException("El empleado debe tener perfil de jugador socio en la disciplina.");
        }
        InscripcionDisciplina inscripcion = club.getAcademias().buscar(id, torneo.getDeporte().getId());
        if (!jugador.estaActivo() || inscripcion == null || !inscripcion.estaActiva()
                || inscripcion.getNivel() != torneo.getNivel()) {
            throw new IllegalArgumentException("Empleado sin inscripción activa en la disciplina y nivel del torneo.");
        }
        return new CandidatoTorneo(id, jugador.getNombre(), List.of(id), null,
                jugador.obtenerRankingPorDisciplina(torneo.getDeporte().getId()));
    }

    private CandidatoTorneo candidatoExterno(TorneoInterno torneo, String id) {
        CandidatoTorneo candidato = torneo.getClasificacion().stream().filter(c -> c.id().equals("EXTERNO:" + id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "El participante externo no está registrado en la clasificación."));
        if (torneo.getModalidad() != ModalidadTorneo.SENCILLOS) {
            throw new IllegalArgumentException("Solo se admiten participantes externos individuales registrados.");
        }
        return candidato;
    }

    public ResultadoOperacion inscribirEmpleado(Sesion sesion, String torneoId, List<String> jugadores, String equipoId,
            TipoCupo cupo) {
        try {
            exigirAdministrador(sesion);
            TorneoInterno torneo = exigirTorneoAbierto(torneoId);
            if (torneo.getModalidad() != ModalidadTorneo.SENCILLOS) {
                throw new IllegalArgumentException("Los empleados solo pueden inscribirse en torneos individuales.");
            }

            if (torneo.getTipo() == TipoTorneo.COMPETITIVO) {
                throw new IllegalArgumentException(
                        "La inscripción de empleados en torneos competitivos es gratuita y no genera premio en efectivo.");
            }

            if (jugadores == null || jugadores.size() != 1) {
                throw new IllegalArgumentException("Seleccione un empleado para sencillos.");
            }
            String id = jugadores.get(0);
            if (esParticipanteExterno(id)) {
                throw new IllegalArgumentException("El empleado debe existir en el registro laboral.");
            }
            var empleado = club.getVentas().getEmpleados().stream().filter(e -> e.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El empleado no existe."));
            if (!empleado.getUsuario().estaActivo() || (!club.getAcceso().tienePerfil(id, Perfil.EMPLEADO)
                    && !club.getAcceso().tienePerfil(id, Perfil.ENTRENADOR)
                    && !club.getAcceso().tienePerfil(id, Perfil.FISIOTERAPEUTA))) {
                throw new IllegalArgumentException("El empleado debe estar activo y tener perfil laboral.");
            }
            CandidatoTorneo candidato = candidatoEmpleado(torneo, id);
            torneo.registrarInscripcion(candidato, cupo);
            return ResultadoOperacion.correcto("Inscripción gratuita del empleado registrada.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion retirar(Sesion sesion, String torneoId, String participanteId) {
        try {
            exigirAdministrador(sesion);
            exigirTorneoAbierto(torneoId).retirarInscripcion(participanteId);
            return ResultadoOperacion.correcto("Inscripción retirada; se liberó el cupo de la unidad completa.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion liberarPreferenciales(Sesion sesion, String id) {
        try {
            exigirAdministrador(sesion);
            int liberados = exigirTorneoAbierto(id).liberarPreferenciales(LocalDateTime.now(club.getReloj()));
            return ResultadoOperacion.correcto("Se convirtieron " + liberados + " cupos preferenciales en regulares.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(club.getReloj());
    }

    private void exigirOrganizador(Sesion sesion) {
        if (!club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR)
                && !club.getAcceso().tienePerfil(sesion, Perfil.ENTRENADOR)) {
            throw new IllegalArgumentException("Solo el administrador o un entrenador organiza partidos");
        }
    }

    private TorneoInterno exigirTorneo(String id) {
        TorneoInterno torneo = torneos.get(id);
        if (torneo == null) {
            throw new IllegalArgumentException("El torneo no existe");
        }
        return torneo;
    }

    public HistorialTorneo consultarHistorial(Sesion sesion, String torneoId) {
        if (!club.getAcceso().estaAutenticada(sesion)) {
            throw new IllegalArgumentException("Inicie sesión para consultar los torneos");
        }
        TorneoInterno torneo = exigirTorneo(torneoId);
        EstadoTorneo estado = torneo.estado(ahora());
        FormatoLiga formato = torneo.getFormato();
        if (formato == null) {
            List<ParticipanteCompetencia> inscritos = new ArrayList<>();
            for (InscripcionTorneo inscripcion : torneo.getInscripciones()) {
                if (inscripcion.activa()) {
                    CandidatoTorneo c = inscripcion.participante();
                    inscritos.add(new ParticipanteCompetencia(c.id(), c.nombre(), c.equipoId(), true));
                }
            }
            return new HistorialTorneo(torneo.getId(), torneo.getNombre(), estado, inscritos, null, null, null,
                    "Aún no existen encuentros registrados");
        }
        String mensaje;
        if (estado == EstadoTorneo.FINALIZADO) {
            mensaje = "Torneo finalizado";
        } else {
            mensaje = "Torneo en curso";
        }
        return new HistorialTorneo(torneo.getId(), torneo.getNombre(), estado, formato.getParticipantes(),
                formato.getEncuentros(), formato.getTabla(), torneo.getGanadorId(), mensaje);
    }

    public ResultadoOperacion iniciarCompetencia(Sesion sesion, String torneoId, boolean eliminacion) {
        try {
            exigirOrganizador(sesion);
            TorneoInterno torneo = exigirTorneo(torneoId);
            if (torneo.estado(ahora()) != EstadoTorneo.EN_CURSO) {
                throw new IllegalArgumentException("La competencia solo se puede iniciar cuando el torneo está en curso");
            }
            if (torneo.getFormato() != null) {
                throw new IllegalArgumentException("La competencia del torneo ya ha sido iniciada");
            }
            List<CandidatoTorneo> inscritos = new ArrayList<>();
            for (InscripcionTorneo inscripcion : torneo.getInscripciones()) {
                if (inscripcion.activa()) {
                    inscritos.add(inscripcion.participante());
                }
            }


            int cantidad = inscritos.size();
            if (cantidad < 2) {
                throw new IllegalArgumentException("Se necesitan al menos dos inscritos para iniciar.");
            }
            if (eliminacion && (cantidad < 4 || (cantidad & (cantidad - 1)) != 0)) {
                throw new IllegalArgumentException("La eliminación directa exige 4, 8, 16... inscritos.");
            }


            FormatoLiga formato;
            if (eliminacion) {
                formato = new FormatoFases();
            } else {
                formato = new FormatoLiga();
            }
            formato.configurarDeporte(torneo.getDeporte());
            if (torneo.getDeporte().es("TENIS") || torneo.getDeporte().es("PADEL")) {
                formato.configurarSets(2);
            }
            for (CandidatoTorneo c : inscritos) {
                formato.registrarParticipante(new ParticipanteCompetencia(c.id(), c.nombre(), c.equipoId(), true));
            }
            if (eliminacion) {
                Map<String, List<String>> cruces = new LinkedHashMap<>();
                for (int i = 0; i < cantidad; i += 2) {
                    cruces.put("CRUCE-" + (i / 2 + 1), List.of(inscritos.get(i).id(), inscritos.get(i + 1).id()));
                }
                if (!((FormatoFases) formato).configurarGrupos(cruces, 1)) {
                    throw new IllegalArgumentException("No se pudo armar el cuadro de eliminación.");
                }
            } else {
                int numero = 1;
                for (int i = 0; i < cantidad; i++) {
                    for (int j = i + 1; j < cantidad; j++) {
                        formato.registrarEncuentro("E-" + numero, inscritos.get(i).id(), inscritos.get(j).id());
                        numero++;
                    }
                }
            }
            torneo.iniciar(formato);
            return ResultadoOperacion.correcto(
                    "Competencia iniciada con " + formato.getEncuentros().size() + " encuentros programados.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }

    }

    public ResultadoOperacion registrarResultado(Sesion sesion, String torneoId, String encuentroId, int local,
            int visitante, String desempate) {
        try {
            exigirOrganizador(sesion);
            TorneoInterno torneo = exigirTorneo(torneoId);
            FormatoLiga formato = torneo.getFormato();
            if (torneo.estado(ahora()) != EstadoTorneo.EN_CURSO || formato == null) {
                throw new IllegalArgumentException(
                        "Solo se registran resultados en torneos en curso con la competencia iniciada");
            }
            EncuentroTorneo encuentro = formato.buscarEncuentro(encuentroId);
            if (encuentro == null) {
                throw new IllegalArgumentException("El encuentro no pertenece a este torneo");
            }
            if (encuentro.completo()) {
                throw new IllegalArgumentException("El encuentro ya tiene resultado registrado");
            }
            if (!torneo.getDeporte().isColectivo()) {
                int objetivo = formato.getSetsParaGanar();
                if (Math.max(local, visitante) != objetivo || Math.min(local, visitante) < 0
                        || Math.min(local, visitante) >= objetivo) {
                    throw new IllegalArgumentException("En " + torneo.getDeporte().getNombre()
                            + " el ganador debe llegar a " + objetivo + " sets y no hay empates");
                }
            }
            if (!formato.registrarMarcador(encuentroId, local, visitante, desempate)) {
                throw new IllegalArgumentException("El resultado no cumple las reglas del deporte");
            }
            if (formato instanceof FormatoFases fases) {
                fases.avanzar();
            }
            return ResultadoOperacion.correcto("Resultado registrado");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion finalizar(Sesion sesion, String torneoId) {
        try {
            exigirOrganizador(sesion);
            TorneoInterno torneo = exigirTorneo(torneoId);
            if (torneo.isFinalizado()) {
                throw new IllegalArgumentException("El torneo ya finalizo no se generan premios de nuevo");
            }
            FormatoLiga formato = torneo.getFormato();
            if (formato == null) {
                throw new IllegalArgumentException("El torneo no tiene encuentros registrados");
            }
            long pendientes = formato.getEncuentros().stream().filter(e -> !e.completo()).count();
            String ganador;
            if (formato instanceof FormatoFases) {
                ganador = formato.getCampeon();
                if (ganador == null) {
                    throw new IllegalArgumentException(
                            "Falta completar la eliminación: quedan " + pendientes + " encuentros pendientes");
                }
            } else {
                if (pendientes > 0) {
                    throw new IllegalArgumentException("Faltan " + pendientes + " resultados para cerrar la tabla.");
                }
                ganador = formato.getTabla().get(0).participanteId();
            }
            List<String> posiciones = new ArrayList<>();
            posiciones.add(ganador);
            for (FormatoLiga.Fila fila : formato.getTabla()) {
                if (!fila.participanteId().equals(ganador)) {
                    posiciones.add(fila.participanteId());
                }
            }
            torneo.finalizar(posiciones, ganador);
            String mensaje = "Torneo finalizado. Ganador: " + ganador + ".";
            if (torneo.getTipo() == TipoTorneo.RECREATIVO) {
                mensaje += " Bonos emitidos: " + club.getBonos().emitir(torneo).size() + ".";
            }
            return ResultadoOperacion.correcto(mensaje);
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }
}
