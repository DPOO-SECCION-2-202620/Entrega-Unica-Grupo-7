package pruebas;

import aplicacion.EstadoClub;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import modelo.ventas.*;
import persistencia.PersistenciaClubJson;

public class PruebaConsolaBloqueD {

    private static void ok(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    public static void main(String[] args) throws Exception {
        Path directorio = Files.createTempDirectory(Path.of(".build"), "consola-d-");
        Path archivo = directorio.resolve("club.json");
        String entradas = String.join("\n", "15", "ADMIN", "Administrador", "ClaveConsola2026", "13", "ADMIN",
                "ClaveConsola2026", "16", "E", "Empleado", "57", "E", "0", "EMPLEADO", "ClaveConsola2026", "17", "S",
                "Socio", "57", "S", "0", "SOCIO", "ClaveConsola2026", "19", "SEDE2", "8", "PRINCIPAL", "5", "ART",
                "Raqueta", "Deportes", "10000", "1", "20", "ART", "PRINCIPAL", "SEDE2", "14", "13", "E",
                "ClaveConsola2026", "21", "SG", "SEDE2", "3", "SUG", "Snack aprobado", "Comida", "1000", "3",
                "pendiente", "23", "24", "SG", "gluten", "14", "13", "ADMIN", "ClaveConsola2026", "22", "SG", "s", "14",
                "13", "E", "ClaveConsola2026", "11", "SEDE2", "VUI", "CAFETERIA", "SUG", "1", "0", "S", "0", "0", "0",
                "SI", "23", "21", "RECH", "SEDE2", "1", "NO", "Rechazado", "Otros", "100", "1", "14", "13", "ADMIN",
                "ClaveConsola2026", "22", "RECH", "n", "14", "13", "E", "ClaveConsola2026", "23", "0", "");
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        Process proceso = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp",
                System.getProperty("java.class.path"), "consola.ConsolaClub", archivo.toString())
                .redirectErrorStream(true).start();
        try (var entrada = proceso.getOutputStream()) {
            entrada.write(entradas.getBytes(StandardCharsets.UTF_8));
        }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(directorio.resolve("transcripcion.txt"), salida);
        ok(proceso.waitFor() == 0 && !salida.contains("No se realizó la operación"),
                "recorrido de consola sin errores");
        EstadoClub club = new PersistenciaClubJson().cargar(archivo);
        ok(club.getInventario().buscarProducto("ART") == null && club.getInventario().consultarStock("SEDE2",
                club.getInventario().buscarProductoCatalogo("ART")) == 5, "traslado desde menú persistente");
        Venta v = club.getVentas().buscarVenta("VUI");
        ok(v != null && v.getUbicacion().equals("SEDE2") && v.getValorFinalPagado() == 1080
                && club.getInventario().consultarStock("SEDE2",
                        club.getInventario().buscarProductoCatalogo("SUG")) == 2,
                "aprobar y vender desde menú en ubicación destino");
        int aviso = salida.indexOf("Snack aprobado | Alérgenos: [gluten]");
        int confirmar = salida.indexOf("Confirmar compra (SI/NO)");
        ok(aviso >= 0 && confirmar > aviso && salida.contains("Propina sugerida"),
                "alérgenos y sugerencia antes de confirmar");
        ok(salida.contains("PENDIENTE") && salida.contains("APROBADA") && salida.contains("RECHAZADA")
                && club.getInventario().buscarProductoCatalogo("NO") == null,
                "autor observa estados y rechazo no crea producto");
        System.out.println("Transcripción: " + directorio.resolve("transcripcion.txt").toAbsolutePath());
    }
}
