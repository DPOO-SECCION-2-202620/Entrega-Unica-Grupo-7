package pruebas;

import aplicacion.*;
import gestores.GestorAcceso.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.partidos.*;
import modelo.reservas.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaCierreTorneos {

    static final Clock RELOJ = PruebaCierreBloqueC.RELOJ;
    static final LocalDate D = LocalDate.of(2026, 11, 10);

    static void ok(boolean c, String m) {
        PruebaCierreBloqueC.ok(c, m);
    }

    static void si(ResultadoOperacion r) {
        PruebaCierreBloqueC.si(r);
    }

    static void no(ResultadoOperacion r) {
        PruebaCierreBloqueC.no(r);
    }

    static Competencia competencia(EstadoClub c, Sesion admin, String id, String dep, FormatoLiga f) {
        Competencia co = new Competencia(id, id, "Club", 2026, c.buscarDeporte(dep), "Mayores", f);
        si(c.getOperaciones().registrarCompetencia(admin, co));
        return co;
    }

    static FormatoLiga f(EstadoClub c, String id) {
        return (FormatoLiga) c.getCompetencias().buscarCompetencia(id).getFormato();
    }

    static List<String> nomina(int desde, int hasta) {
        return java.util.stream.IntStream.range(desde, hasta).mapToObj(i -> "J" + i).toList();
    }

    static void oficial(EstadoClub c, Sesion admin, String id, Competencia co, LocalDateTime hora, int goles) {
        PartidoCompetencia p = new PartidoCompetencia(id, hora, c.buscarDeporte("FUTBOL"), co,
                c.getEquipos().buscarEquipo("CLUB"), "Homónimo", "Visitante");
        p.vincularEncuentro(id, "X", "Homónimo", null);
        si(c.getOperaciones().registrarPartido(admin, p));
        si(c.getOperaciones().alinear(admin, id, "CLUB", nomina(0, 7)));
        si(c.getOperaciones().registrarResultado(admin, id, goles, 0));
        si(c.getOperaciones().registrarMetrica(admin, id, "J0", TipoEstadistica.GOLES, goles));
        si(c.getOperaciones().registrarMetrica(admin, id, "J1", TipoEstadistica.GOLES, goles));
        si(c.getOperaciones().cerrarActa(admin, id));
        si(c.getOperaciones().procesarTablaLiga(admin, id));
        no(c.getOperaciones().procesarTablaLiga(admin, id));
    }

    public static void main(String[] args) throws Exception {
        PersistenciaClubJson p = new PersistenciaClubJson(RELOJ);
        Path ruta = Path.of(args[1]);
        if (args[0].equals("crear")) {
            EstadoClub c = new EstadoClub(RELOJ);
            var admin = SoporteComercial.administrador(c);
            var d = c.getDeportivo();
            for (String dep : List.of("FUTBOL", "BALONCESTO", "VOLEIBOL")) {
                competencia(c, admin, dep, dep, new FormatoLiga());
                for (String id : List.of("A", "B", "C")) {
                    si(d.registrarExterno(admin, dep, id, "Homónimo"));
                }
                si(d.registrarEncuentro(admin, dep, "1", "A", "B"));
                si(d.registrarEncuentro(admin, dep, "2", "B", "C"));
                if (dep.equals("FUTBOL")) {
                    si(d.resultadoExterno(admin, dep, "1", 2, 0, null));
                    si(d.resultadoExterno(admin, dep, "2", 1, 1, null));
                    ok(f(c, dep).obtenerPuntos("A") == 3 && f(c, dep).obtenerPuntos("B") == 1
                            && f(c, dep).obtenerPuntos("C") == 1, "fútbol puntúa ambos lados 3/1/0");
                } else if (dep.equals("BALONCESTO")) {
                    no(d.resultadoExterno(admin, dep, "1", 20, 20, null));
                    si(d.resultadoExterno(admin, dep, "1", 20, 10, null));
                    si(d.resultadoExterno(admin, dep, "2", 8, 10, null));
                    ok(f(c, dep).obtenerPuntos("A") == 2 && f(c, dep).obtenerPuntos("B") == 2
                            && f(c, dep).obtenerPuntos("C") == 2, "baloncesto victoria2 derrota1");
                } else {
                    no(d.resultadoExterno(admin, dep, "1", 3, 3, null));
                    no(d.resultadoExterno(admin, dep, "1", 2, 0, null));
                    si(d.resultadoExterno(admin, dep, "1", 3, 0, null));
                    si(d.resultadoExterno(admin, dep, "2", 3, 2, null));
                    ok(f(c, dep).obtenerPuntos("A") == 3 && f(c, dep).obtenerPuntos("B") == 2
                            && f(c, dep).obtenerPuntos("C") == 1, "voleibol 3/0 directo y 2/1 decisivo");
                }
                no(d.resultadoExterno(admin, dep, "1", 3, 0, null));
                ok(f(c, dep).getTabla().size() == 3, "homónimos externos tienen IDs diferentes");
            }
            competencia(c, admin, "COPA", "FUTBOL", new FormatoFases());
            for (String id : List.of("A", "B", "C", "D")) {
                si(d.registrarExterno(admin, "COPA", id, id));
            }
            Map<String, List<String>> grupos = new LinkedHashMap<>();
            grupos.put("Norte", List.of("A", "B"));
            grupos.put("Sur", List.of("C", "D"));
            si(d.configurarGrupos(admin, "COPA", grupos, 2));
            no(d.avanzar(admin, "COPA"));
            si(d.resultadoExterno(admin, "COPA", "GRUPO-1", 0, 0, null));
            si(d.resultadoExterno(admin, "COPA", "GRUPO-2", 2, 0, null));
            si(d.avanzar(admin, "COPA"));
            no(d.avanzar(admin, "COPA"));
            var copa = (FormatoFases) f(c, "COPA");
            ok(copa.buscarEncuentro("ELIM-1-1").localId().equals("A")
                    && copa.buscarEncuentro("ELIM-1-1").visitanteId().equals("D"),
                    "desempate de grupos estable por ID y cruce primero-último");
            no(d.resultadoExterno(admin, "COPA", "ELIM-1-1", 1, 1, null));
            si(d.resultadoExterno(admin, "COPA", "ELIM-1-1", 1, 1, "A"));
            si(d.estadoParticipante(admin, "COPA", "B", false));
            si(d.victoriaAdministrativa(admin, "COPA", "ELIM-1-2", "C"));
            si(c.getOperaciones().registrarEquipo(admin,
                    new Equipo("CLUB", "Club", c.buscarDeporte("FUTBOL"), "Mayores", 12)));
            for (int i = 0; i < 8; i++) {
                String nombreJugador;
                if (i < 2) {
                    nombreJugador = "Alex";
                } else {
                    nombreJugador = "J" + i;
                }
                c.registrarJugador("J" + i, nombreJugador, LocalDate.of(2000, 1, 1));
                si(c.getOperaciones().inscribir(admin, "J" + i, "CLUB", "Jugador", i + 1));
            }
            Competencia liga = competencia(c, admin, "LIGA-CLUB", "FUTBOL", new FormatoLiga()),
                    otra = competencia(c, admin, "OTRA", "FUTBOL", new FormatoLiga());
            for (Competencia co : List.of(liga, otra)) {
                si(c.getOperaciones().registrarNomina(admin, co.getId(), "CLUB", nomina(0, 7)));
                si(d.registrarExterno(admin, co.getId(), "X", "Homónimo"));
            }
            oficial(c, admin, "P1", liga, D.atTime(9, 0), 2);
            oficial(c, admin, "P2", liga, D.plusDays(1).atTime(9, 0), 3);
            oficial(c, admin, "P3", otra, D.atTime(12, 0), 9);
            var tabla = d.clasificacion(admin, "FUTBOL", "LIGA-CLUB", TipoEstadistica.GOLES, D, D.plusDays(2));
            ok(tabla.get(0).jugadorId().equals("J0") && tabla.get(1).jugadorId().equals("J1")
                    && tabla.get(0).metricas().get(TipoEstadistica.GOLES) == 5 && tabla.get(0).partidos() == 2,
                    "dos actas suman5, otro torneo excluido y homónimos empatados conservan IDs");
            si(d.actualizarNomina(admin, "LIGA-CLUB", "CLUB", nomina(1, 8)));
            ok(c.getCompetencias().buscarNomina("LIGA-CLUB", "CLUB").getJugadores().size() == 8
                    && c.getCompetencias().buscarNomina("LIGA-CLUB", "CLUB").getJugadoresActuales().size() == 7
                    && c.getPartidos().buscarPartido("P1").getParticipantes("CLUB").contains(c.buscarJugador("J0")),
                    "actualización de nómina conserva selección e integrantes históricos");
            si(c.getOperaciones().registrarInstalacion(admin,
                    new Instalacion("F1", "Fútbol", c.buscarDeporte("FUTBOL"), 22, false)));
            ok(c.getReservas().buscarInstalacion("F1").getAgenda().estaDisponible(D.atTime(9, 0), D.atTime(11, 0)),
                    "partido externo no ocupa cancha del club");
            si(c.getOperaciones().registrarEquipo(admin,
                    new Equipo("OTROCLUB", "Otro club", c.buscarDeporte("FUTBOL"), "Mayores", 12)));
            for (int i = 8; i < 15; i++) {
                c.registrarJugador("J" + i, "J" + i, LocalDate.of(2000, 1, 1));
                si(c.getOperaciones().inscribir(admin, "J" + i, "OTROCLUB", "Jugador", i - 7));
            }
            Competencia doble = competencia(c, admin, "COPA-CLUB", "FUTBOL", new FormatoFases());
            si(c.getOperaciones().registrarNomina(admin, "COPA-CLUB", "CLUB", nomina(0, 7)));
            si(c.getOperaciones().registrarNomina(admin, "COPA-CLUB", "OTROCLUB", nomina(8, 15)));
            si(d.configurarGrupos(admin, "COPA-CLUB", Map.of("Único", List.of("OTROCLUB", "CLUB")), 2));
            for (int ronda = 0; ronda < 2; ronda++) {
                String id = "DUAL-" + ronda;
                String encuentro;
                if (ronda == 0) {
                    encuentro = "GRUPO-1";
                } else {
                    encuentro = "ELIM-1-1";
                }
                PartidoCompetencia pc = new PartidoCompetencia(id, D.plusDays(5 + ronda).atTime(9, 0),
                        c.buscarDeporte("FUTBOL"), doble, c.getEquipos().buscarEquipo("CLUB"), "Otro club", "Sede");
                ok(pc.vincularEncuentro(encuentro, "OTROCLUB", "Otro club", c.getEquipos().buscarEquipo("OTROCLUB"))
                        && pc.configurarOcupacion(c.getReservas().buscarInstalacion("F1"), 120),
                        "vínculo canónico y ocupación de partido oficial");
                si(c.getOperaciones().registrarPartido(admin, pc));
                si(c.getOperaciones().alinear(admin, id, "CLUB", nomina(0, 7)));
                no(c.getOperaciones().registrarResultado(admin, id, 0, 0));
                si(c.getOperaciones().alinear(admin, id, "OTROCLUB", nomina(8, 15)));
                si(c.getOperaciones().registrarResultado(admin, id, 0, 0));
                if (ronda == 1) {
                    no(c.getOperaciones().cerrarActa(admin, id));
                    si(d.definirDesempate(admin, id, "OTROCLUB"));
                }
                si(c.getOperaciones().cerrarActa(admin, id));
                si(c.getOperaciones().procesarTablaLiga(admin, id));
                si(d.avanzar(admin, "COPA-CLUB"));
            }
            ok(((FormatoFases) doble.getFormato()).getCampeon().equals("OTROCLUB")
                    && c.getPartidos().buscarPartido("DUAL-0").getSelecciones().size() == 2,
                    "ambos equipos alineados y final empatada con ganador explícito");
            ok(!c.getReservas().buscarInstalacion("F1").getAgenda().estaDisponible(D.plusDays(5).atTime(9, 0),
                    D.plusDays(5).atTime(10, 0)), "partido en sede bloquea instalación física");
            p.salvar(ruta, c);
        } else {
            EstadoClub c = p.cargar(ruta);
            var admin = SoporteComercial.administrador(c);
            var d = c.getDeportivo();
            var copa = (FormatoFases) f(c, "COPA");
            if (args[0].equals("continuar")) {
                ok(copa.getRonda() == 1 && copa.getCampeon() == null
                        && copa.buscarEncuentro("ELIM-1-1").ganador().equals("A"), "semifinal y desempate recuperados");
                si(d.avanzar(admin, "COPA"));
                no(d.avanzar(admin, "COPA"));
                si(d.resultadoExterno(admin, "COPA", "ELIM-2-1", 0, 0, "C"));
                si(d.avanzar(admin, "COPA"));
                no(d.avanzar(admin, "COPA"));
                ok(copa.getCampeon().equals("C"), "grupos, semifinales, final y campeón completos");
                p.salvar(ruta, c);
            } else {
                ok(copa.getCampeon().equals("C") && copa.getRonda() == 2 && copa.getEncuentros().size() == 5,
                        "campeón y avance conservados al reiniciar");
                no(d.avanzar(admin, "COPA"));
                no(c.getOperaciones().procesarTablaLiga(admin, "P1"));
                ok(f(c, "LIGA-CLUB").obtenerPuntos("CLUB") == 6 && f(c, "LIGA-CLUB").getTabla().size() == 2,
                        "tabla completa reconstruida sin duplicar puntos");
                ok(c.getCompetencias().buscarNomina("LIGA-CLUB", "CLUB").getJugadoresActuales().stream()
                        .noneMatch(j -> j.getId().equals("J0"))
                        && c.getPartidos().buscarPartido("P1").esParticipante(c.buscarJugador("J0")),
                        "miembros actuales y selección histórica distintos tras cargar");
                JSONObject roto = new JSONObject(Files.readString(ruta));
                JSONArray competencias = roto.getJSONArray("competencias");
                for (int i = 0; i < competencias.length(); i++) {
                    if (competencias.getJSONObject(i).getString("id").equals("COPA")) {
                        competencias.getJSONObject(i).getJSONObject("torneo").put("campeon", "A");
                    }
                }
                Path invalido = ruta.resolveSibling("campeon-invalido.json");
                Files.writeString(invalido, roto.toString());
                boolean rechazo = false;
                try {
                    p.cargar(invalido);
                } catch (java.io.IOException e) {
                    rechazo = true;
                }
                ok(rechazo, "campeón inventado en JSON rechazado");
            }
        }
    }
}
