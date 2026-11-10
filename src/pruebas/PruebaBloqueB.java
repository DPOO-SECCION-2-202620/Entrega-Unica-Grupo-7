package pruebas;

import aplicacion.*;
import gestores.*;
import gestores.GestorAcceso.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.partidos.*;
import modelo.usuarios.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaBloqueB {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-06T15:30:00Z"), ZoneId.of("America/Bogota"));
    private static EstadoClub c;
    private static ServicioClub s;
    private static Sesion admin, coach;

    private static void ok(boolean valor, String mensaje) {
        if (!valor) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static void exito(ResultadoOperacion r) {
        ok(r.exito(), r.mensaje());
    }

    private static void no(ResultadoOperacion r, String texto) {
        ok(!r.exito() && r.mensaje().toLowerCase(Locale.ROOT).contains(texto.toLowerCase(Locale.ROOT)),
                "rechazo diferenciado: " + texto);
    }

    private static List<String> ids(String prefijo, int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad).mapToObj(i -> prefijo + i).toList();
    }

    private static List<String> sin(String prefijo, int cantidad, String excluido) {
        return ids(prefijo, cantidad).stream().filter(id -> !id.equals(excluido)).toList();
    }

    private static void jugador(String id) {
        if (c.buscarJugador(id) == null) {
            String nombreJugador;
            if (id.equals("A0") || id.equals("A1")) {
                nombreJugador = "Alex";
            } else {
                nombreJugador = id;
            }
            c.registrarJugador(id, nombreJugador, LocalDate.of(2000, 1, 1));
        }
    }

    private static Equipo equipo(String id, String deporte, String prefijo, int cantidad) {
        Equipo e = new Equipo(id, id, c.buscarDeporte(deporte), "Mayores", 12);
        exito(s.registrarEquipo(admin, e));
        for (int i = 0; i < cantidad; i++) {
            jugador(prefijo + i);
            exito(s.inscribir(coach, prefijo + i, id, "Jugador", i + 1));
        }
        return e;
    }

    private static Competencia torneo(String id, String deporte) {
        Competencia t = new Competencia(id, id, "Club", 2026, c.buscarDeporte(deporte), "Mayores", new FormatoLiga());
        exito(s.registrarCompetencia(admin, t));
        return t;
    }

    private static PartidoCompetencia oficial(String id, Competencia torneo, Equipo e, int dia) {
        PartidoCompetencia p = new PartidoCompetencia(id, LocalDateTime.of(2026, 11, dia, 12, 0), torneo.getDeporte(),
                torneo, e, "Rival externo", "Escenario");
        exito(s.registrarPartido(coach, p));
        return p;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: crear|verificar archivo.json");
        }
        Path archivo = Path.of(args[1]);
        PersistenciaClubJson persistencia = new PersistenciaClubJson(RELOJ);
        if (args[0].equals("crear")) {
            c = new EstadoClub(RELOJ);
            s = c.getOperaciones();
            admin = SoporteComercial.administrador(c);
            c.registrarEmpleado("COACH", "Entrenador");
            coach = SoporteComercial.habilitar(c, admin, "COACH", Perfil.ENTRENADOR);
            c.registrarCuentaBase("BASE", "Cuenta base", "base", "ClavePrueba2026");
            Sesion base = c.getAcceso().autenticar("base", "ClavePrueba2026");
            Equipo a = equipo("EA", "FUTBOL", "A", 8), b = equipo("EB", "FUTBOL", "B", 8),
                    transfer = equipo("EC", "FUTBOL", "C", 7);
            jugador("NUEVO");
            no(s.inscribir(coach, "NUEVO", "EA", "Jugador", 1), "Dorsal ocupado");
            no(s.inscribir(coach, "A0", "EB", "Jugador", 9), "mismo deporte");
            int total = a.getCantidadInscritos();
            ok(!a.agregarInscripcion(new InscripcionEquipo(c.buscarJugador("NUEVO"), a, "", 10))
                    && !c.buscarJugador("NUEVO")
                            .agregarInscripcionEquipo(new InscripcionEquipo(c.buscarJugador("NUEVO"), a, "Jugador", 10))
                    && a.getCantidadInscritos() == total,
                    "entidades rechazan inscripción unilateral o datos inválidos");
            c.registrarJugador("EDAD", "Edad frontera", LocalDate.of(2013, 11, 6));
            Equipo sub13 = new Equipo("SUB13", "Sub13", c.buscarDeporte("FUTBOL"), "Sub-13", 1),
                    sub15 = new Equipo("SUB15", "Sub15", c.buscarDeporte("FUTBOL"), "Sub-15", 1);
            exito(s.registrarEquipo(admin, sub13));
            exito(s.registrarEquipo(admin, sub15));
            no(s.inscribir(coach, "EDAD", "SUB13", "Jugador", 1), "Edad incompatible");
            exito(s.inscribir(coach, "EDAD", "SUB15", "Jugador", 1));
            c.registrarJugador("EDAD2", "Otro", LocalDate.of(2012, 1, 1));
            no(s.inscribir(coach, "EDAD2", "SUB15", "Jugador", 2), "cupos");
            Competencia t1 = torneo("T1", "FUTBOL"), t2 = torneo("T2", "FUTBOL");
            no(s.registrarNomina(coach, "T1", "EC", ids("C", 6)), "insuficiente");
            ok(c.getCompetencias().buscarNomina("T1", "EC") == null, "nómina insuficiente no deja registro parcial");
            exito(s.registrarNomina(coach, "T1", "EA", ids("A", 8)));
            exito(s.registrarNomina(coach, "T2", "EA", ids("A", 8)));
            PartidoCompetencia p1 = oficial("P1", t1, a, 1);
            no(s.registrarResultado(coach, "P1", 2, 0), "alineación");
            exito(s.alinear(coach, "P1", "EA", ids("A", 7)));
            var anterior = p1.getParticipantes("EA");
            List<String> ajena = new ArrayList<>(ids("A", 6));
            ajena.add("B0");
            no(s.alinear(coach, "P1", "EA", ajena), "fuera de la nómina");
            ok(anterior.equals(p1.getParticipantes("EA")), "alineación fallida conserva selección anterior");
            no(s.alinear(base, "P1", "EA", ids("A", 7)), "permiso");
            exito(s.registrarResultado(coach, "P1", 2, 0));
            exito(s.registrarMetrica(coach, "P1", "A0", TipoEstadistica.GOLES, 2));
            no(s.registrarMetrica(coach, "P1", "A7", TipoEstadistica.GOLES, 1), "ajeno");
            ok(!p1.registrarEstadistica(new EstadisticaPartido(c.buscarJugador("B0"), p1)),
                    "modelo rechaza estadística ajena");
            exito(s.registrarTarjeta(coach, "P1", "A0", TipoTarjeta.AMARILLA));
            exito(s.registrarTarjeta(coach, "P1", "A1", TipoTarjeta.ROJA));
            no(s.registrarTarjeta(coach, "P1", "A1", TipoTarjeta.ROJA), "incompatibles");
            no(s.retirarJugador(coach, "A0", "EA", "Salida"), "actas pendientes");
            no(s.cambiarEstadoEquipo(admin, "EA", false), "acta pendiente");
            ok(c.getHabilitacion().getSuspensiones().isEmpty(), "tarjetas sin cierre no generan suspensión prematura");
            no(s.cerrarActa(base, "P1"), "permiso");
            exito(s.cerrarActa(coach, "P1"));
            no(s.cerrarActa(coach, "P1"), "ya cerrada");
            ok(!p1.getEstadisticas().get(0).registrarMetrica(TipoEstadistica.ASISTENCIAS, 1),
                    "métricas cerradas no se modifican por la entidad");
            PartidoCompetencia cancelado = oficial("CANCELADO", t1, a, 2);
            exito(s.cancelarPartido(coach, "CANCELADO"));
            ok(!c.getHabilitacion().consultar(coach, "CANCELADO", "EA").puedeJugar(),
                    "partido cancelado no se considera disputable");
            no(s.cerrarActa(coach, "CANCELADO"), "cancelada");
            PartidoCompetencia otro = oficial("OTRA", t2, a, 2);
            exito(s.alinear(coach, "OTRA", "EA", ids("A", 7)));
            exito(s.registrarResultado(coach, "OTRA", 1, 1));
            exito(s.cerrarActa(coach, "OTRA"));
            PartidoInterno interno = new PartidoInterno("I1", LocalDateTime.of(2026, 11, 2, 16, 0),
                    c.buscarDeporte("FUTBOL"), a, b);
            exito(s.registrarPartido(coach, interno));
            exito(s.registrarResultado(coach, "I1", 0, 0));
            exito(s.registrarTarjeta(coach, "I1", "A3", TipoTarjeta.ROJA));
            exito(s.cerrarActa(coach, "I1"));
            ok(c.getHabilitacion().getSuspensiones().stream().filter(sp -> sp.jugadorId().equals("A1"))
                    .allMatch(sp -> sp.partidoCumplimientoId() == null),
                    "otra competencia, internos y cancelación no consumen sanción T1");
            PartidoCompetencia p2 = oficial("P2", t1, a, 3);
            var d = c.getHabilitacion().consultar(coach, "P2", "EA");
            ok(d.cantidad() == 7 && d.puedeJugar()
                    && d.excluidos().stream()
                            .anyMatch(e -> e.jugadorId().equals("A1") && e.motivo().contains("Suspensión")),
                    "siete habilitados pueden jugar y suspensión explica exclusión");
            c.getHabilitacion().consultar(coach, "P2", "EA");
            ok(c.getHabilitacion().getSuspensiones().stream().filter(sp -> sp.jugadorId().equals("A1"))
                    .allMatch(sp -> sp.partidoCumplimientoId() == null), "consultar dos veces no consume suspensión");
            exito(s.alinear(coach, "P2", "EA", sin("A", 8, "A1")));
            anterior = p2.getParticipantes("EA");
            no(s.alinear(coach, "P2", "EA", ids("A", 7)), "Suspensión vigente");
            ok(anterior.equals(p2.getParticipantes("EA")), "rechazo de suspendido atómico");
            p2.definirSeleccion("EA", ids("A", 7).stream().map(c::buscarJugador).toList());
            ok(!p2.registrarResultado(2, 0, c.getCompetencias().buscarNomina("T1", "EA")),
                    "resultado por modelo registrado también comprueba habilitación");
            exito(s.alinear(coach, "P2", "EA", sin("A", 8, "A1")));
            exito(s.registrarResultado(coach, "P2", 2, 0));
            exito(s.registrarTarjeta(coach, "P2", "A0", TipoTarjeta.AMARILLA));
            exito(s.cerrarActa(coach, "P2"));
            ok(c.getHabilitacion().getSuspensiones().stream()
                    .anyMatch(sp -> sp.jugadorId().equals("A1") && "P2".equals(sp.partidoCumplimientoId())),
                    "roja se cumple en siguiente partido aplicable");
            PartidoCompetencia p3 = oficial("P3", t1, a, 4);
            no(s.alinear(coach, "P3", "EA", ids("A", 7)), "Suspensión vigente");
            exito(s.alinear(coach, "P3", "EA", sin("A", 8, "A0")));
            exito(s.registrarResultado(coach, "P3", 1, 0));
            exito(s.registrarTarjeta(coach, "P3", "A2", TipoTarjeta.ROJA));
            exito(s.registrarTarjeta(coach, "P3", "A4", TipoTarjeta.AMARILLA));
            exito(s.cerrarActa(coach, "P3"));
            PartidoInterno interno2 = new PartidoInterno("I2", LocalDateTime.of(2026, 11, 4, 16, 0),
                    c.buscarDeporte("FUTBOL"), a, b);
            exito(s.registrarPartido(coach, interno2));
            exito(s.retirarJugador(coach, "A7", "EA", "Retiro temporal"));
            ok(a.getCantidadInscritos() == 7 && c.getHabilitacion().consultar(coach, "I2", "EA").cantidad() == 6
                    && !interno2.puedeDisputarse(),
                    "siete inscritos con un suspendido son seis habilitados, también en el modelo interno");
            no(s.registrarResultado(coach, "I2", 1, 0), "mínimo 7");
            exito(s.inscribir(coach, "A7", "EA", "Jugador", 8));
            exito(s.registrarResultado(coach, "I2", 1, 0));
            exito(s.cerrarActa(coach, "I2"));
            PartidoCompetencia p4 = oficial("P4", t1, a, 5);
            ok(c.getHabilitacion().motivo(p4, a, c.buscarJugador("A0")) == null,
                    "dos amarillas suspenden un partido y después recupera habilitación");
            exito(s.retirarJugador(coach, "A2", "EA", "Transferencia"));
            ok(!a.contieneJugador(c.buscarJugador("A2")) && !c.buscarJugador("A2").contieneInscripcionEquipo(a)
                    && a.estaDisponibleNumero(3) && a.getCantidadInscritos() == 7,
                    "retiro actualiza ambos extremos y libera cupo/dorsal");
            exito(s.inscribir(coach, "NUEVO", "EA", "Jugador", 3));
            exito(s.inscribir(coach, "A2", "EC", "Jugador", 8));
            List<String> nominaTransfer = new ArrayList<>(ids("C", 7));
            nominaTransfer.add("A2");
            exito(s.registrarNomina(coach, "T1", "EC", nominaTransfer));
            PartidoCompetencia transferencia = oficial("TRANSFERENCIA", t1, transfer, 6);
            ok(c.getHabilitacion().motivo(transferencia, transfer, c.buscarJugador("A2")).contains("Suspensión"),
                    "cambiar de equipo no evade sanción de la misma competencia");
            ok(p1.getParticipantes("EA").contains(c.buscarJugador("A2"))
                    && p1.getEstadisticas().get(0).getJugador() == c.buscarJugador("A0"),
                    "retiro no modifica participantes o estadísticas del pasado");
            Equipo basket = equipo("BASK1", "BALONCESTO", "BK", 5), basket2 = equipo("BASK2", "BALONCESTO", "BQ", 5);
            exito(s.inscribir(coach, "A2", "BASK1", "Jugador", 6)); // Otro deporte sigue permitido.
            exito(s.cambiarEstadoUsuario(admin, "BK0", false));
            exito(s.cambiarEstadoUsuario(admin, "BK1", false));
            PartidoInterno baloncesto = new PartidoInterno("BASK", LocalDateTime.of(2026, 11, 5, 12, 0),
                    c.buscarDeporte("BALONCESTO"), basket, basket2);
            exito(s.registrarPartido(coach, baloncesto));
            d = c.getHabilitacion().consultar(coach, "BASK", "BASK1");
            ok(d.cantidad() == 4 && d.minimo() == 5 && !d.puedeJugar() && d.excluidos().size() == 2,
                    "baloncesto con cuatro habilitados no juega y lista bajas");
            no(s.registrarResultado(coach, "BASK", 10, 8), "mínimo 5");
            exito(s.cambiarEstadoEquipo(admin, "BASK2", false));
            ok(baloncesto.estaCancelado() && basket2.getInscripciones().isEmpty()
                    && basket2.getHistorialInscripciones().size() == 5,
                    "baja de equipo cancela pendientes y conserva historial");
            Equipo v1 = equipo("V1", "VOLEIBOL", "VA", 6);
            Competencia vt = torneo("VT", "VOLEIBOL");
            vt.configurarSetsParaGanar(2);
            exito(s.registrarNomina(coach, "VT", "V1", ids("VA", 6)));
            PartidoCompetencia voley = oficial("VOLEY", vt, v1, 5);
            exito(s.alinear(coach, "VOLEY", "V1", ids("VA", 6)));
            no(s.registrarResultado(coach, "VOLEY", 1, 1), "Resultado inválido");
            no(s.registrarResultado(coach, "VOLEY", 3, 1), "Resultado inválido");
            exito(s.registrarResultado(coach, "VOLEY", 2, 1));
            exito(s.cerrarActa(coach, "VOLEY"));
            Deporte nombreNuevo = new Deporte("VOLEIBOL", new String("Voleibol"), true);
            PartidoCompetencia comparacion = new PartidoCompetencia("CADENA", LocalDateTime.of(2026, 11, 5, 12, 0),
                    nombreNuevo, vt, v1, "Rival", "Escenario");
            ok(comparacion.cumpleRequisitosCompetencia(c.getCompetencias().buscarNomina("VT", "V1"))
                    && !comparacion.registrarResultado(1, 1, c.getCompetencias().buscarNomina("VT", "V1"))
                    && comparacion.registrarResultado(2, 1, c.getCompetencias().buscarNomina("VT", "V1")),
                    "new String Voleibol rechaza empate y permite resultado válido");
            ok(c.getHabilitacion().getSuspensiones().size() == 4 && c.getHabilitacion().getAmarillas().size() == 1,
                    "historial contiene cumplidas, pendiente y contador residual");
            persistencia.salvar(archivo, c);
        } else if (args[0].equals("verificar")) {
            c = persistencia.cargar(archivo);
            s = c.getOperaciones();
            admin = SoporteComercial.administrador(c);
            coach = c.getAcceso().autenticar("COACH", "ClavePrueba2026");
            ok(c.getHabilitacion().getSuspensiones().size() == 4 && c.getHabilitacion().getSuspensiones().stream()
                    .filter(sp -> sp.partidoCumplimientoId() == null).count() == 1,
                    "sanciones cumplidas y vigente recuperadas sin duplicar");
            ok(c.getHabilitacion().getAmarillas().get(new GestorHabilitacion.Clave("COMP:T1", "A4")) == 1,
                    "amarilla residual persistente");
            Partido transferencia = c.getPartidos().buscarPartido("TRANSFERENCIA");
            ok(c.getHabilitacion().motivo(transferencia, c.getEquipos().buscarEquipo("EC"), c.buscarJugador("A2"))
                    .contains("Suspensión"), "sanción sigue vigente tras retiro y recarga");
            ok(c.getPartidos().buscarPartido("P1").getParticipantes("EA").contains(c.buscarJugador("A2"))
                    && !c.getEquipos().buscarEquipo("EA").contieneJugador(c.buscarJugador("A2"))
                    && c.getEquipos().buscarEquipo("EC").contieneJugador(c.buscarJugador("A2")),
                    "historia de alineación y afiliación actual distintas, misma identidad");
            ok(c.getEquipos().buscarEquipo("EA").getHistorialInscripciones().stream()
                    .anyMatch(i -> i.getJugador() == c.buscarJugador("A2") && !i.estaActiva())
                    && c.getPartidos().buscarPartido("BASK").estaCancelado()
                    && !c.getEquipos().buscarEquipo("BASK2").estaActivo(), "retiros, baja y cancelación persistentes");
            ok(c.getPartidos().buscarPartido("VOLEY").getSetsParaGanar() == 2
                    && ((PartidoCompetencia) c.getPartidos().buscarPartido("VOLEY")).getMarcadorClub() == 2,
                    "marcador por sets y configuración persistentes");
            no(s.cerrarActa(coach, "P1"), "ya cerrada");
            persistencia.salvar(archivo, c);
            c = persistencia.cargar(archivo);
            s = c.getOperaciones();
            ok(c.getHabilitacion().getSuspensiones().size() == 4 && c.getHabilitacion().getAmarillas().size() == 1,
                    "segunda recarga no repite efectos disciplinarios");
            JSONObject original = new JSONObject(Files.readString(archivo)), roto = new JSONObject(original.toString());
            roto.getJSONArray("suspensiones").getJSONObject(0).put("partidoCumplimientoId", "CANCELADO");
            rechazarArchivo(persistencia, archivo, roto, "cumplimiento falso en partido cancelado");
            roto = new JSONObject(original.toString());
            roto.getJSONArray("amarillasAcumuladas").getJSONObject(0).put("cantidad", 2);
            rechazarArchivo(persistencia, archivo, roto, "contador disciplinario alterado");
            roto = new JSONObject(original.toString());
            roto.getJSONArray("partidos").getJSONObject(0).getJSONObject("selecciones").getJSONArray("EA").put(0, "B0");
            rechazarArchivo(persistencia, archivo, roto, "alineación ajena a plantilla");
        } else {
            throw new IllegalArgumentException("Modo desconocido");
        }
    }

    private static void rechazarArchivo(PersistenciaClubJson p, Path original, JSONObject json, String caso)
            throws Exception {
        Path ruta = original.resolveSibling("bloque-b-invalido.json");
        String texto = json.toString();
        Files.writeString(ruta, texto);
        boolean rechazado = false;
        try {
            p.cargar(ruta);
        } catch (java.io.IOException e) {
            rechazado = true;
        }
        ok(rechazado && Files.readString(ruta).equals(texto), caso + " rechazado sin sobrescribir");
    }
}
