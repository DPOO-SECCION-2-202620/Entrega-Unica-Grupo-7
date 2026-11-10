package pruebas;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import modelo.competencias.FormatoFases;
import modelo.partidos.PartidoAcademia;
import persistencia.PersistenciaClubJson;

public class PruebaConsolaBC {

    static void ok(boolean c, String m) {
        PruebaCierreBloqueC.ok(c, m);
    }

    static String ejecutar(
        Path log,
        List<String> argumentos,
        String... entradas
    ) throws Exception {
        List<String> cmd = new ArrayList<>(
            List.of(
                Path.of(
                    System.getProperty("java.home"),
                    "bin",
                    "java.exe"
                ).toString(),
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-cp",
                System.getProperty("java.class.path")
            )
        );
        cmd.addAll(argumentos);
        Process proceso = new ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start();
        try (var entrada = proceso.getOutputStream()) {
            entrada.write(
                (String.join("\n", entradas) + "\n").getBytes(
                    StandardCharsets.UTF_8
                )
            );
        }
        String salida = new String(
            proceso.getInputStream().readAllBytes(),
            StandardCharsets.UTF_8
        );
        Files.writeString(log, salida);
        ok(proceso.waitFor() == 0, "proceso correcto: " + log.getFileName());
        return salida;
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory(
            Path.of(".build"),
            "consola-bc-"
        );
        Path academia = carpeta.resolve("academia.json"),
            torneo = carpeta.resolve("torneo.json");
        ejecutar(
            carpeta.resolve("fixture-c.txt"),
            List.of("pruebas.PruebaCierreBloqueC", "crear", academia.toString())
        );
        String salida = ejecutar(
            carpeta.resolve("socio.txt"),
            List.of("consola.ConsolaClub", academia.toString()),
            "13",
            "A",
            "ClavePrueba2026",
            "65",
            "1",
            "MESA",
            "65",
            "3",
            "TENIS",
            "65",
            "4",
            "67",
            "4",
            "RESERVA",
            "R3",
            "Ajena",
            "68",
            "TENIS",
            "0",
            "ACES",
            "2026-11-07",
            "2026-11-09",
            "0"
        );
        ok(
            salida.contains("Inscripción a academia registrada") &&
                salida.contains("Sin permiso docente") &&
                salida.contains("Actividad, motivo o permiso inválidos"),
            "socio puede inscribirse, no programar ni cancelar actividades ajenas"
        );
        ok(
            !salida.contains("jugadorId=B,") && salida.contains("jugadorId=A,"),
            "estadísticas propias sin filtrar las de otro socio"
        );
        salida = ejecutar(
            carpeta.resolve("coach.txt"),
            List.of("consola.ConsolaClub", academia.toString()),
            "13",
            "COACH",
            "ClavePrueba2026",
            "65",
            "4",
            "CLI-M",
            "TENIS",
            "Sencillos",
            "A",
            "B",
            "T1",
            "2026-11-09T09:00",
            "60",
            "2",
            "52",
            "CLI-M",
            "2",
            "0",
            "65",
            "5",
            "CLI-G",
            "TENIS",
            "PRINCIPIANTE",
            "COACH",
            "T3",
            "2026-11-10T10:00",
            "60",
            "36",
            "CLI-G",
            "A",
            "37",
            "CLI-G",
            "A",
            "ASISTIO",
            "4",
            "Buena sesión",
            "65",
            "5",
            "CLI-CANCEL",
            "TENIS",
            "PRINCIPIANTE",
            "COACH",
            "T3",
            "2026-11-11T10:00",
            "60",
            "67",
            "3",
            "ENTRENAMIENTO",
            "CLI-CANCEL",
            "T2",
            "2026-11-11T11:00",
            "60",
            "Lluvia",
            "67",
            "4",
            "ENTRENAMIENTO",
            "CLI-CANCEL",
            "Cierre de cancha",
            "67",
            "1",
            "2026-11-09",
            "2026-11-12",
            "0"
        );
        ok(
            !salida.contains("No se realizó la operación") &&
                !salida.contains("Operación rechazada"),
            "consola completa partido, ELO, entrenamiento, asistencia y cambios de agenda"
        );
        var p = new PersistenciaClubJson();
        var c = p.cargar(academia);
        ok(
            c.getPartidos().buscarPartido("CLI-M") instanceof
                PartidoAcademia m &&
                m.estaCerrado() &&
                m.getCambiosElo().size() == 2 &&
                c
                    .getEntrenamientos()
                    .buscarEntrenamiento("CLI-G")
                    .getRegistroAsistencia()
                    .size() == 1,
            "cambios deportivos de consola persistidos"
        );
        ok(
            c
                .getEntrenamientos()
                .buscarEntrenamiento("CLI-CANCEL")
                .estaCancelada() &&
                c
                    .getEntrenamientos()
                    .buscarEntrenamiento("CLI-CANCEL")
                    .getCambiosAgenda()
                    .size() == 2,
            "historia de agenda guardada por consola"
        );
        ejecutar(
            carpeta.resolve("fixture-b.txt"),
            List.of("pruebas.PruebaCierreTorneos", "crear", torneo.toString())
        );
        salida = ejecutar(
            carpeta.resolve("torneo.txt"),
            List.of("consola.ConsolaClub", torneo.toString()),
            "13",
            "ADMIN-TEST",
            "ClavePrueba2026",
            "66",
            "6",
            "COPA",
            "66",
            "4",
            "COPA",
            "ELIM-2-1",
            "2",
            "1",
            "0",
            "66",
            "6",
            "COPA",
            "66",
            "1",
            "COPA",
            "0",
            "49",
            "NUEVA",
            "Copa nueva",
            "Club",
            "2026",
            "FUTBOL",
            "Mayores",
            "FASES",
            "66",
            "2",
            "NUEVA",
            "EXT-A",
            "Rival nuevo",
            "0"
        );
        ok(
            !salida.contains("No se realizó la operación") &&
                !salida.contains("Operación rechazada") &&
                salida.contains("Campeón: A"),
            "consola avanza fases, declara campeón y crea torneo por fases"
        );
        c = p.cargar(torneo);
        ok(
            (
                (FormatoFases) c
                    .getCompetencias()
                    .buscarCompetencia("COPA")
                    .getFormato()
            )
                .getCampeon()
                .equals("A") &&
                (
                    (FormatoFases) c
                        .getCompetencias()
                        .buscarCompetencia("NUEVA")
                        .getFormato()
                ).buscarParticipante("EXT-A") != null,
            "campeón y participante nuevo recuperados"
        );
        System.out.println("Transcripciones: " + carpeta.toAbsolutePath());
    }
}
