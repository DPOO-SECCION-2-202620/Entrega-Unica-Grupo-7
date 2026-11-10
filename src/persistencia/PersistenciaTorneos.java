package persistencia;

import aplicacion.EstadoClub;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.Deporte;
import modelo.partidos.*;
import org.json.*;

final class PersistenciaTorneos {

    private static void exigir(boolean c, String m) {
        if (!c) {
            throw new IllegalArgumentException(m);
        }
    }

    static JSONArray tabla(FormatoLiga f) {
        JSONArray filas = new JSONArray();
        for (var r : f.getTabla()) {
            filas.put(
                    new JSONObject().put("id", r.participanteId()).put("nombre", r.nombre()).put("jugados", r.jugados())
                            .put("ganados", r.ganados()).put("empatados", r.empatados()).put("perdidos", r.perdidos())
                            .put("favor", r.favor()).put("contra", r.contra()).put("puntos", r.puntos()));
        }
        return filas;
    }

    static JSONObject salvar(FormatoLiga f) {
        JSONObject j = new JSONObject();
        JSONArray participantes = new JSONArray(), encuentros = new JSONArray();
        for (var p : f.getParticipantes()) {
            Object equipoClubIdJson;
            if (p.equipoClubId() == null) {
                equipoClubIdJson = JSONObject.NULL;
            } else {
                equipoClubIdJson = p.equipoClubId();
            }
            participantes.put(new JSONObject().put("id", p.id()).put("nombre", p.nombre())
                    .put("equipoClubId", equipoClubIdJson).put("activo", p.activo()));
        }
        for (var e : f.getEncuentros()) {
            Object marcadorLocalJson;
            if (e.local() == null) {
                marcadorLocalJson = JSONObject.NULL;
            } else {
                marcadorLocalJson = e.local();
            }
            Object marcadorVisitanteJson;
            if (e.visitante() == null) {
                marcadorVisitanteJson = JSONObject.NULL;
            } else {
                marcadorVisitanteJson = e.visitante();
            }
            Object desempateJson;
            if (e.ganadorDesempate() == null) {
                desempateJson = JSONObject.NULL;
            } else {
                desempateJson = e.ganadorDesempate();
            }
            encuentros.put(new JSONObject().put("id", e.id()).put("localId", e.localId())
                    .put("visitanteId", e.visitanteId()).put("fase", e.fase()).put("grupo", e.grupo())
                    .put("local", marcadorLocalJson).put("visitante", marcadorVisitanteJson)
                    .put("desempate", desempateJson).put("administrativo", e.administrativo()));
        }
        var r = f.getReglas();
        j.put("reglas", new JSONObject().put("victoria", r.victoria()).put("empate", r.empate())
                .put("derrota", r.derrota()).put("ajustadaSets", r.ajustadaPorSets()));
        j.put("participantes", participantes).put("encuentros", encuentros)
                .put("vinculos", new JSONObject(f.getVinculos())).put("tabla", tabla(f));
        if (f instanceof FormatoFases fases) {
            JSONObject grupos = new JSONObject();
            fases.getGrupos().forEach((nombre, ids) -> grupos.put(nombre, new JSONArray(ids)));
            Object campeonJson;
            if (fases.getCampeon() == null) {
                campeonJson = JSONObject.NULL;
            } else {
                campeonJson = fases.getCampeon();
            }
            j.put("grupos", grupos).put("ordenGrupos", new JSONArray(fases.getGrupos().keySet()))
                    .put("clasificados", fases.getClasificadosPorGrupo()).put("ronda", fases.getRonda())
                    .put("campeon", campeonJson);
        }
        return j;
    }

    static FormatoLiga leer(JSONObject competencia, EstadoClub c) {
        String tipo = competencia.getString("formato");
        exigir(tipo.equals("LIGA") || tipo.equals("FASES"), "Formato de competencia desconocido.");
        FormatoLiga f;
        if (tipo.equals("FASES")) {
            f = new FormatoFases();
        } else {
            f = new FormatoLiga();
        }
        Deporte d = c.buscarDeporte(competencia.getString("deporteId"));
        exigir(d != null, "Deporte de torneo inexistente.");
        f.configurarDeporte(d);
        JSONObject j = competencia.getJSONObject("torneo"), reglas = j.getJSONObject("reglas");
        exigir(f.configurarPuntuacion(new ReglasPuntuacion(reglas.getInt("victoria"), reglas.getInt("empate"),
                reglas.getInt("derrota"), reglas.getBoolean("ajustadaSets")))
                && f.configurarSets(competencia.getInt("setsParaGanar")), "Configuración de torneo inválida.");
        JSONArray participantes = j.getJSONArray("participantes");
        for (int i = 0; i < participantes.length(); i++) {
            JSONObject p = participantes.getJSONObject(i);
            String equipo;
            if (p.isNull("equipoClubId")) {
                equipo = null;
            } else {
                equipo = p.getString("equipoClubId");
            }

            if (equipo != null) {
                exigir(c.getEquipos().buscarEquipo(equipo) != null
                        && c.getEquipos().buscarEquipo(equipo).getDeporte() == d
                        && c.getEquipos().buscarEquipo(equipo).getCategoria()
                                .equals(competencia.getString("categoria")),
                        "Participante del club inexistente o incompatible.");
            }
            exigir(f.registrarParticipante(new ParticipanteCompetencia(p.getString("id"), p.getString("nombre"), equipo,
                    p.getBoolean("activo"))), "Participante duplicado.");
        }
        if (f instanceof FormatoFases fases) {
            JSONObject grupos = j.getJSONObject("grupos");
            JSONArray orden = j.getJSONArray("ordenGrupos");
            Map<String, List<String>> mapa = new LinkedHashMap<>();
            for (int i = 0; i < orden.length(); i++) {
                String nombre = orden.getString(i);
                JSONArray ids = grupos.getJSONArray(nombre);
                List<String> lista = new ArrayList<>();
                for (int k = 0; k < ids.length(); k++) {
                    lista.add(ids.getString(k));
                }
                exigir(mapa.putIfAbsent(nombre, lista) == null, "Grupo repetido.");
            }
            exigir(mapa.size() == grupos.length(), "Grupos sin orden completo.");
            if (!mapa.isEmpty()) {
                exigir(fases.restaurarGrupos(mapa, j.getInt("clasificados")), "Grupos inválidos.");
            }
        }
        JSONArray encuentros = j.getJSONArray("encuentros");
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < encuentros.length(); i++) {
            JSONObject e = encuentros.getJSONObject(i);
            String id = e.getString("id");
            exigir(vistos.add(id), "Encuentro repetido.");
            if (f instanceof FormatoFases fases) {
                while (f.buscarEncuentro(id) == null && fases.getRonda() < j.getInt("ronda")) {
                    exigir(fases.avanzar(), "Avance prematuro o cruce inválido.");
                }
            } else if (f.buscarEncuentro(id) == null) {
                exigir(f.registrarEncuentro(id, e.getString("localId"), e.getString("visitanteId")),
                        "Encuentro inválido o duplicado.");
            }
            EncuentroTorneo actual = f.buscarEncuentro(id);
            exigir(actual != null && actual.localId().equals(e.getString("localId"))
                    && actual.visitanteId().equals(e.getString("visitanteId"))
                    && actual.fase().equals(e.getString("fase")) && actual.grupo().equals(e.getString("grupo")),
                    "Cruce no coincide con su fase.");
            exigir(e.isNull("local") == e.isNull("visitante"), "Marcador parcial.");
            if (!e.isNull("local")) {
                String datoRegistrado;
                if (e.isNull("desempate")) {
                    datoRegistrado = null;
                } else {
                    datoRegistrado = e.getString("desempate");
                }
                exigir(f.restaurarMarcador(id, e.getInt("local"), e.getInt("visitante"), datoRegistrado,
                        e.getBoolean("administrativo")), "Resultado de torneo inválido o repetido.");
            }
        }
        if (f instanceof FormatoFases fases) {
            while (fases.getRonda() < j.getInt("ronda")) {
                exigir(fases.avanzar(), "Fase incompleta.");
            }
            if (!j.isNull("campeon")) {
                exigir(fases.avanzar() && j.getString("campeon").equals(fases.getCampeon()),
                        "Campeón no corresponde a resultados completos.");
            }
            exigir(fases.getRonda() == j.getInt("ronda"), "Ronda incoherente.");
        }
        exigir(f.getEncuentros().size() == encuentros.length() && tabla(f).similar(j.getJSONArray("tabla")),
                "Tabla/cruces no coinciden con resultados.");
        JSONObject vinculos = j.getJSONObject("vinculos");
        for (String partido : vinculos.keySet()) {
            f.cargarVinculo(partido, vinculos.getString(partido));
        }
        exigir(new JSONObject(f.getPuntosEquipos()).similar(competencia.getJSONObject("puntos"))
                && competencia.getJSONArray("procesados").length() == f.getPartidosProcesados().size()
                && new HashSet<>(f.getPartidosProcesados())
                        .equals(new HashSet<>(lista(competencia.getJSONArray("procesados")))),
                "Snapshot de puntos/procesados inconsistente.");
        return f;
    }

    private static List<String> lista(JSONArray array) {
        List<String> r = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            r.add(array.getString(i));
        }
        return r;
    }

    static void validarVinculos(EstadoClub c) {
        for (Competencia co : c.getCompetencias().getCompetencias()) {
            if (co.getFormato() instanceof FormatoLiga f) {
                for (var vinculo : f.getVinculos().entrySet()) {
                    Partido p = c.getPartidos().buscarPartido(vinculo.getKey());
                    exigir(p instanceof PartidoCompetencia, "Vínculo con partido inexistente.");
                    PartidoCompetencia pc = (PartidoCompetencia) p;
                    EncuentroTorneo e = f.buscarEncuentro(vinculo.getValue());
                    exigir(e != null && e.completo() && !e.administrativo() && pc.getEncuentroTorneoId().equals(e.id()),
                            "Vínculo de encuentro incoherente.");
                    boolean local = e.localId().equals(pc.getEquipoClub().getId());
                    String localId;
                    String visitanteId;
                    Integer marcadorLocal;
                    Integer marcadorVisitante;
                    if (local) {
                        localId = pc.getEquipoClub().getId();
                        visitanteId = pc.getRivalId();
                        marcadorLocal = pc.getMarcadorClub();
                        marcadorVisitante = pc.getMarcadorRival();
                    } else {
                        localId = pc.getRivalId();
                        visitanteId = pc.getEquipoClub().getId();
                        marcadorLocal = pc.getMarcadorRival();
                        marcadorVisitante = pc.getMarcadorClub();
                    }
                    exigir(pc.getCompetencia() == co && pc.tieneResultado() && !pc.estaCancelado()
                            && (pc.estaCerrado() || pc.esLegado()) && e.localId().equals(localId)
                            && e.visitanteId().equals(visitanteId) && e.local().equals(marcadorLocal)
                            && e.visitante().equals(marcadorVisitante)
                            && Objects.equals(e.ganadorDesempate(), pc.getGanadorDesempateId()),
                            "Resultado de tabla difiere del acta vinculada.");
                }
            }
        }
    }
}
