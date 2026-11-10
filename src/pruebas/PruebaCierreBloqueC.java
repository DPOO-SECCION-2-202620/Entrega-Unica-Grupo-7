package pruebas;

import aplicacion.*;
import gestores.*;
import gestores.GestorAcceso.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.deportes.*;
import modelo.entrenamientos.*;
import modelo.partidos.*;
import modelo.reservas.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaCierreBloqueC {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-06T13:00:00Z"), ZoneId.of("America/Bogota"));
    static final LocalDate D = LocalDate.of(2026, 11, 7);

    static void ok(boolean c, String m) {
        if (!c) {
            throw new AssertionError(m);
        }
        System.out.println("OK: " + m);
    }

    static void si(ResultadoOperacion r) {
        ok(r.exito(), r.mensaje());
    }

    static void no(ResultadoOperacion r) {
        ok(!r.exito(), "rechazo: " + r.mensaje());
    }

    static LocalDateTime h(int hora) {
        return D.atTime(hora, 0);
    }

    static ResultadoOperacion juego(EstadoClub c, Sesion s, String id, String d, String mod, List<String> a,
            List<String> b, String i, LocalDateTime h) {
        return c.getDeportivo().programarAcademia(s, id, d, mod, a, b, i, h, 60, 2);
    }

    public static void main(String[] args) throws Exception {
        PersistenciaClubJson p = new PersistenciaClubJson(RELOJ);
        Path ruta = Path.of(args[1]);
        if (args[0].equals("crear")) {
            EstadoClub c = new EstadoClub(RELOJ);
            var admin = SoporteComercial.administrador(c);
            var s = c.getOperaciones();
            for (String id : List.of("A", "B", "C", "D", "E", "COACH")) {
                String nombreJugador;
                if (id.equals("A") || id.equals("B")) {
                    nombreJugador = "Alex";
                } else {
                    nombreJugador = id;
                }
                si(s.registrarJugador(admin, id, nombreJugador, LocalDate.of(2000, 1, 1)));
                SoporteComercial.habilitar(c, admin, id, Perfil.SOCIO);
            }
            c.altaEmpleado(admin, "COACH", "COACH");
            ok(c.actualizarCuenta(admin, "COACH", "COACH", Set.of(Perfil.SOCIO, Perfil.ENTRENADOR)),
                    "doble perfil docente-jugador");
            Sesion coach = c.getAcceso().autenticar("COACH", "ClavePrueba2026"),
                    a = c.getAcceso().autenticar("A", "ClavePrueba2026");
            for (String id : List.of("T1", "T2", "T3")) {
                si(s.registrarInstalacion(admin, new Instalacion(id, id, c.buscarDeporte("TENIS"), 4, false)));
            }
            si(s.registrarInstalacion(admin, new Instalacion("P1", "Pádel", c.buscarDeporte("PADEL"), 4, true)));
            no(s.reservar(a, "SIN-INSCRIPCION", "A", "T1", h(9), h(10), 2, "Sencillos"));
            no(c.getAcademias().inscribir(a, "A", "TENIS", NivelAcademia.AVANZADO));
            for (String id : List.of("A", "B", "C", "D", "COACH")) {
                modelo.deportes.NivelAcademia nivelSeleccionado;
                if (id.equals("B")) {
                    nivelSeleccionado = NivelAcademia.INTERMEDIO;
                } else if (id.equals("C") || id.equals("COACH")) {
                    nivelSeleccionado = NivelAcademia.AVANZADO;
                } else {
                    nivelSeleccionado = NivelAcademia.PRINCIPIANTE;
                }
                si(c.getAcademias().inscribir(admin, id, "TENIS", nivelSeleccionado));
            }
            no(c.getAcademias().inscribir(a, "A", "TENIS", NivelAcademia.PRINCIPIANTE));
            no(c.getAcademias().inscribir(admin, "A", "FUTBOL", NivelAcademia.PRINCIPIANTE));
            ok(c.getAcademias().consultarRankings("MESA").isEmpty()
                    && c.getSocios().consultarRankingsDisciplina("MESA").isEmpty(),
                    "sin ranking fantasma de disciplina no inscrita");
            no(juego(c, coach, "NIVELES", "TENIS", "Sencillos", List.of("A"), List.of("C"), "T1", h(9)));
            si(juego(c, coach, "M1", "TENIS", "Sencillos", List.of("A"), List.of("B"), "T1", h(9)));
            no(juego(c, a, "SIN-PERMISO", "TENIS", "Sencillos", List.of("A"), List.of("B"), "T2", h(12)));
            no(s.reservar(admin, "COLISION", "D", "T1", h(9), h(10), 2, "Sencillos"));
            no(c.getDeportivo().programarGrupo(coach, "COLISION-G", "TENIS", NivelAcademia.PRINCIPIANTE, "COACH", "T1",
                    h(9), 60));
            no(s.reservar(admin, "USUARIO", "A", "T2", h(9), h(10), 2, "Sencillos"));
            si(c.getDeportivo().programarGrupo(coach, "GRUPO", "TENIS", NivelAcademia.PRINCIPIANTE, "COACH", "T2",
                    h(11), 60));
            no(c.getDeportivo().programarGrupo(coach, "DOCENTE-OCUPADO", "TENIS", NivelAcademia.PRINCIPIANTE, "COACH",
                    "T3", h(11), 60));
            no(s.convocar(coach, "GRUPO", "B"));
            si(s.convocar(coach, "GRUPO", "A"));
            si(s.convocar(coach, "GRUPO", "D"));
            for (double nota : new double[] { Double.NaN, Double.POSITIVE_INFINITY, 0, 6, }) {
                no(s.registrarAsistencia(coach, "GRUPO", "A", EstadoAsistencia.ASISTIO, nota, ""));
            }
            no(s.registrarAsistencia(coach, "GRUPO", "B", EstadoAsistencia.ASISTIO, 4.0, "No convocado"));
            si(s.registrarAsistencia(coach, "GRUPO", "A", EstadoAsistencia.LLEGO_TARDE, null, "Tráfico"));
            si(s.registrarAsistencia(coach, "GRUPO", "D", EstadoAsistencia.FALTO, null, ""));
            no(s.registrarAsistencia(coach, "GRUPO", "A", EstadoAsistencia.ASISTIO, 4.0, "Duplicado"));
            no(c.getDeportivo().cancelar(coach, c.getEntrenamientos().buscarEntrenamiento("GRUPO"), "Ya evaluado"));
            si(s.registrarMetrica(coach, "M1", "A", TipoEstadistica.ACES, 3));
            no(s.registrarResultado(coach, "M1", 1, 1));
            ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1000, "rechazo sin modificar ranking");
            si(s.registrarResultado(coach, "M1", 2, 0));
            no(s.registrarResultado(coach, "M1", 2, 0));
            ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1016
                    && c.buscarJugador("B").obtenerRankingPorDisciplina("TENIS") == 984,
                    "ELO exacto 1016/984 y niveles adyacentes permitidos");
            ok(c.getPartidos().buscarPartido("M1").getEstadisticas().stream()
                    .anyMatch(e -> e.getJugador().getId().equals("A") && e.getMetricas().get(TipoEstadistica.ACES) == 3
                            && e.getEloResultante() == 1016),
                    "métricas previas y ELO resultante conservados");
            si(juego(c, coach, "EXCEPCION", "TENIS", "Sencillos", List.of("D"), List.of("COACH"), "T1", h(12)));
            si(c.getDeportivo().cancelar(coach, c.getPartidos().buscarPartido("EXCEPCION"), "Demostración excepción"));
            no(juego(c, coach, "EXCEPCION-DOBLES", "TENIS", "Dobles", List.of("A", "COACH"), List.of("C", "D"), "T1",
                    h(13)));
            si(c.getAcademias().actualizar(admin, "C", "TENIS", NivelAcademia.INTERMEDIO, true));
            si(juego(c, coach, "DOBLES", "TENIS", "Dobles", List.of("A", "D"), List.of("B", "C"), "T1", h(13)));
            si(s.registrarResultado(coach, "DOBLES", 2, 1));
            ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1031
                    && c.buscarJugador("D").obtenerRankingPorDisciplina("TENIS") == 1015
                    && c.buscarJugador("B").obtenerRankingPorDisciplina("TENIS") == 969
                    && c.buscarJugador("C").obtenerRankingPorDisciplina("TENIS") == 985,
                    "dobles aplica delta basado en promedio a los cuatro");
            for (String id : List.of("A", "B", "C", "D")) {
                si(c.getAcademias().inscribir(admin, id, "PADEL", NivelAcademia.PRINCIPIANTE));
            }
            no(juego(c, coach, "PADEL2", "PADEL", "Sencillos", List.of("A"), List.of("B"), "P1", h(14)));
            no(s.reservar(admin, "PADEL-R2", "A", "P1", h(14), h(15), 2, "Sencillos"));
            si(juego(c, coach, "PADEL4", "PADEL", "Dobles", List.of("A", "B"), List.of("C", "D"), "P1", h(14)));
            si(s.registrarResultado(coach, "PADEL4", 2, 0));
            ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1031
                    && c.buscarJugador("A").obtenerRankingPorDisciplina("PADEL") == 1016, "disciplinas separadas");
            si(s.registrarInstalacion(admin,
                    new Instalacion("T4", "Solo sencillos", c.buscarDeporte("TENIS"), 2, false)));
            no(juego(c, coach, "DOBLES-CAPACIDAD", "TENIS", "Dobles", List.of("A", "D"), List.of("B", "C"), "T4",
                    h(15)));
            si(s.reservar(admin, "RES-DOBLES", "D", "T2", h(15), h(16), 4, "Dobles"));
            no(c.getDeportivo().reprogramar(admin, c.getReservas().buscarReserva("RES-DOBLES"), "T4", h(15), 60,
                    "Capacidad insuficiente"));
            si(c.getAcademias().configurar(admin, new GestorAcademias.Configuracion(0, false, 40, 1200)));
            no(juego(c, coach, "REGLA-CERO", "TENIS", "Sencillos", List.of("A"), List.of("B"), "T1", h(15)));
            si(c.getAcademias().inscribir(admin, "E", "MESA", NivelAcademia.PRINCIPIANTE));
            ok(c.buscarJugador("E").obtenerRankingPorDisciplina("MESA") == 1200,
                    "ranking inicial configurable crea un movimiento real");
            si(c.getAcademias().configurar(admin, new GestorAcademias.Configuracion(1, true, 32, 1000)));
            si(c.getAcademias().actualizar(admin, "C", "PADEL", NivelAcademia.PRINCIPIANTE, false));
            no(s.reservar(admin, "INACTIVA", "C", "P1", h(15), h(16), 4, "Dobles"));
            si(c.getAcademias().actualizar(admin, "C", "PADEL", NivelAcademia.PRINCIPIANTE, true));
            si(c.getAgenda().configurarMaximo(admin, "TENIS", 2));
            // Dos intervalos disjuntos en la misma cancha: el tercero ocupa la otra sin exceder dos simultáneas.
            si(s.reservar(admin, "R1", "A", "T1", h(16), h(17), 2, "Sencillos"));
            si(s.reservar(admin, "R2", "B", "T1", h(17), h(18), 2, "Sencillos"));
            si(s.reservar(admin, "R3", "C", "T2", h(16), h(18), 2, "Sencillos"));
            no(s.reservar(admin, "MAXIMO", "D", "T3", h(16), h(17), 2, "Sencillos"));
            no(c.getAgenda().configurarMaximo(admin, "TENIS", 1));
            var r1 = c.getReservas().buscarReserva("R1");
            no(c.getDeportivo().reprogramar(a, r1, "T2", h(16), 60, "Cruce"));
            ok(r1.getInstalacion().getId().equals("T1") && r1.getInicio().equals(h(16))
                    && r1.getCambiosAgenda().isEmpty(), "reprogramación fallida conserva intervalo e historia");
            no(c.getDeportivo().cancelar(c.getAcceso().autenticar("B", "ClavePrueba2026"), r1, "Ajena"));
            si(c.getDeportivo().reprogramar(a, r1, "T3", h(18), 60, "Nueva cancha"));
            si(c.getDeportivo().cancelar(a, r1, "Ya no asistiré"));
            si(s.reservar(admin, "LIBERADA", "D", "T3", h(18), h(19), 2, "Sencillos"));
            si(juego(c, coach, "PENDIENTE", "TENIS", "Sencillos", List.of("A"), List.of("B"), "T1",
                    D.plusDays(1).atTime(9, 0)));
            ok(c.getDeportivo().estadisticas(a, "TENIS", null, D, D).size() == 1,
                    "socio consulta únicamente sus estadísticas acumuladas");
            p.salvar(ruta, c);
        } else {
            EstadoClub c = p.cargar(ruta);
            var coach = c.getAcceso().autenticar("COACH", "ClavePrueba2026");
            if (args[0].equals("continuar")) {
                ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1031
                        && c.getAcademias().buscar("B", "TENIS").getNivel() == NivelAcademia.INTERMEDIO,
                        "niveles y ranking recuperados en otro proceso");
                no(c.getOperaciones().registrarResultado(coach, "DOBLES", 2, 1));
                si(c.getOperaciones().registrarResultado(coach, "PENDIENTE", 0, 2));
                ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1012
                        && c.buscarJugador("B").obtenerRankingPorDisciplina("TENIS") == 988,
                        "segundo resultado cronológico calcula ELO desde ranking recuperado");
                p.salvar(ruta, c);
            } else {
                ok(c.buscarJugador("A").obtenerRankingPorDisciplina("TENIS") == 1012
                        && c.buscarJugador("A").getMovimientosRanking().stream()
                                .filter(m -> m.referencia().equals("ELO:PENDIENTE")).count() == 1,
                        "segunda recarga no vuelve a aplicar ELO");
                var r = c.getReservas().buscarReserva("R1");
                ok(r.estaCancelada() && r.getCambiosAgenda().size() == 2
                        && c.getReservas().buscarReserva("LIBERADA") != null,
                        "cancelación y reprogramación conservan historia sin bloquear");
                ok(c.getEntrenamientos().buscarEntrenamiento("GRUPO").getRegistroAsistencia().get(0)
                        .getCalificacion() == null && c.getAgenda().getMaximo("TENIS") == 2,
                        "nota opcional y límite recuperados");
                JSONObject roto = new JSONObject(Files.readString(ruta));
                JSONArray juegos = roto.getJSONArray("partidos");
                for (int i = 0; i < juegos.length(); i++) {
                    if (juegos.getJSONObject(i).getString("id").equals("M1")) {
                        juegos.getJSONObject(i).getJSONObject("elo").getJSONObject("A").put("nuevo", 9999);
                    }
                }
                Path invalido = ruta.resolveSibling("elo-invalido.json");
                Files.writeString(invalido, roto.toString());
                boolean rechazo = false;
                try {
                    p.cargar(invalido);
                } catch (java.io.IOException e) {
                    rechazo = true;
                }
                ok(rechazo, "archivo ELO inconsistente rechazado");
            }
        }
    }
}
