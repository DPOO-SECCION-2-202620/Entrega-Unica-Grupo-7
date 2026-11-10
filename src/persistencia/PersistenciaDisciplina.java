package persistencia;

import aplicacion.EstadoClub;
import gestores.GestorHabilitacion.*;
import java.util.*;
import modelo.partidos.Partido;
import modelo.usuarios.Jugador;
import org.json.*;

final class PersistenciaDisciplina {

    static JSONObject listas(Map<String, List<Jugador>> listas) {
        JSONObject j = new JSONObject();
        for (var e : listas.entrySet()) {
            j.put(e.getKey(), new JSONArray(e.getValue().stream().map(Jugador::getId).toList()));
        }
        return j;
    }

    static void cargarListas(JSONObject j, Partido partido, EstadoClub c, boolean plantilla) {
        for (String equipo : j.keySet()) {
            JSONArray ids = j.getJSONArray(equipo);
            List<Jugador> lista = new ArrayList<>();
            for (int i = 0; i < ids.length(); i++) {
                Jugador jugador = c.buscarJugador(ids.getString(i));
                if (jugador == null) {
                    throw new IllegalArgumentException("Jugador de alineación/plantilla inexistente.");
                }
                lista.add(jugador);
            }
            if (plantilla) {
                partido.restaurarPlantilla(equipo, lista);
            } else {
                partido.restaurarSeleccion(equipo, lista);
            }
        }
    }

    static void salvar(JSONObject raiz, EstadoClub c) {
        c.getHabilitacion().validarHistorial();
        JSONArray amarillas = new JSONArray(), suspensiones = new JSONArray();
        for (var e : c.getHabilitacion().getAmarillas().entrySet()) {
            amarillas.put(new JSONObject().put("ambito", e.getKey().ambito()).put("jugadorId", e.getKey().jugadorId())
                    .put("cantidad", e.getValue()));
        }
        for (Suspension s : c.getHabilitacion().getSuspensiones()) {
            Object partidoCumplimientoIdJson;
            if (s.partidoCumplimientoId() == null) {
                partidoCumplimientoIdJson = JSONObject.NULL;
            } else {
                partidoCumplimientoIdJson = s.partidoCumplimientoId();
            }
            Object equipoCumplimientoIdJson;
            if (s.equipoCumplimientoId() == null) {
                equipoCumplimientoIdJson = JSONObject.NULL;
            } else {
                equipoCumplimientoIdJson = s.equipoCumplimientoId();
            }
            suspensiones.put(new JSONObject().put("id", s.id()).put("ambito", s.ambito())
                    .put("jugadorId", s.jugadorId()).put("equipoOrigenId", s.equipoOrigenId())
                    .put("partidoOrigenId", s.partidoOrigenId()).put("partidoCumplimientoId", partidoCumplimientoIdJson)
                    .put("equipoCumplimientoId", equipoCumplimientoIdJson));
        }
        raiz.put("amarillasAcumuladas", amarillas).put("suspensiones", suspensiones);
    }

    static void cargar(JSONObject raiz, EstadoClub c) {
        Map<Clave, Integer> contadores = new LinkedHashMap<>();
        List<Suspension> historial = new ArrayList<>();
        JSONArray amarillas = raiz.getJSONArray("amarillasAcumuladas"),
                suspensiones = raiz.getJSONArray("suspensiones");
        for (int i = 0; i < amarillas.length(); i++) {
            JSONObject j = amarillas.getJSONObject(i);
            if (contadores.putIfAbsent(new Clave(j.getString("ambito"), j.getString("jugadorId")),
                    j.getInt("cantidad")) != null) {
                throw new IllegalArgumentException("Contador de amarillas duplicado.");
            }
        }
        for (int i = 0; i < suspensiones.length(); i++) {
            JSONObject j = suspensiones.getJSONObject(i);
            String datoRegistrado;
            if (j.isNull("partidoCumplimientoId")) {
                datoRegistrado = null;
            } else {
                datoRegistrado = j.getString("partidoCumplimientoId");
            }
            String datoRegistrado2;
            if (j.isNull("equipoCumplimientoId")) {
                datoRegistrado2 = null;
            } else {
                datoRegistrado2 = j.getString("equipoCumplimientoId");
            }
            historial.add(new Suspension(j.getString("id"), j.getString("ambito"), j.getString("jugadorId"),
                    j.getString("equipoOrigenId"), j.getString("partidoOrigenId"), datoRegistrado, datoRegistrado2));
        }
        c.getHabilitacion().cargarDisciplina(contadores, historial);
    }
}
