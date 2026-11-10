package pruebas;

import aplicacion.*;
import gestores.GestorAcceso.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.reservas.Instalacion;
import modelo.socios.PagoMensualidad;
import persistencia.PersistenciaClubJson;

public class PruebaConsolaRoles {

    private static void ok(boolean c, String mensaje) {
        if (!c) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static String ejecutar(Path ruta, Path log, String... entradas) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process proceso = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp",
                System.getProperty("java.class.path"), "consola.ConsolaClub", ruta.toString()).redirectErrorStream(true)
                .start();
        try (var salida = proceso.getOutputStream()) {
            salida.write((String.join("\n", entradas) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        String texto = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(log, texto);
        ok(proceso.waitFor() == 0, "consola termina correctamente: " + log.getFileName());
        return texto;
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory(Path.of(".build"), "roles-");
        Path ruta = carpeta.resolve("club.json");
        EstadoClub c = new EstadoClub();
        var admin = SoporteComercial.administrador(c);
        for (String id : List.of("J1", "J2", "C1")) {
            c.registrarJugador(id, id, LocalDate.of(2000, 1, 1));
        }
        for (String id : List.of("C1", "F1", "E1")) {
            c.registrarEmpleado(id, id);
        }
        ok(c.habilitarCuenta(admin, "J1", "socio1", "ClaveRoles2026", Set.of(Perfil.SOCIO)), "cuenta socio1");
        ok(c.habilitarCuenta(admin, "J2", "socio2", "ClaveRoles2026", Set.of(Perfil.SOCIO)), "cuenta socio2");
        ok(c.habilitarCuenta(admin, "C1", "entrenador", "ClaveRoles2026", Set.of(Perfil.SOCIO, Perfil.ENTRENADOR)),
                "cuenta combinada");
        ok(c.habilitarCuenta(admin, "F1", "fisio", "ClaveRoles2026", Set.of(Perfil.FISIOTERAPEUTA)),
                "cuenta fisioterapeuta");
        ok(c.habilitarCuenta(admin, "E1", "caja", "ClaveRoles2026", Set.of(Perfil.EMPLEADO)), "cuenta caja");
        c.getOperaciones().registrarInstalacion(admin,
                new Instalacion("TEN", "Tenis", c.buscarDeporte("TENIS"), 4, false));
        for (String id : List.of("J1", "J2", "C1")) {
            SoporteComercial.academiaHistorica(c, id, "TENIS");
        }
        var caja = c.getAcceso().autenticar("caja", "ClaveRoles2026");
        ok(c.getOperaciones().reservar(admin, "RESERVA-AJENA", "J2", "TEN", LocalDateTime.of(2026, 11, 6, 10, 0),
                LocalDateTime.of(2026, 11, 6, 11, 0), 2, "Sencillos").exito(), "reserva ajena de prueba");
        ok(c.getOperaciones().registrarPago(caja,
                new PagoMensualidad("PAGO-AJENO", c.buscarJugador("J2"), 100, LocalDate.of(2026, 11, 6), "2026-11"))
                .exito(), "pago ajeno de prueba");
        PersistenciaClubJson p = new PersistenciaClubJson();
        p.salvar(ruta, c);
        String texto = ejecutar(ruta, carpeta.resolve("anonimo.txt"), "11", "1", "0");
        ok(!texto.contains("8. Registrar producto") && !texto.contains("1. Registrar jugador")
                && !texto.contains("ID de la venta") && texto.contains("Opción no disponible"),
                "sin sesión no se muestran ni ejecutan operaciones de negocio");
        texto = ejecutar(ruta, carpeta.resolve("socio.txt"), "13", "socio1", "ClaveRoles2026", "11", "30", "5",
                "R-PROPIA", "TEN", "2026-11-06T12:00", "2026-11-06T13:00", "Sencillos", "6", "14", "29", "0");
        ok(texto.contains("5. Reservar para mí") && !texto.contains("11. Registrar venta")
                && !texto.contains("31. Registrar equipo") && texto.contains("Opción no disponible"),
                "socio ve solo sus acciones y no invoca caja ni después de salir");
        ok(!texto.contains("PAGO-AJENO") && !texto.contains("RESERVA-AJENA") && texto.contains("R-PROPIA"),
                "consultas de socio no filtran información de otros");
        EstadoClub despues = p.cargar(ruta);
        ok(despues.getReservas().buscarReserva("R-PROPIA").getJugador() == despues.buscarJugador("J1")
                && despues.getSocios().getPagosMensualidades().size() == 1,
                "reserva propia creada sin alterar pagos por opciones prohibidas");
        texto = ejecutar(ruta, carpeta.resolve("entrenador.txt"), "13", "entrenador", "ClaveRoles2026", "0");
        ok(texto.contains("34. Programar entrenamiento") && texto.contains("5. Reservar para mí")
                && texto.contains("43. Solicitar cambio de turno") && !texto.contains("11. Registrar venta")
                && !texto.contains("45. Resolver solicitud"),
                "entrenador-jugador ve unión de opciones sin permiso de caja o administración");
        texto = ejecutar(ruta, carpeta.resolve("fisio.txt"), "13", "fisio", "ClaveRoles2026", "0");
        ok(texto.contains("43. Solicitar cambio de turno") && !texto.contains("34. Programar entrenamiento")
                && !texto.contains("5. Reservar") && !texto.contains("11. Registrar venta"),
                "fisioterapeuta ve personal y no funciones de otros roles");
        texto = ejecutar(ruta, carpeta.resolve("caja.txt"), "13", "caja", "ClaveRoles2026", "0");
        ok(texto.contains("11. Registrar venta") && texto.contains("29. Registrar mensualidad")
                && !texto.contains("31. Registrar equipo") && !texto.contains("45. Resolver solicitud"),
                "caja sin opciones administrativas o técnicas deportivas");
        texto = ejecutar(ruta, carpeta.resolve("administrador.txt"), "13", "ADMIN-TEST", "ClavePrueba2026", "0");
        ok(texto.contains("1. Registrar jugador") && texto.contains("40. Configurar horario")
                && texto.contains("56. Actualizar login"), "administrador ve gestión integrada");
        c = p.cargar(ruta);
        admin = SoporteComercial.administrador(c);
        caja = c.getAcceso().autenticar("caja", "ClaveRoles2026");
        ok(!c.actualizarCuenta(admin, "E1", "socio1", Set.of(Perfil.EMPLEADO)),
                "actualización no puede tomar otro login");
        ok(!c.actualizarCuenta(admin, "ADMIN-TEST", "admin", Set.of(Perfil.SOCIO)),
                "no se retira permiso al último administrador");
        ok(c.actualizarCuenta(admin, "E1", "nuevo.coach", Set.of(Perfil.ENTRENADOR))
                && !c.getAcceso().estaAutenticada(caja) && c.getAcceso().autenticar("caja", "ClaveRoles2026") == null
                && c.getAcceso().autenticar("nuevo.coach", "ClaveRoles2026") != null,
                "cambio de login/perfiles revoca sesiones sin cambiar contraseña");
        var socio = c.getAcceso().autenticar("socio1", "ClaveRoles2026");
        ok(c.getAcceso().restablecerClave(admin, "J1", "NuevaClaveRoles2026") && !c.getAcceso().estaAutenticada(socio)
                && c.getAcceso().autenticar("socio1", "ClaveRoles2026") == null
                && c.getAcceso().autenticar("socio1", "NuevaClaveRoles2026") != null,
                "restablecimiento de clave protegido y sesiones revocadas");
        p.salvar(ruta, c);
        EstadoClub otra = p.cargar(ruta);
        ok(otra.getAcceso().autenticar("nuevo.coach", "ClaveRoles2026") != null
                && otra.getAcceso().autenticar("socio1", "NuevaClaveRoles2026") != null,
                "cambios de cuenta sobreviven recarga");
        System.out.println("Transcripciones de roles: " + carpeta.toAbsolutePath());
    }
}
