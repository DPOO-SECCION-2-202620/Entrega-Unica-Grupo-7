package pruebas;

import aplicacion.EstadoClub;
import java.nio.file.*;
import java.time.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.partidos.*;
import modelo.usuarios.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaMigracionBloqueB {

    private static void ok(boolean c, String mensaje) {
        if (!c) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    public static void main(String[] args) throws Exception {
        EstadoClub c = new EstadoClub();
        SoporteComercial.administrador(c);
        Equipo equipo = new Equipo("V", "Voleibol", c.buscarDeporte("VOLEIBOL"), "Mayores", 10);
        c.getEquipos().registrarEquipo(equipo);
        Competencia competencia = new Competencia("LEG", "Histórico", "Club", 2026, c.buscarDeporte("VOLEIBOL"),
                "Mayores", new FormatoLiga());
        c.getCompetencias().registrarCompetencia(competencia);
        NominaCompetencia nomina = new NominaCompetencia(competencia, equipo);
        for (int i = 0; i < 6; i++) {
            c.registrarJugador("J" + i, "J" + i, LocalDate.of(2000, 1, 1));
            Jugador j = c.buscarJugador("J" + i);
            c.getEquipos().inscribirJugador(j, equipo, "Jugador", i + 1);
            nomina.agregarJugador(j);
        }
        c.getCompetencias().registrarNomina(nomina);
        PartidoCompetencia partido = new PartidoCompetencia("OLD", LocalDateTime.of(2026, 10, 1, 12, 0),
                c.buscarDeporte("VOLEIBOL"), competencia, equipo, "Rival", "Escenario");
        c.getPartidos().registrarPartido(partido);
        ok(partido.restaurarResultado(1, 0, nomina, true), "preparación de marcador válido en formato anterior");
        partido.restaurarEstado(false, false, true);
        EstadisticaPartido estadistica = new EstadisticaPartido(c.buscarJugador("J0"), partido);
        estadistica.registrarMetrica(TipoEstadistica.ACES, 1);
        partido.registrarEstadistica(estadistica);
        Path carpeta = Files.createTempDirectory(Path.of(".build"), "migracion-b-");
        Path viejo = carpeta.resolve("version4.json"), actual = carpeta.resolve("version5.json");
        PersistenciaClubJson p = new PersistenciaClubJson();
        p.salvar(viejo, c);
        JSONObject raiz = new JSONObject(Files.readString(viejo));
        raiz.put("version", 4);
        raiz.remove("amarillasAcumuladas");
        raiz.remove("suspensiones");
        JSONObject j = raiz.getJSONArray("partidos").getJSONObject(0);
        for (String campo : new String[] { "selecciones", "plantillas", "cancelado", "cerrado", "legado",
                "setsParaGanar", }) {
            j.remove(campo);
        }
        raiz.getJSONArray("competencias").getJSONObject(0).remove("setsParaGanar");
        raiz.getJSONArray("competencias").getJSONObject(0).put("puntos", new JSONObject());
        Files.writeString(viejo, raiz.toString());
        String original = Files.readString(viejo);
        EstadoClub migrado = p.cargar(viejo);
        PartidoCompetencia historico = (PartidoCompetencia) migrado.getPartidos().buscarPartido("OLD");
        ok(historico.esLegado() && historico.getMarcadorClub() == 1 && historico.getMarcadorRival() == 0
                && historico.getSelecciones().isEmpty(),
                "marcador histórico conservado sin inventar alineación o sets");
        ok(historico.getEstadisticas().size() == 1 && migrado.getHabilitacion().getSuspensiones().isEmpty(),
                "estadísticas conservadas y ninguna suspensión retroactiva");
        var admin = SoporteComercial.administrador(migrado);
        ok(!migrado.getOperaciones().cerrarActa(admin, "OLD").exito(),
                "legado sin acta histórica no genera disciplina automáticamente");
        p.salvar(actual, migrado);
        EstadoClub segunda = p.cargar(actual);
        ok(((PartidoCompetencia) segunda.getPartidos().buscarPartido("OLD")).getMarcadorClub() == 1
                && original.equals(Files.readString(viejo)),
                "segunda recarga conserva resultado y archivo original intacto");
    }
}
