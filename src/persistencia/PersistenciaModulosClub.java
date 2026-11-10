package persistencia;

import aplicacion.EstadoClub;
import java.time.*;
import java.util.*;
import modelo.administracion.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.entrenamientos.*;
import modelo.partidos.*;
import modelo.reservas.Instalacion;
import modelo.socios.*;
import modelo.usuarios.*;
import org.json.*;

final class PersistenciaModulosClub {

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private static Object nulo(Object objeto) {
        if (objeto == null) {
            return JSONObject.NULL;
        } else {
            return objeto;
        }
    }

    static void salvar(JSONObject raiz, EstadoClub c) {
        JSONArray equipos = new JSONArray(), pagos = new JSONArray(), entrenamientos = new JSONArray(),
                competencias = new JSONArray(), nominas = new JSONArray(), partidos = new JSONArray();
        for (Equipo e : c.getEquipos().getEquipos()) {
            exigir(c.buscarDeporte(e.getDeporte().getId()) == e.getDeporte(), "Deporte de equipo no canónico.");
            JSONObject j = new JSONObject().put("id", e.getId()).put("nombre", e.getNombre())
                    .put("deporteId", e.getDeporte().getId()).put("categoria", e.getCategoria())
                    .put("cupoMaximo", e.getCupoMaximo()).put("activo", e.estaActivo());
            JSONArray inscripciones = new JSONArray();
            for (InscripcionEquipo i : e.getHistorialInscripciones()) {
                exigir(c.buscarJugador(i.getJugador().getId()) == i.getJugador(), "Jugador de equipo no canónico.");
                inscripciones.put(new JSONObject().put("jugadorId", i.getJugador().getId())
                        .put("posicion", i.getPosicion()).put("dorsal", i.getNumeroCamiseta())
                        .put("activa", i.estaActiva()).put("motivoRetiro", i.getMotivoRetiro()));
            }
            equipos.put(j.put("inscripciones", inscripciones));
        }
        for (PagoMensualidad p : c.getSocios().getPagosMensualidades()) {
            pagos.put(new JSONObject().put("id", p.getIdPago()).put("jugadorId", p.getJugador().getId())
                    .put("importe", p.getImporteRecibido()).put("fecha", p.getFechaPago().toString())
                    .put("periodo", p.getPeriodo()));
        }
        for (Entrenamiento e : c.getEntrenamientos().getEntrenamientos()) {
            exigir(c.getVentas().buscarEmpleadoPorCodigo(e.getEntrenador().getCodigoDescuento()) == e.getEntrenador()
                    && c.getReservas().buscarInstalacion(e.getInstalacion().getId()) == e.getInstalacion(),
                    "Entrenamiento con referencias no canónicas.");
            Object equipoIdJson;
            if (e.getEquipo() == null) {
                equipoIdJson = JSONObject.NULL;
            } else {
                equipoIdJson = e.getEquipo().getId();
            }
            Object disciplinaIdJson;
            if (e.getDisciplina() == null) {
                disciplinaIdJson = JSONObject.NULL;
            } else {
                disciplinaIdJson = e.getDisciplina().getId();
            }
            Object nivelGrupoJson;
            if (e.getNivelGrupo() == null) {
                nivelGrupoJson = JSONObject.NULL;
            } else {
                nivelGrupoJson = e.getNivelGrupo().name();
            }
            JSONObject j = new JSONObject().put("id", e.getId()).put("inicio", e.getInicio().toString())
                    .put("duracion", e.getDuracionMinutos()).put("entrenadorId", e.getEntrenador().getId())
                    .put("instalacionId", e.getInstalacion().getId()).put("equipoId", equipoIdJson)
                    .put("disciplinaId", disciplinaIdJson).put("nivelGrupo", nivelGrupoJson)
                    .put("cancelado", e.estaCancelada()).put("cambiosAgenda", PersistenciaAcademiasAgenda.cambios(e));
            JSONArray convocados = new JSONArray(), asistencias = new JSONArray();
            for (Jugador jugador : e.getConvocados()) {
                exigir(c.buscarJugador(jugador.getId()) == jugador, "Convocado no canónico.");
                convocados.put(jugador.getId());
            }
            for (AsistenciaEvaluacion a : e.getRegistroAsistencia()) {
                asistencias.put(new JSONObject().put("jugadorId", a.getJugador().getId())
                        .put("estado", a.getEstadoAsistencia().name()).put("nota", nulo(a.getCalificacion()))
                        .put("comentario", nulo(a.getComentario())));
            }
            entrenamientos.put(j.put("convocados", convocados).put("asistencias", asistencias));
        }
        for (Competencia co : c.getCompetencias().getCompetencias()) {
            exigir(co.getFormato() instanceof FormatoLiga, "Formato de competencia no soportado por el esquema.");
            FormatoLiga liga = (FormatoLiga) co.getFormato();
            String tipoFormato;
            if (liga instanceof FormatoFases) {
                tipoFormato = "FASES";
            } else {
                tipoFormato = "LIGA";
            }
            competencias.put(new JSONObject().put("id", co.getId()).put("nombre", co.getNombre())
                    .put("organizador", co.getOrganizador()).put("temporada", co.getTemporada())
                    .put("deporteId", co.getDeporte().getId()).put("categoria", co.getCategoria())
                    .put("setsParaGanar", co.getSetsParaGanar()).put("formato", tipoFormato)
                    .put("torneo", PersistenciaTorneos.salvar(liga))
                    .put("puntos", new JSONObject(liga.getPuntosEquipos()))
                    .put("procesados", new JSONArray(liga.getPartidosProcesados())));
        }
        for (NominaCompetencia nomina : c.getCompetencias().getNominas()) {
            JSONArray ids = new JSONArray();
            for (Jugador j : nomina.getJugadores()) {
                exigir(c.buscarJugador(j.getId()) == j, "Jugador de nómina no canónico.");
                ids.put(j.getId());
            }
            nominas.put(new JSONObject().put("competenciaId", nomina.getCompetencia().getId())
                    .put("equipoId", nomina.getEquipo().getId()).put("jugadores", ids).put("actuales",
                            new JSONArray(nomina.getJugadoresActuales().stream().map(Jugador::getId).toList())));
        }
        for (Partido p : c.getPartidos().getPartidos()) {
            JSONObject j = new JSONObject().put("id", p.getId()).put("fecha", p.getFechaHora().toString())
                    .put("deporteId", p.getDeporte().getId());
            if (p instanceof PartidoInterno i) {
                j.put("tipo", "INTERNO").put("localId", i.getLocal().getId())
                        .put("visitanteId", i.getVisitante().getId()).put("marcadorLocal", nulo(i.getMarcadorLocal()))
                        .put("marcadorVisitante", nulo(i.getMarcadorVisitante()));
            } else if (p instanceof PartidoCompetencia co) {
                Object equipoRivalClubIdJson;
                if (co.getEquipoRivalClub() == null) {
                    equipoRivalClubIdJson = JSONObject.NULL;
                } else {
                    equipoRivalClubIdJson = co.getEquipoRivalClub().getId();
                }
                Object ganadorDesempateIdJson;
                if (co.getGanadorDesempateId() == null) {
                    ganadorDesempateIdJson = JSONObject.NULL;
                } else {
                    ganadorDesempateIdJson = co.getGanadorDesempateId();
                }
                j.put("tipo", "COMPETENCIA").put("competenciaId", co.getCompetencia().getId())
                        .put("equipoId", co.getEquipoClub().getId()).put("institucionRival", co.getInstitucionRival())
                        .put("rivalId", co.getRivalId()).put("encuentroTorneoId", co.getEncuentroTorneoId())
                        .put("equipoRivalClubId", equipoRivalClubIdJson)
                        .put("ganadorDesempateId", ganadorDesempateIdJson).put("escenario", co.getEscenario())
                        .put("marcadorLocal", nulo(co.getMarcadorClub()))
                        .put("marcadorVisitante", nulo(co.getMarcadorRival()));
            } else if (p instanceof PartidoAcademia a) {
                JSONObject elo = new JSONObject();
                a.getCambiosElo().forEach((id, cambio) -> elo.put(id,
                        new JSONObject().put("anterior", cambio.anterior()).put("nuevo", cambio.nuevo())));
                j.put("tipo", "ACADEMIA").put("modalidad", a.getModalidad())
                        .put("ladoA", new JSONArray(a.getLadoA().stream().map(Jugador::getId).toList()))
                        .put("ladoB", new JSONArray(a.getLadoB().stream().map(Jugador::getId).toList()))
                        .put("diferenciaNiveles", a.getDiferenciaNiveles())
                        .put("excepcionEntrenador", a.permiteExcepcionEntrenador()).put("k", a.getK()).put("elo", elo)
                        .put("marcadorLocal", nulo(a.getMarcadorA())).put("marcadorVisitante", nulo(a.getMarcadorB()));
            } else {
                throw new IllegalArgumentException("Tipo de partido no soportado.");
            }
            Object instalacionIdJson;
            if (p.getInstalacion() == null) {
                instalacionIdJson = JSONObject.NULL;
            } else {
                instalacionIdJson = p.getInstalacion().getId();
            }
            j.put("selecciones", PersistenciaDisciplina.listas(p.getSelecciones()))
                    .put("plantillas", PersistenciaDisciplina.listas(p.getPlantillas()))
                    .put("instalacionId", instalacionIdJson).put("duracion", p.getDuracionMinutos())
                    .put("cambiosAgenda", PersistenciaAcademiasAgenda.cambios(p)).put("cancelado", p.estaCancelado())
                    .put("cerrado", p.estaCerrado()).put("legado", p.esLegado())
                    .put("setsParaGanar", p.getSetsParaGanar());
            JSONArray estadisticas = new JSONArray(), sanciones = new JSONArray();
            for (EstadisticaPartido e : p.getEstadisticas()) {
                exigir(c.buscarJugador(e.getJugador().getId()) == e.getJugador(),
                        "Estadística con jugador no canónico.");
                JSONObject metricas = new JSONObject();
                for (var entrada : e.getMetricas().entrySet()) {
                    metricas.put(entrada.getKey().name(), entrada.getValue());
                }
                estadisticas.put(new JSONObject().put("jugadorId", e.getJugador().getId()).put("metricas", metricas)
                        .put("elo", nulo(e.getEloResultante())));
            }
            for (Sancion s : p.getSanciones()) {
                exigir(c.buscarJugador(s.getJugador().getId()) == s.getJugador(), "Sanción con jugador no canónico.");
                sanciones
                        .put(new JSONObject().put("jugadorId", s.getJugador().getId()).put("tipo", s.getTipo().name()));
            }
            partidos.put(j.put("estadisticas", estadisticas).put("sanciones", sanciones));
        }
        JSONArray turnos = new JSONArray(), horarios = new JSONArray(), solicitudes = new JSONArray();
        for (Turno t : c.getPersonal().getTurnos()) {
            turnos.put(turno(t));
        }
        for (HorarioAtencion h : c.getPersonal().getHorarios()) {
            horarios.put(new JSONObject().put("dia", h.dia().name()).put("inicio", h.inicio().toString()).put("fin",
                    h.fin().toString()));
        }
        for (SolicitudCambioTurno solicitud : c.getPersonal().getSolicitudes()) {
            solicitudes.put(new JSONObject().put("id", solicitud.id()).put("empleadoId", solicitud.empleadoId())
                    .put("anterior", turno(solicitud.anterior())).put("propuesto", turno(solicitud.propuesto()))
                    .put("estado", solicitud.estado().name()).put("motivo", solicitud.motivo()));
        }
        raiz.put("equipos", equipos).put("mensualidades", pagos).put("entrenamientos", entrenamientos)
                .put("competencias", competencias).put("nominas", nominas).put("partidos", partidos)
                .put("turnos", turnos).put("horariosAtencion", horarios).put("solicitudesTurno", solicitudes);
    }

    private static boolean participa(EstadoClub c, Partido p, Jugador j) {
        if (j == null) {
            return false;
        }

        if (p instanceof PartidoAcademia a) {
            return a.esParticipante(j);
        }

        if (!p.getSelecciones().isEmpty()) {
            return p.esParticipante(j);
        }

        if (p instanceof PartidoInterno i) {
            return java.util.stream.Stream.concat(i.getLocal().getHistorialInscripciones().stream(),
                    i.getVisitante().getHistorialInscripciones().stream()).anyMatch(in -> in.getJugador() == j);
        }

        if (p instanceof PartidoCompetencia pc) {
            NominaCompetencia n = c.getCompetencias().buscarNomina(pc.getCompetencia().getId(),
                    pc.getEquipoClub().getId());
            return n != null && n.contieneJugador(j);
        }
        return false;
    }

    private static List<Jugador> jugadores(JSONArray ids, EstadoClub c) {
        List<Jugador> lista = new ArrayList<>();
        for (int i = 0; i < ids.length(); i++) {
            Jugador j = c.buscarJugador(ids.getString(i));
            exigir(j != null, "Participante de academia inexistente.");
            lista.add(j);
        }
        return lista;
    }

    private static JSONObject turno(Turno t) {
        return new JSONObject().put("id", t.id()).put("empleadoId", t.empleadoId()).put("dia", t.dia().name())
                .put("inicio", t.inicio().toString()).put("fin", t.fin().toString());
    }

    private static Turno leerTurno(JSONObject j) {
        return new Turno(j.getString("id"), j.getString("empleadoId"), DayOfWeek.valueOf(j.getString("dia")),
                LocalTime.parse(j.getString("inicio")), LocalTime.parse(j.getString("fin")));
    }

    static void cargar(JSONObject raiz, EstadoClub c) {
        for (String seccion : List.of("equipos", "mensualidades", "entrenamientos", "competencias", "nominas",
                "partidos", "turnos", "horariosAtencion", "solicitudesTurno")) {
            raiz.getJSONArray(seccion);
        }
        JSONArray equipos = raiz.getJSONArray("equipos");
        for (int i = 0; i < equipos.length(); i++) {
            JSONObject j = equipos.getJSONObject(i);
            Deporte deporte = c.buscarDeporte(j.getString("deporteId"));
            exigir(deporte != null && deporte.isColectivo() && j.getInt("cupoMaximo") > 0
                    && Set.of("Sub-13", "Sub-15", "Sub-17", "Mayores").contains(j.getString("categoria")),
                    "Equipo incompatible.");
            Equipo equipo = new Equipo(j.getString("id"), j.getString("nombre"), deporte, j.getString("categoria"),
                    j.getInt("cupoMaximo"));
            equipo.restaurarActivo(j.optBoolean("activo", true));
            exigir(c.getEquipos().registrarEquipo(equipo), "Equipo inválido o duplicado.");
            JSONArray inscripciones = j.getJSONArray("inscripciones");
            for (int k = 0; k < inscripciones.length(); k++) {
                JSONObject in = inscripciones.getJSONObject(k);
                exigir(c.getEquipos().cargarInscripcion(c.buscarJugador(in.getString("jugadorId")), equipo,
                        in.getString("posicion"), in.getInt("dorsal"), in.optBoolean("activa", true),
                        in.optString("motivoRetiro", "")), "Inscripción inválida o jugador inexistente.");
            }
        }
        JSONArray pagos = raiz.getJSONArray("mensualidades");
        for (int i = 0; i < pagos.length(); i++) {
            JSONObject j = pagos.getJSONObject(i);
            c.getSocios().cargarPago(new PagoMensualidad(j.getString("id"), c.buscarJugador(j.getString("jugadorId")),
                    j.getDouble("importe"), LocalDate.parse(j.getString("fecha")), j.getString("periodo")));
        }
        JSONArray competencias = raiz.getJSONArray("competencias");
        for (int i = 0; i < competencias.length(); i++) {
            JSONObject j = competencias.getJSONObject(i);
            FormatoLiga formato;
            if (raiz.getInt("version") >= 6) {
                formato = PersistenciaTorneos.leer(j, c);
            } else {
                formato = new FormatoLiga();
            }
            exigir(c.getCompetencias()
                    .registrarCompetencia(new Competencia(j.getString("id"), j.getString("nombre"),
                            j.getString("organizador"), j.getInt("temporada"),
                            c.buscarDeporte(j.getString("deporteId")), j.getString("categoria"), formato)),
                    "Competencia inválida o duplicada.");
        }
        for (int i = 0; i < competencias.length(); i++) {
            JSONObject co = competencias.getJSONObject(i);
            if (raiz.getInt("version") < 6) {
                exigir(c.getCompetencias().buscarCompetencia(co.getString("id"))
                        .configurarSetsParaGanar(co.optInt("setsParaGanar", 3)), "Configuración de sets inválida.");
            } else {
                c.getCompetencias().buscarCompetencia(co.getString("id"))
                        .restaurarSetsParaGanar(co.getInt("setsParaGanar"));
            }
        }
        JSONArray nominas = raiz.getJSONArray("nominas");
        for (int i = 0; i < nominas.length(); i++) {
            JSONObject j = nominas.getJSONObject(i);
            NominaCompetencia n = new NominaCompetencia(
                    c.getCompetencias().buscarCompetencia(j.getString("competenciaId")),
                    c.getEquipos().buscarEquipo(j.getString("equipoId")));
            JSONArray jugadores = j.getJSONArray("jugadores");
            for (int k = 0; k < jugadores.length(); k++) {
                exigir(n.cargarJugadorHistorico(c.buscarJugador(jugadores.getString(k))),
                        "Jugador inválido o ajeno a la nómina.");
            }
            if (raiz.getInt("version") >= 6) {
                JSONArray actuales = j.getJSONArray("actuales");
                List<Jugador> lista = new ArrayList<>();
                for (int k = 0; k < actuales.length(); k++) {
                    Jugador jugador = c.buscarJugador(actuales.getString(k));
                    exigir(jugador != null && n.contieneJugador(jugador), "Miembro actual sin historia en nómina.");
                    lista.add(jugador);
                }
                n.actualizarActuales(lista);
            }
            exigir(c.getCompetencias().cargarNomina(n), "Nómina inválida o duplicada.");
        }
        JSONArray entrenamientos = raiz.getJSONArray("entrenamientos");
        for (int i = 0; i < entrenamientos.length(); i++) {
            JSONObject j = entrenamientos.getJSONObject(i);
            Empleado entrenador = c.getVentas().buscarEmpleadoPorCodigo("EMP-" + j.getString("entrenadorId"));
            Equipo equipo;
            if (j.isNull("equipoId")) {
                equipo = null;
            } else {
                equipo = c.getEquipos().buscarEquipo(j.getString("equipoId"));
            }
            exigir(j.isNull("equipoId") || equipo != null, "Equipo de entrenamiento inexistente.");
            Entrenamiento e = new Entrenamiento(j.getString("id"), LocalDateTime.parse(j.getString("inicio")),
                    j.getInt("duracion"), entrenador, c.getReservas().buscarInstalacion(j.getString("instalacionId")),
                    equipo);
            exigir(equipo == null
                    || (e.getInstalacion() != null && equipo.getDeporte() == e.getInstalacion().getDeporte()),
                    "Deporte del entrenamiento incompatible.");
            if (raiz.getInt("version") >= 6 && !j.isNull("disciplinaId") && equipo == null) {
                e.definirGrupoAcademia(c.buscarDeporte(j.getString("disciplinaId")),
                        NivelAcademia.valueOf(j.getString("nivelGrupo")));
            }
            JSONArray convocados = j.getJSONArray("convocados"), asistencias = j.getJSONArray("asistencias");
            for (int k = 0; k < convocados.length(); k++) {
                Jugador jugador = c.buscarJugador(convocados.getString(k));
                exigir(jugador != null && (equipo == null
                        || equipo.getHistorialInscripciones().stream().anyMatch(in -> in.getJugador() == jugador))
                        && e.convocarJugador(jugador), "Convocado inexistente, ajeno o duplicado.");
            }
            for (int k = 0; k < asistencias.length(); k++) {
                JSONObject a = asistencias.getJSONObject(k);
                Double notaRegistrada;
                if (a.isNull("nota")) {
                    notaRegistrada = null;
                } else {
                    notaRegistrada = a.getDouble("nota");
                }
                String datoRegistrado;
                if (a.isNull("comentario")) {
                    datoRegistrado = null;
                } else {
                    datoRegistrado = a.getString("comentario");
                }
                exigir(e.registrarAsistencia(new AsistenciaEvaluacion(c.buscarJugador(a.getString("jugadorId")), e,
                        EstadoAsistencia.valueOf(a.getString("estado")), notaRegistrada, datoRegistrado)),
                        "Asistencia inválida.");
            }
            if (raiz.getInt("version") >= 6) {
                e.restaurarCancelado(j.getBoolean("cancelado"));
                PersistenciaAcademiasAgenda.cargarCambios(e, j.getJSONArray("cambiosAgenda"), c);
            }
            exigir(c.getEntrenamientos().cargarEntrenamiento(e), "Entrenamiento inválido, duplicado o con cruce.");
        }
        JSONArray partidos = raiz.getJSONArray("partidos");
        for (int i = 0; i < partidos.length(); i++) {
            JSONObject j = partidos.getJSONObject(i);
            Partido partido;
            Deporte deporte = c.buscarDeporte(j.getString("deporteId"));
            LocalDateTime fecha = LocalDateTime.parse(j.getString("fecha"));
            if (j.getString("tipo").equals("INTERNO")) {
                PartidoInterno interno = new PartidoInterno(j.getString("id"), fecha, deporte,
                        c.getEquipos().buscarEquipo(j.getString("localId")),
                        c.getEquipos().buscarEquipo(j.getString("visitanteId")));
                exigir(interno.sonEquiposCompatibles(), "Equipos de partido incompatibles.");
                partido = interno;
            } else if (j.getString("tipo").equals("COMPETENCIA")) {
                Competencia co = c.getCompetencias().buscarCompetencia(j.getString("competenciaId"));
                Equipo equipo = c.getEquipos().buscarEquipo(j.getString("equipoId"));
                exigir(co != null && equipo != null && co.getDeporte() == deporte && equipo.getDeporte() == deporte
                        && co.getCategoria().equals(equipo.getCategoria()),
                        "Partido oficial con referencias incompatibles.");
                partido = new PartidoCompetencia(j.getString("id"), fecha, deporte, co, equipo,
                        j.getString("institucionRival"), j.getString("escenario"));
                if (raiz.getInt("version") >= 6) {
                    PartidoCompetencia pc = (PartidoCompetencia) partido;
                    Equipo rival;
                    if (j.isNull("equipoRivalClubId")) {
                        rival = null;
                    } else {
                        rival = c.getEquipos().buscarEquipo(j.getString("equipoRivalClubId"));
                    }
                    exigir(j.isNull("equipoRivalClubId") || rival != null, "Equipo rival del club inexistente.");
                    exigir(pc.vincularEncuentro(j.getString("encuentroTorneoId"), j.getString("rivalId"),
                            j.getString("institucionRival"), rival), "Rival/cruce inválidos.");
                    if (!j.isNull("ganadorDesempateId")) {
                        exigir(pc.definirDesempate(j.getString("ganadorDesempateId")),
                                "Ganador de desempate inválido.");
                    }
                }
            } else if (j.getString("tipo").equals("ACADEMIA") && raiz.getInt("version") >= 6) {
                List<Jugador> ladoA = jugadores(j.getJSONArray("ladoA"), c),
                        ladoB = jugadores(j.getJSONArray("ladoB"), c);
                partido = new PartidoAcademia(j.getString("id"), fecha, deporte, j.getString("modalidad"), ladoA, ladoB,
                        j.getInt("diferenciaNiveles"), j.getBoolean("excepcionEntrenador"), j.getInt("k"));
            } else {
                throw new IllegalArgumentException("Tipo de partido desconocido.");
            }
            exigir(partido.configurarSetsParaGanar(j.optInt("setsParaGanar", 3)),
                    "Configuración de sets de partido inválida.");
            if (raiz.getInt("version") >= 6) {
                Instalacion instalacion;
                if (j.isNull("instalacionId")) {
                    instalacion = null;
                } else {
                    instalacion = c.getReservas().buscarInstalacion(j.getString("instalacionId"));
                }
                exigir(j.isNull("instalacionId") || instalacion != null, "Instalación de partido inexistente.");
                exigir(partido.configurarOcupacion(instalacion, j.getInt("duracion")),
                        "Ocupación de partido inválida.");
                PersistenciaAcademiasAgenda.cargarCambios(partido, j.getJSONArray("cambiosAgenda"), c);
            } else {
                partido.restaurarHorario(null, fecha, 0);
            }
            exigir(j.isNull("marcadorLocal") == j.isNull("marcadorVisitante"), "Marcador incompleto.");
            if (!j.isNull("marcadorLocal")) {
                boolean resultado;
                if (partido instanceof PartidoAcademia academia) {
                    resultado = academia.restaurarResultado(j.getInt("marcadorLocal"), j.getInt("marcadorVisitante"));
                } else if (partido instanceof PartidoInterno in) {
                    resultado = in.restaurarResultado(j.getInt("marcadorLocal"), j.getInt("marcadorVisitante"),
                            raiz.getInt("version") < 5 || j.optBoolean("legado", false));
                } else {
                    resultado = ((PartidoCompetencia) partido).restaurarResultado(j.getInt("marcadorLocal"),
                            j.getInt("marcadorVisitante"),
                            c.getCompetencias().buscarNomina(((PartidoCompetencia) partido).getCompetencia().getId(),
                                    ((PartidoCompetencia) partido).getEquipoClub().getId()),
                            raiz.getInt("version") < 5 || j.optBoolean("legado", false));
                }
                exigir(resultado, "Resultado inválido.");
            }

            if (raiz.getInt("version") >= 5) {
                PersistenciaDisciplina.cargarListas(j.getJSONObject("selecciones"), partido, c, false);
                PersistenciaDisciplina.cargarListas(j.getJSONObject("plantillas"), partido, c, true);
                if (partido.tieneResultado() && !j.getBoolean("legado")) {
                    for (Equipo equipo : partido.getEquiposClub()) {
                        exigir(partido.getPlantillas().containsKey(equipo.getId())
                                && partido.getPlantillas().get(equipo.getId())
                                        .containsAll(partido.getParticipantes(equipo.getId()))
                                && partido.getParticipantes(equipo.getId()).size() >= deporte.getMinimoJugadores(),
                                "Falta plantilla/alineación del partido jugado.");
                    }
                }
            }

            if (partido instanceof PartidoAcademia academia) {
                JSONObject elo = j.getJSONObject("elo");
                for (String id : elo.keySet()) {
                    JSONObject cambio = elo.getJSONObject(id);
                    academia.cargarCambioElo(id,
                            new PartidoAcademia.CambioElo(cambio.getDouble("anterior"), cambio.getDouble("nuevo")));
                }
            }
            JSONArray estadisticas = j.getJSONArray("estadisticas"), sanciones = j.getJSONArray("sanciones");
            for (int k = 0; k < estadisticas.length(); k++) {
                JSONObject e = estadisticas.getJSONObject(k);
                EstadisticaPartido estadistica = new EstadisticaPartido(c.buscarJugador(e.getString("jugadorId")),
                        partido);
                JSONObject metricas = e.getJSONObject("metricas");
                for (String tipo : metricas.keySet()) {
                    exigir(estadistica.registrarMetrica(TipoEstadistica.valueOf(tipo), metricas.getInt(tipo)),
                            "Métrica inválida.");
                }
                if (!e.isNull("elo")) {
                    estadistica.cargarEloResultante(e.getInt("elo"));
                }
                exigir(participa(c, partido, estadistica.getJugador()), "Estadística de jugador ajeno al partido.");
                exigir(partido.registrarEstadistica(estadistica), "Estadística inválida o jugador inexistente.");
            }
            for (int k = 0; k < sanciones.length(); k++) {
                JSONObject sa = sanciones.getJSONObject(k);
                exigir(participa(c, partido, c.buscarJugador(sa.getString("jugadorId"))),
                        "Tarjeta de jugador ajeno al partido.");
                exigir(partido.registrarSancion(new Sancion(c.buscarJugador(sa.getString("jugadorId")), partido,
                        TipoTarjeta.valueOf(sa.getString("tipo")))), "Sanción inválida.");
            }
            partido.restaurarEstado(j.optBoolean("cancelado", false), j.optBoolean("cerrado", false),
                    (raiz.getInt("version") < 5 && partido.tieneResultado()) || j.optBoolean("legado", false));
            exigir(c.getPartidos().cargarPartido(partido), "Partido inválido, duplicado o con cruce de instalación.");
        }
        if (raiz.getInt("version") < 6) {
            for (int i = 0; i < competencias.length(); i++) {
                JSONObject j = competencias.getJSONObject(i);
                Map<String, Integer> puntos = new LinkedHashMap<>(), esperados = new LinkedHashMap<>();
                List<String> procesados = new ArrayList<>();
                JSONObject tabla = j.getJSONObject("puntos");
                for (String equipo : tabla.keySet()) {
                    exigir(c.getEquipos().buscarEquipo(equipo) != null, "Puntos de equipo inexistente.");
                    puntos.put(equipo, tabla.getInt(equipo));
                }
                JSONArray ids = j.getJSONArray("procesados");
                for (int k = 0; k < ids.length(); k++) {
                    String id = ids.getString(k);
                    Partido p = c.getPartidos().buscarPartido(id);
                    exigir(p instanceof PartidoCompetencia, "Partido procesado inexistente.");
                    PartidoCompetencia pc = (PartidoCompetencia) p;
                    exigir(pc.getCompetencia() == c.getCompetencias().buscarCompetencia(j.getString("id"))
                            && pc.getDeporte().es("FUTBOL") && pc.getMarcadorClub() != null && !procesados.contains(id),
                            "Partido procesado incompatible.");
                    int valor;
                    if (pc.getMarcadorClub() > pc.getMarcadorRival()) {
                        valor = 3;
                    } else if (pc.getMarcadorClub().equals(pc.getMarcadorRival())) {
                        valor = 1;
                    } else {
                        valor = 0;
                    }
                    esperados.merge(pc.getEquipoClub().getId(), valor, Integer::sum);
                    procesados.add(id);
                }
                exigir(puntos.equals(esperados), "Tabla de liga no coincide con partidos procesados.");
                FormatoLiga liga = (FormatoLiga) c.getCompetencias().buscarCompetencia(j.getString("id")).getFormato();
                liga.cargarEstado(puntos, procesados);
                liga.reconstruirLegado(
                        procesados.stream().map(id -> (PartidoCompetencia) c.getPartidos().buscarPartido(id)).toList());
                for (NominaCompetencia n : c.getCompetencias().getNominas()) {
                    if (n.getCompetencia().getId().equals(j.getString("id"))
                            && liga.buscarParticipante(n.getEquipo().getId()) == null) {
                        liga.registrarParticipante(new ParticipanteCompetencia(n.getEquipo().getId(),
                                n.getEquipo().getNombre(), n.getEquipo().getId(), n.getEquipo().estaActivo()));
                    }
                }
            }
        }
        JSONArray horarios = raiz.getJSONArray("horariosAtencion"), turnos = raiz.getJSONArray("turnos"),
                solicitudes = raiz.getJSONArray("solicitudesTurno");
        for (int i = 0; i < horarios.length(); i++) {
            JSONObject h = horarios.getJSONObject(i);
            c.getPersonal().cargarHorario(new HorarioAtencion(DayOfWeek.valueOf(h.getString("dia")),
                    LocalTime.parse(h.getString("inicio")), LocalTime.parse(h.getString("fin"))));
        }
        for (int i = 0; i < turnos.length(); i++) {
            c.getPersonal().cargarTurno(leerTurno(turnos.getJSONObject(i)));
        }
        for (int i = 0; i < solicitudes.length(); i++) {
            JSONObject s = solicitudes.getJSONObject(i);
            c.getPersonal()
                    .cargarSolicitud(new SolicitudCambioTurno(s.getString("id"), s.getString("empleadoId"),
                            leerTurno(s.getJSONObject("anterior")), leerTurno(s.getJSONObject("propuesto")),
                            SolicitudCambioTurno.Estado.valueOf(s.getString("estado")), s.getString("motivo")));
        }
    }
}
