package pruebas;

import aplicacion.*;
import consola.MenuPorRol;
import gestores.*;
import gestores.GestorAcceso.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.administracion.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.entrenamientos.*;
import modelo.partidos.*;
import modelo.reservas.*;
import modelo.socios.PagoMensualidad;
import modelo.usuarios.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaBloqueA {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-06T15:30:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 6);

    private static void ok(boolean c, String mensaje) {
        if (!c) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static void exito(ResultadoOperacion r) {
        ok(r.exito(), r.mensaje());
    }

    private static void rechazo(ResultadoOperacion r) {
        ok(!r.exito(), "rechazo: " + r.mensaje());
    }

    private static Turno turno(String id, String empleado, String inicio, String fin) {
        return new Turno(id, empleado, DayOfWeek.MONDAY, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: crear|verificar archivo.json");
        }
        PersistenciaClubJson p = new PersistenciaClubJson(RELOJ);
        Path archivo = Path.of(args[1]);
        if (args[0].equals("crear")) {
            EstadoClub c = new EstadoClub(RELOJ);
            ServicioClub s = c.getOperaciones();
            Sesion admin = SoporteComercial.administrador(c);
            rechazo(s.cambiarEstadoUsuario(admin, "ADMIN-TEST", false));
            ok(c.getDeportes().size() == 6 && c.buscarDeporte(new String("Fútbol")) == c.buscarDeporte("FUTBOL"),
                    "seis deportes y claves estables");
            for (int i = 0; i < 14; i++) {
                String nombreJugador;
                if (i < 2) {
                    nombreJugador = "Alex";
                } else {
                    nombreJugador = "Jugador " + i;
                }
                exito(s.registrarJugador(admin, "J" + i, nombreJugador, LocalDate.of(2000, 1, 1)));
            }
            c.buscarJugador("J0").actualizarCorreo("alex@uni.edu");
            ok(c.habilitarCuenta(admin, "J0", "socio", "ClavePrueba2026", Set.of(Perfil.SOCIO)),
                    "login de socio propio");
            ok(!c.habilitarCuenta(admin, "J1", " SOCIO ", "ClavePrueba2026", Set.of(Perfil.SOCIO)),
                    "login duplicado normalizado rechazado");
            Sesion socio = c.getAcceso().autenticar(" SoCiO ", "ClavePrueba2026");
            ok(socio != null, "login normalizado autentica");
            ok(c.getAcceso().autenticar("socio", "mala") == null
                    && c.getAcceso().autenticar("inexistente", "ClavePrueba2026") == null,
                    "credenciales inválidas rechazadas");
            exito(s.registrarJugador(admin, "C1", "Entrenador jugador", LocalDate.of(1990, 1, 1)));
            for (String id : List.of("C1", "C2", "F1", "E1")) {
                String nombreEmpleado;
                if (id.equals("C1")) {
                    nombreEmpleado = "Entrenador jugador";
                } else {
                    nombreEmpleado = id;
                }
                ok(c.altaEmpleado(admin, id, nombreEmpleado), "empleado " + id);
            }
            ok(c.habilitarCuenta(admin, "C1", "coach", "ClavePrueba2026", Set.of(Perfil.ENTRENADOR, Perfil.SOCIO)),
                    "entrenador y jugador en una cuenta");
            Sesion coach = c.getAcceso().autenticar("coach", "ClavePrueba2026"),
                    coach2 = SoporteComercial.habilitar(c, admin, "C2", Perfil.ENTRENADOR),
                    fisio = SoporteComercial.habilitar(c, admin, "F1", Perfil.FISIOTERAPEUTA),
                    caja = SoporteComercial.habilitar(c, admin, "E1", Perfil.EMPLEADO);
            ok(c.buscarJugador("C1").getUsuario() == c.getVentas().buscarEmpleadoPorCodigo("EMP-C1").getUsuario(),
                    "identidad única jugador-entrenador");
            ok(c.getSocios().buscarJugadorPorId("J0") == c.buscarJugador("J0"),
                    "gestor de socios comparte el registro central");
            ok(c.buscarJugador("J0").getEdad(LocalDate.of(2020, 1, 1)) == 20
                    && c.buscarJugador("J0").getEdad(LocalDate.of(2019, 12, 31)) == 19, "edad derivada en fronteras");
            ok(MenuPorRol.opciones(c, null).keySet().equals(new LinkedHashSet<>(List.of("13", "18", "0"))),
                    "sin sesión acceso, creación autorizada de perfiles y salida");
            var ms = MenuPorRol.opciones(c, socio);
            ok(ms.containsKey("5") && !ms.containsKey("1") && !ms.containsKey("11") && !ms.containsKey("40"),
                    "menú socio restringido");
            var mt = MenuPorRol.opciones(c, coach);
            ok(mt.containsKey("34") && mt.containsKey("5") && mt.containsKey("43") && !mt.containsKey("11")
                    && !mt.containsKey("45"), "menú combinado entrenador-socio");
            var mf = MenuPorRol.opciones(c, fisio);
            ok(mf.containsKey("43") && !mf.containsKey("34") && !mf.containsKey("11") && !mf.containsKey("5"),
                    "menú fisioterapeuta");
            ok(MenuPorRol.opciones(c, caja).containsKey("11") && !MenuPorRol.opciones(c, caja).containsKey("31"),
                    "menú caja sin gestión deportiva administrativa");
            rechazo(s.registrarJugador(socio, "NO", "No", LocalDate.of(2000, 1, 1)));
            exito(s.registrarInstalacion(admin, new Instalacion("TEN", "Tenis", c.buscarDeporte("TENIS"), 4, false)));
            exito(s.registrarInstalacion(admin,
                    new Instalacion("FUT", "Cancha fútbol", c.buscarDeporte("FUTBOL"), 22, false)));
            exito(s.registrarInstalacion(admin,
                    new Instalacion("FUT2", "Otra cancha", c.buscarDeporte("FUTBOL"), 22, false)));
            SoporteComercial.academiaHistorica(c, "J0", "TENIS");
            SoporteComercial.academiaHistorica(c, "C1", "TENIS");
            exito(s.reservar(socio, "R0", "J0", "TEN", FECHA.atTime(10, 0), FECHA.atTime(11, 0), 2, "Sencillos"));
            exito(s.reservar(coach, "RC", "C1", "TEN", FECHA.atTime(11, 0), FECHA.atTime(12, 0), 2, "Sencillos"));
            rechazo(s.reservar(socio, "NO", "C1", "TEN", FECHA.atTime(12, 0), FECHA.atTime(13, 0), 2, "Sencillos"));
            exito(s.registrarJugador(admin, "AD2", "Administrador jugador", LocalDate.of(1990, 1, 1)));
            ok(c.habilitarCuenta(admin, "AD2", "ClavePrueba2026", Set.of(Perfil.ADMINISTRADOR, Perfil.SOCIO)),
                    "segundo administrador de prueba");
            Sesion admin2 = c.getAcceso().autenticar("AD2", "ClavePrueba2026");
            rechazo(s.reservar(admin2, "NO", "AD2", "TEN", FECHA.atTime(12, 0), FECHA.atTime(13, 0), 2, "Sencillos"));
            for (String id : List.of("EQ1", "EQ2")) {
                exito(s.registrarEquipo(admin, new Equipo(id, id, c.buscarDeporte("FUTBOL"), "Mayores", 12)));
            }
            for (int i = 0; i < 14; i++) {
                String equipoJugador;
                if (i < 7) {
                    equipoJugador = "EQ1";
                } else {
                    equipoJugador = "EQ2";
                }
                exito(s.inscribir(coach, "J" + i, equipoJugador, "Jugador", (i % 7) + 1));
            }
            rechazo(s.inscribir(admin2, "AD2", "EQ1", "Jugador", 10));
            Jugador j = c.buscarJugador("J0");
            for (double importe : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY, }) {
                rechazo(s.registrarPago(caja, new PagoMensualidad("BAD", j, importe, FECHA, "2026-11")));
            }
            Jugador ajeno = new Jugador(LocalDate.of(2000, 1, 1), new Usuario("J0", "Otra identidad", "otro", ""));
            rechazo(s.registrarPago(caja, new PagoMensualidad("BAD", ajeno, 100, FECHA, "2026-11")));
            rechazo(s.registrarPago(caja, new PagoMensualidad("BAD", j, 100, FECHA, "2026-13")));
            rechazo(s.registrarPago(socio, new PagoMensualidad("BAD", j, 100, FECHA, "2026-11")));
            exito(s.registrarPago(caja, new PagoMensualidad("P1", j, 1000, FECHA, "2026-11")));
            exito(s.registrarPago(caja, new PagoMensualidad("P2", j, 500, FECHA, "2026-11")));
            exito(s.registrarPago(caja, new PagoMensualidad("P3", j, 250, FECHA.minusDays(7), "2026-10")));
            rechazo(s.registrarPago(caja, new PagoMensualidad("P1", j, 1000, FECHA, "2026-11")));
            ok(c.getSocios().getPagosMensualidades().size() == 3
                    && c.getSocios().consultarTotalMensualidadesPorPeriodo(FECHA.withDayOfMonth(1), FECHA) == 1500,
                    "pagos legítimos de mismo periodo y filtro temporal");
            boolean inmutable = false;
            try {
                c.getSocios().getPagosMensualidades().clear();
            } catch (UnsupportedOperationException e) {
                inmutable = true;
            }
            ok(inmutable && c.getSocios().getPagosMensualidades().size() == 3, "consulta no permite borrar historia");
            for (String id : List.of("TR1", "TR2")) {
                String empleado;
                if (id.equals("TR1")) {
                    empleado = "C1";
                } else {
                    empleado = "C2";
                }
                String equipo;
                if (id.equals("TR1")) {
                    equipo = "EQ1";
                } else {
                    equipo = "EQ2";
                }
                int horaEntrenamiento;
                if (id.equals("TR1")) {
                    horaEntrenamiento = 12;
                } else {
                    horaEntrenamiento = 14;
                }
                exito(s.registrarEntrenamiento(admin,
                        new Entrenamiento(id, FECHA.atTime(horaEntrenamiento, 0), 60,
                                c.getVentas().buscarEmpleadoPorCodigo("EMP-" + empleado),
                                c.getReservas().buscarInstalacion("FUT"), c.getEquipos().buscarEquipo(equipo))));
            }
            rechazo(s.registrarEntrenamiento(coach,
                    new Entrenamiento("CRUCE", FECHA.atTime(12, 30), 60,
                            c.getVentas().buscarEmpleadoPorCodigo("EMP-C1"), c.getReservas().buscarInstalacion("FUT2"),
                            c.getEquipos().buscarEquipo("EQ1"))));
            exito(s.convocar(coach, "TR1", "J0"));
            exito(s.convocar(coach, "TR1", "J1"));
            exito(s.convocar(coach2, "TR2", "J7"));
            rechazo(s.registrarAsistencia(coach, "TR1", "J0", EstadoAsistencia.ASISTIO, Double.NaN, ""));
            rechazo(s.registrarAsistencia(coach2, "TR1", "J0", EstadoAsistencia.ASISTIO, 4.0, ""));
            exito(s.registrarAsistencia(coach, "TR1", "J0", EstadoAsistencia.ASISTIO, 4.5, "Buen trabajo"));
            exito(s.registrarAsistencia(coach, "TR1", "J1", EstadoAsistencia.FALTO, null, ""));
            exito(s.registrarAsistencia(coach2, "TR2", "J7", EstadoAsistencia.ASISTIO, 5.0, ""));
            ok(c.getReportes().consultarDetalleAsistencia("NO", FECHA, FECHA).isEmpty()
                    && c.getReportes().consultarDetalleAsistencia("EQ1", FECHA, FECHA).size() == 2,
                    "asistencia filtra equipo real");
            exito(s.actualizarRanking(coach, "J0", "Tenis", 1100, FECHA.minusDays(5), "RK1"));
            exito(s.actualizarRanking(coach, "J0", "TENIS", 1150, FECHA.minusDays(1), "RK2"));
            exito(s.actualizarRanking(coach, "J1", "TENIS", 1200, FECHA.minusDays(2), "RK3"));
            var evolucion = c.getReportes().consultarEvolucion("TENIS", FECHA.minusDays(6), FECHA);
            ok(evolucion.size() == 3
                    && evolucion.stream().map(GestorReportes.EvolucionRanking::jugadorId).distinct().count() == 2,
                    "homónimos separados e historia cronológica");
            Competencia competencia = new Competencia("COMP", "Liga", "Club", 2026, c.buscarDeporte("FUTBOL"),
                    "Mayores", new FormatoLiga());
            exito(s.registrarCompetencia(admin, competencia));
            exito(s.registrarNomina(coach, "COMP", "EQ1",
                    java.util.stream.IntStream.range(0, 7).mapToObj(i -> "J" + i).toList()));
            exito(s.registrarPartido(coach, new PartidoCompetencia("PC", FECHA.atTime(16, 0), c.buscarDeporte("FUTBOL"),
                    competencia, c.getEquipos().buscarEquipo("EQ1"), "Rival", "Visitante")));
            exito(s.alinear(coach, "PC", "EQ1",
                    java.util.stream.IntStream.range(0, 7).mapToObj(i -> "J" + i).toList()));
            exito(s.registrarResultado(coach, "PC", 2, 1));
            rechazo(s.procesarTablaLiga(coach, "PC"));
            exito(s.registrarMetrica(coach, "PC", "J0", TipoEstadistica.GOLES, 1));
            exito(s.registrarTarjeta(coach, "PC", "J0", TipoTarjeta.AMARILLA));
            rechazo(s.registrarMetrica(coach, "PC", "J7", TipoEstadistica.GOLES, 1));
            exito(s.cerrarActa(coach, "PC"));
            exito(s.procesarTablaLiga(coach, "PC"));
            rechazo(s.procesarTablaLiga(coach, "PC"));
            exito(s.registrarPartido(coach,
                    new PartidoInterno("PI", FECHA.plusDays(1).atTime(15, 0), c.buscarDeporte("FUTBOL"),
                            c.getEquipos().buscarEquipo("EQ1"), c.getEquipos().buscarEquipo("EQ2"))));
            exito(s.registrarResultado(admin, "PI", 3, 0));
            exito(s.cerrarActa(admin, "PI"));
            modelo.ventas.Articulo producto = new modelo.ventas.Articulo("ART", "Artículo", "Deportes", 1000);
            ok(c.getVentas().registrarProducto(admin, "PRINCIPAL", producto, 3)
                    && c.getVentas().procesarVenta(caja, "V", j.getUsuario(), "TIENDA", FECHA,
                            List.of(new modelo.ventas.DetalleVenta(producto, 1)), null, 50),
                    "venta integrada para reconciliación financiera");
            var resumen = s.consultarFinanzas(admin, null, FECHA.withDayOfMonth(1), FECHA);
            ok(resumen.mensualidades() == 1500 && resumen.ventasPagadas() == 1240 && resumen.propina() == 50
                    && resumen.ingresoSinPropinas() == 2690, "ventas, impuestos, propinas y pagos se reconcilian");
            boolean denegado = false;
            try {
                s.consultarFinanzas(socio, null, FECHA, FECHA);
            } catch (IllegalArgumentException e) {
                denegado = true;
            }
            ok(denegado, "socio no accede a finanzas globales");
            exito(c.getPersonal().configurarHorario(admin,
                    new HorarioAtencion(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0))));
            for (Turno t : List.of(turno("TC1", "C1", "08:00", "18:00"), turno("TC1B", "C1", "08:00", "18:00"),
                    turno("TC2", "C2", "08:00", "18:00"), turno("TC2B", "C2", "12:01", "18:00"),
                    turno("TF", "F1", "08:00", "18:00"))) {
                exito(c.getPersonal().registrarTurno(admin, t));
            }
            ok(c.getPersonal().consultarCobertura(admin).isEmpty(), "cobertura laboral inicial completa");
            exito(c.getPersonal().solicitarCambio(coach, "CAMBIO-OK", turno("TC1", "C1", "07:00", "19:00")));
            exito(c.getPersonal().resolverCambio(admin, "CAMBIO-OK", true));
            exito(c.getPersonal().solicitarCambio(coach2, "CAMBIO-NO", turno("TC2", "C2", "08:00", "12:00")));
            var turnosAntes = c.getPersonal().getTurnos();
            var r = c.getPersonal().resolverCambio(admin, "CAMBIO-NO", true);
            rechazo(r);
            ok(r.mensaje().contains("12:00") && r.mensaje().contains("12:01")
                    && turnosAntes.equals(c.getPersonal().getTurnos()),
                    "hueco de un minuto detectado sin contar dos veces al mismo entrenador");
            rechazo(c.getPersonal().solicitarCambio(fisio, "AJENO", turno("TC2", "C2", "08:00", "18:00")));
            ok(c.getPersonal().consultarSolicitudes(coach).size() == 1, "empleado consulta únicamente sus solicitudes");
            exito(s.cambiarEstadoUsuario(admin, "J0", false));
            ok(c.getAcceso().autenticar("socio", "ClavePrueba2026") == null && !c.getAcceso().estaAutenticada(socio),
                    "baja invalida acceso y sesiones");
            rechazo(s.registrarPago(caja, new PagoMensualidad("NO", j, 100, FECHA, "2026-11")));
            ok(!c.getSeguridad().iniciarUso(admin, "R0"), "personal no puede ingresar un socio dado de baja");
            ok(c.getSocios().getPagosMensualidades().size() == 3
                    && c.getEquipos().buscarEquipo("EQ1").contieneJugador(j),
                    "baja lógica conserva pagos y referencias deportivas");
            p.salvar(archivo, c);
        } else if (args[0].equals("verificar")) {
            EstadoClub c = p.cargar(archivo);
            ServicioClub s = c.getOperaciones();
            Sesion admin = SoporteComercial.administrador(c),
                    coach = c.getAcceso().autenticar("coach", "ClavePrueba2026");
            Jugador j = c.buscarJugador("J0");
            ok(j == c.getSocios().buscarJugadorPorId("J0") && c.getReservas().buscarReserva("R0").getJugador() == j
                    && c.getSocios().buscarPago("P1").getJugador() == j
                    && c.getEquipos().buscarEquipo("EQ1").getInscripciones().get(0).getJugador() == j,
                    "identidad canónica compartida después de reiniciar");
            ok(c.buscarJugador("C1").getUsuario() == c.getVentas().buscarEmpleadoPorCodigo("EMP-C1").getUsuario()
                    && coach != null, "jugador-entrenador y login recuperados");
            ok(!j.estaActivo() && c.getAcceso().autenticar("socio", "ClavePrueba2026") == null
                    && j.getCorreo().equals("alex@uni.edu"), "baja y datos personales persistentes");
            ok(j.obtenerRankingPorDisciplina("TENIS") == 1150 && j.getMovimientosRanking().size() == 3
                    && c.getReportes().consultarEvolucion("TENIS", FECHA.minusDays(6), FECHA).size() == 3,
                    "ranking e historia sin reaplicar movimientos");
            Entrenamiento e = c.getEntrenamientos().buscarEntrenamiento("TR1");
            ok(e.getEquipo() == c.getEquipos().buscarEquipo("EQ1") && e.getConvocados().get(0) == j
                    && e.getRegistroAsistencia().get(0).getJugador() == j
                    && e.getEntrenador() == c.getVentas().buscarEmpleadoPorCodigo("EMP-C1"),
                    "relaciones de entrenamiento reconstruidas por ID");
            ok(!c.getReservas().buscarInstalacion("FUT").getAgenda().estaDisponible(FECHA.atTime(12, 0),
                    FECHA.atTime(13, 0)), "agenda de entrenamiento recuperada");
            ok(c.getReportes().consultarDetalleAsistencia("EQ1", FECHA, FECHA).size() == 2
                    && c.getReportes().consultarDetalleAsistencia("NO", FECHA, FECHA).isEmpty(),
                    "reportes de asistencia conservan filtros");
            PartidoCompetencia pc = (PartidoCompetencia) c.getPartidos().buscarPartido("PC");
            ok(c.getReportes().consultarResultados("FUTBOL", FECHA, FECHA.plusDays(1)).size() == 2
                    && c.getReportes().consultarEstadisticasJugador("J0", FECHA, FECHA).size() == 1,
                    "reportes de resultados por ID deportivo y estadísticas por jugador");
            ok(pc.getMarcadorClub() == 2 && pc.getEstadisticas().get(0).getJugador() == j
                    && pc.getSanciones().get(0).getJugador() == j, "resultados, métricas y tarjetas persistentes");
            FormatoLiga liga = (FormatoLiga) c.getCompetencias().buscarCompetencia("COMP").getFormato();
            ok(liga.obtenerPuntos("EQ1") == 3 && liga.yaProcesado("PC") && !s.procesarTablaLiga(coach, "PC").exito(),
                    "liga recuperada sin duplicar puntos");
            ok(c.getPersonal().getSolicitudes().size() == 2
                    && c.getPersonal().getSolicitudes().get(0).estado() == SolicitudCambioTurno.Estado.APROBADA
                    && c.getPersonal().getSolicitudes().get(1).estado() == SolicitudCambioTurno.Estado.RECHAZADA
                    && c.getPersonal().consultarCobertura(admin).isEmpty(),
                    "turnos y decisiones persistentes sin reaprobar");
            ok(s.consultarFinanzas(admin, null, FECHA.withDayOfMonth(1), FECHA).ingresoSinPropinas() == 2690
                    && c.getVentas().consultarPuntos("J0") == 23, "finanzas y fidelidad estables pese a baja");
            p.salvar(archivo, c);
            EstadoClub otra = p.cargar(archivo);
            ok(((FormatoLiga) otra.getCompetencias().buscarCompetencia("COMP").getFormato()).obtenerPuntos("EQ1") == 3
                    && otra.getSocios().getPagosMensualidades().size() == 3, "segunda recarga no duplica efectos");
            JSONObject original = new JSONObject(Files.readString(archivo)),
                    corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("mensualidades").getJSONObject(0).put("jugadorId", "NO_EXISTE");
            rechazarArchivo(p, archivo, corrupto, "pago huérfano");
            corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("competencias").getJSONObject(0).getJSONObject("puntos").put("EQ1", 6);
            rechazarArchivo(p, archivo, corrupto, "liga duplicada");
            corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("turnos").getJSONObject(0).put("empleadoId", "NO_EXISTE");
            rechazarArchivo(p, archivo, corrupto, "turno huérfano");
            corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("cuentas").getJSONObject(1).put("login",
                    corrupto.getJSONArray("cuentas").getJSONObject(0).getString("login"));
            rechazarArchivo(p, archivo, corrupto, "login duplicado");
        } else {
            throw new IllegalArgumentException("Modo desconocido");
        }
    }

    private static void rechazarArchivo(PersistenciaClubJson p, Path archivo, JSONObject json, String caso)
            throws Exception {
        Path ruta = archivo.resolveSibling("bloque-a-invalido.json");
        String texto = json.toString();
        Files.writeString(ruta, texto);
        boolean rechazado = false;
        try {
            p.cargar(ruta);
        } catch (java.io.IOException e) {
            rechazado = true;
        }
        ok(rechazado && texto.equals(Files.readString(ruta)), caso + " rechazado sin sobrescribir");
    }
}
