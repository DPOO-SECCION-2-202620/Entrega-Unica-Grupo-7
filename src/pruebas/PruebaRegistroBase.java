package pruebas;

import aplicacion.EstadoClub;
import consola.MenuPorRol;
import gestores.GestorAcceso.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import persistencia.PersistenciaClubJson;

public class PruebaRegistroBase {

    private static void ok(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static String consola(Path archivo, Path log, String... entradas) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process proceso = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp",
                System.getProperty("java.class.path"), "consola.ConsolaClub", archivo.toString())
                .redirectErrorStream(true).start();
        try (var entrada = proceso.getOutputStream()) {
            entrada.write((String.join("\n", entradas) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(log, salida);
        ok(proceso.waitFor() == 0, "consola finaliza sin error");
        return salida;
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory(Path.of(".build"), "registro-base-");
        Path archivo = carpeta.resolve("club.json");
        String salida = consola(archivo, carpeta.resolve("registro.txt"), "18", "BASE01", "Cuenta nueva", "mi.login",
                "ClaveUsuario2026", "13", "mi.login", "ClaveUsuario2026", "57", "14", "0");
        PersistenciaClubJson p = new PersistenciaClubJson();
        EstadoClub c = p.cargar(archivo);
        ok(!c.getAcceso().tieneAdministrador() && c.getAcceso().getCuentas().size() == 1,
                "registro público no crea administrador ni asigna roles");
        Sesion base = c.getAcceso().autenticar("mi.login", "ClaveUsuario2026");
        ok(base != null && c.getAcceso().getPerfiles(base).equals(Set.of(Perfil.BASE)),
                "cuenta base autentica después de recarga");
        ok(MenuPorRol.opciones(c, base).keySet().equals(new LinkedHashSet<>(List.of("7", "14", "18", "0")))
                && salida.contains("Opción no disponible") && !salida.contains("Perfiles separados por coma"),
                "base sin opciones de negocio ni selección de roles");
        ok(!c.actualizarCuenta(base, "BASE01", "mi.login", Set.of(Perfil.ADMINISTRADOR))
                && !c.habilitarCuenta(base, "BASE01", "otra", "ClaveUsuario2026", Set.of(Perfil.SOCIO)),
                "cuenta base no se asigna permisos por servicio");
        int usuarios = c.getUsuarios().size();
        ok(!c.registrarCuentaBase("OTRO", "Otro", " MI.LOGIN ", "ClaveUsuario2026") && c.buscarUsuario("OTRO") == null
                && !c.registrarCuentaBase("INVALIDO", "Inválido", "invalido", "corta")
                && c.getUsuarios().size() == usuarios, "login repetido/clave corta rechazan sin identidades parciales");
        ok(!c.registrarCuentaBase("BASE01", "Suplantación", "otra", "ClaveUsuario2026"),
                "registro no puede tomar una identidad existente");
        Cuenta original = c.getAcceso().getCuentas().get(0);
        salida = consola(archivo, carpeta.resolve("asignacion.txt"), "15", "ADMIN", "Administrador", "ClaveAdmin2026",
                "13", "ADMIN", "ClaveAdmin2026", "57", "BASE01", "SOCIO", "14", "13", "mi.login", "ClaveUsuario2026",
                "0");
        c = p.cargar(archivo);
        Sesion socio = c.getAcceso().autenticar("mi.login", "ClaveUsuario2026");
        Cuenta asignada = c.getAcceso().getCuentas().stream().filter(cuenta -> cuenta.usuarioId().equals("BASE01"))
                .findFirst().orElseThrow();
        ok(socio != null && c.getAcceso().getPerfiles(socio).equals(Set.of(Perfil.SOCIO)),
                "administrador asigna perfil y usuario entra con sus credenciales originales");
        ok(original.hash().equals(asignada.hash()) && original.sal().equals(asignada.sal())
                && original.login().equals(asignada.login()), "asignación conserva login y contraseña elegidos");
        ok(salida.contains("Pendiente de asignación: BASE01") && salida.contains("5. Reservar para mí"),
                "administrador ve pendientes y el menú refleja el perfil asignado");
        ok(c.registrarCuentaBase("BASE02", "Segunda cuenta", "segunda", "ClaveUsuario2026")
                && !c.getAcceso().tienePerfil("BASE02", Perfil.SOCIO),
                "registro público sigue abierto con administrador existente");
        Sesion admin = c.getAcceso().autenticar("ADMIN", "ClaveAdmin2026"),
                sesionBase = c.getAcceso().autenticar("segunda", "ClaveUsuario2026");
        ok(c.actualizarCuenta(admin, "BASE02", "segunda", Set.of(Perfil.SOCIO))
                && !c.getAcceso().estaAutenticada(sesionBase), "asignar perfil revoca sesión base anterior");
        p.salvar(archivo, c);
        ok(p.cargar(archivo).getAcceso().tienePerfil("BASE02", Perfil.SOCIO), "asignación posterior persistente");
    }
}
