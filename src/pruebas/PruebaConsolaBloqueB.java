package pruebas;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaConsolaBloqueB {

    private static void ok(boolean c, String mensaje) {
        if (!c) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static String ejecutar(List<String> argumentos, String entradas) throws Exception {
        List<String> comando = new ArrayList<>(
                List.of(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(), "-Dfile.encoding=UTF-8",
                        "-Dstdout.encoding=UTF-8", "-cp", System.getProperty("java.class.path")));
        comando.addAll(argumentos);
        Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
        try (var entrada = proceso.getOutputStream()) {
            entrada.write(entradas.getBytes(StandardCharsets.UTF_8));
        }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (proceso.waitFor() != 0) {
            throw new AssertionError(salida);
        }
        return salida;
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory(Path.of(".build"), "consola-b-");
        Path archivo = carpeta.resolve("club.json");
        ejecutar(List.of("pruebas.PruebaBloqueB", "crear", archivo.toString()), "");
        String entradas = String.join("\n", "13", "COACH", "ClavePrueba2026", "58", "TRANSFERENCIA", "EC", "59",
                "TRANSFERENCIA", "EC", "C0,C1,C2,C3,C4,C5,C6", "52", "TRANSFERENCIA", "2", "0", "60", "TRANSFERENCIA",
                "64", "62", "C0", "EC", "Retiro de prueba", "14", "13", "ADMIN-TEST", "ClavePrueba2026", "63", "EC",
                "n", "0", "");
        String salida = ejecutar(List.of("consola.ConsolaClub", archivo.toString()), entradas);
        Files.writeString(carpeta.resolve("transcripcion.txt"), salida);
        ok(!salida.contains("No se realizó la operación"), "recorrido B de consola sin errores");
        ok(salida.contains("Habilitados: 7") && salida.contains("Suspensión vigente")
                && salida.contains("Cumplida en TRANSFERENCIA"), "consola explica habilitación y cumplimiento");
        var club = new PersistenciaClubJson().cargar(archivo);
        ok(club.getPartidos().buscarPartido("TRANSFERENCIA").estaCerrado()
                && !club.getEquipos().buscarEquipo("EC").estaActivo(),
                "resultado, cierre, retiro y baja guardados desde menú");
        ok(club.getPartidos().buscarPartido("TRANSFERENCIA").getParticipantes("EC").contains(club.buscarJugador("C0"))
                && club.getEquipos().buscarEquipo("EC").getInscripciones().isEmpty(),
                "baja conserva selección del partido jugado");
        ok(club.getHabilitacion().getSuspensiones().stream().allMatch(s -> s.partidoCumplimientoId() != null),
                "última suspensión cumplida una vez");
        System.out.println("Transcripción: " + carpeta.resolve("transcripcion.txt").toAbsolutePath());
    }
}
