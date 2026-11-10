package persistencia;

import aplicacion.EstadoClub;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import modelo.deportes.Equipo;
import modelo.deportes.NivelAcademia;
import modelo.torneos.*;
import org.json.JSONArray;
import org.json.JSONObject;

final class PersistenciaTorneosInternos {

    private static JSONObject participante(CandidatoTorneo candidato) {
        Object equipoIdJson;
        if (candidato.equipoId() == null) {
            equipoIdJson = JSONObject.NULL;
        } else {
            equipoIdJson = candidato.equipoId();
        }
        return new JSONObject().put("id", candidato.id()).put("nombre", candidato.nombre())
                .put("jugadores", new JSONArray(candidato.jugadores())).put("puntaje", candidato.puntaje())
                .put("equipoId", equipoIdJson);
    }

    static void salvar(JSONObject raiz, EstadoClub club) {
        JSONArray torneos = new JSONArray();
        for (TorneoInterno torneo : club.getTorneos().consultarParaPersistencia()) {
            JSONArray clasificacion = new JSONArray();
            JSONArray inscripciones = new JSONArray();
            for (CandidatoTorneo candidato : torneo.getClasificacion()) {
                clasificacion.put(participante(candidato));
            }
            for (InscripcionTorneo inscripcion : torneo.getInscripciones()) {
                inscripciones.put(new JSONObject().put("id", inscripcion.id())
                        .put("participante", participante(inscripcion.participante()))
                        .put("cupo", inscripcion.cupo().name()).put("activa", inscripcion.activa()));
            }
            Object categoriaJson;
            if (torneo.getCategoria() == null) {
                categoriaJson = JSONObject.NULL;
            } else {
                categoriaJson = torneo.getCategoria();
            }
            Object nivelJson;
            if (torneo.getNivel() == null) {
                nivelJson = JSONObject.NULL;
            } else {
                nivelJson = torneo.getNivel().name();
            }
            torneos.put(new JSONObject().put("id", torneo.getId()).put("nombre", torneo.getNombre())
                    .put("deporteId", torneo.getDeporte().getId()).put("categoria", categoriaJson)
                    .put("nivel", nivelJson).put("modalidad", torneo.getModalidad().name())
                    .put("capacidad", torneo.getCapacidad()).put("inicio", torneo.getInicio().toString())
                    .put("fechaLiberacion", torneo.getFechaLiberacion().toString()).put("tipo", torneo.getTipo().name())
                    .put("tarifa", torneo.getTarifa().toPlainString()).put("criterio", torneo.getCriterio())
                    .put("clasificacion", clasificacion).put("inscripciones", inscripciones)
                    .put("preferencialesLiberados", torneo.estanPreferencialesLiberados()));
        }
        raiz.put("torneosInternos", torneos);
    }

    private static CandidatoTorneo leerParticipante(JSONObject json, TorneoInterno torneo, EstadoClub club) {
        List<String> jugadores = new ArrayList<>();
        JSONArray ids = json.getJSONArray("jugadores");
        for (int i = 0; i < ids.length(); i++) {
            String id = ids.getString(i);
            if (id.startsWith("EXTERNO:")) {
                if (!json.getString("id").equals(id)) {
                    throw new IllegalArgumentException("Identidad externa inconsistente.");
                }
                jugadores.add(id);
                continue;
            }

            if (club.buscarJugador(id) == null) {
                throw new IllegalArgumentException("Participante de torneo inexistente.");
            }
            jugadores.add(id);
        }
        String equipoId;
        if (json.isNull("equipoId")) {
            equipoId = null;
        } else {
            equipoId = json.getString("equipoId");
        }

        if (equipoId == null && torneo.getModalidad() == ModalidadTorneo.SENCILLOS && jugadores.size() == 1
                && jugadores.get(0).startsWith("EXTERNO:") && !json.getString("id").equals(jugadores.get(0))) {
            throw new IllegalArgumentException("Identidad externa individual inconsistente.");
        }

        if (equipoId != null) {
            Equipo equipo = club.getEquipos().buscarEquipo(equipoId);
            if (equipo == null || equipo.getDeporte() != torneo.getDeporte()
                    || !equipo.getCategoria().equals(torneo.getCategoria())
                    || jugadores.stream().anyMatch(id -> equipo.getHistorialInscripciones().stream()
                            .noneMatch(i -> i.getJugador() == club.buscarJugador(id)))) {
                throw new IllegalArgumentException("Equipo o integrantes históricos incompatibles con el torneo.");
            }
        } else if (torneo.getModalidad() == ModalidadTorneo.SENCILLOS
                && (jugadores.size() != 1 || !json.getString("id").equals(jugadores.get(0)))) {
            throw new IllegalArgumentException("Identidad individual de torneo inconsistente.");
        } else if (torneo.getModalidad() == ModalidadTorneo.DOBLES) {
            if (jugadores.size() != 2 || !jugadores.equals(jugadores.stream().sorted().toList())) {
                throw new IllegalArgumentException("Pareja inconsistente.");
            }
            String id = "PAREJA:" + jugadores.get(0).length() + ":" + jugadores.get(0) + jugadores.get(1).length() + ":"
                    + jugadores.get(1);
            if (!id.equals(json.getString("id"))) {
                throw new IllegalArgumentException("Identidad de pareja inconsistente.");
            }
        }
        return new CandidatoTorneo(json.getString("id"), json.getString("nombre"), jugadores, equipoId,
                json.getDouble("puntaje"));
    }

    static void cargar(JSONObject raiz, EstadoClub club) {
        JSONArray torneos = raiz.getJSONArray("torneosInternos");
        for (int i = 0; i < torneos.length(); i++) {
            JSONObject json = torneos.getJSONObject(i);
            String categoria;
            if (json.isNull("categoria")) {
                categoria = null;
            } else {
                categoria = json.getString("categoria");
            }
            NivelAcademia nivel;
            if (json.isNull("nivel")) {
                nivel = null;
            } else {
                nivel = NivelAcademia.valueOf(json.getString("nivel"));
            }
            TorneoInterno torneo = new TorneoInterno(json.getString("id"), json.getString("nombre"),
                    club.buscarDeporte(json.getString("deporteId")), categoria, nivel,
                    ModalidadTorneo.valueOf(json.getString("modalidad")), json.getInt("capacidad"),
                    LocalDateTime.parse(json.getString("inicio")),
                    LocalDateTime.parse(json.getString("fechaLiberacion")), TipoTorneo.valueOf(json.getString("tipo")),
                    new BigDecimal(json.getString("tarifa")));
            if (torneo.getTarifa().compareTo(new BigDecimal(json.getString("tarifa"))) != 0) {
                throw new IllegalArgumentException("Tarifa recreativa distinta de cero.");
            }
            JSONArray clasificacion = json.getJSONArray("clasificacion");
            List<CandidatoTorneo> candidatos = new ArrayList<>();
            for (int k = 0; k < clasificacion.length(); k++) {
                candidatos.add(leerParticipante(clasificacion.getJSONObject(k), torneo, club));
            }
            if (!json.getString("criterio").isBlank()) {
                torneo.asignarClasificacion(json.getString("criterio"), candidatos);
            } else if (!candidatos.isEmpty()) {
                throw new IllegalArgumentException("Clasificación sin criterio.");
            }
            JSONArray inscripciones = json.getJSONArray("inscripciones");
            for (int k = 0; k < inscripciones.length(); k++) {
                JSONObject inscripcion = inscripciones.getJSONObject(k);
                torneo.restaurarInscripcion(new InscripcionTorneo(inscripcion.getString("id"),
                        leerParticipante(inscripcion.getJSONObject("participante"), torneo, club),
                        TipoCupo.valueOf(inscripcion.getString("cupo")), inscripcion.getBoolean("activa")));
            }
            torneo.restaurarLiberacion(json.getBoolean("preferencialesLiberados"));
            club.getTorneos().cargar(torneo);
        }
    }
}
