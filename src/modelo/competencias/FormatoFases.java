package modelo.competencias;

import java.util.*;

public class FormatoFases extends FormatoLiga {

    private final Map<String, List<String>> grupos = new LinkedHashMap<>();
    private int clasificadosPorGrupo, ronda;
    private String campeon;

    public Map<String, List<String>> getGrupos() {
        return Collections.unmodifiableMap(grupos);
    }

    public int getClasificadosPorGrupo() {
        return clasificadosPorGrupo;
    }

    public int getRonda() {
        return ronda;
    }

    @Override
    public String getCampeon() {
        return campeon;
    }

    public boolean configurarGrupos(Map<String, List<String>> nuevos, int clasificados) {
        return configurar(nuevos, clasificados, false);
    }

    public boolean restaurarGrupos(Map<String, List<String>> nuevos, int clasificados) {
        return configurar(nuevos, clasificados, true);
    }

    private boolean configurar(Map<String, List<String>> nuevos, int clasificados, boolean recuperar) {
        if (!grupos.isEmpty() || !encuentros.isEmpty() || nuevos == null || nuevos.isEmpty() || clasificados < 1) {
            return false;
        }
        List<String> ids = new ArrayList<>();
        for (var grupo : nuevos.entrySet()) {
            if (grupo.getKey() == null || grupo.getKey().isBlank() || grupo.getValue() == null
                    || grupo.getValue().size() < 2 || grupo.getValue().size() < clasificados) {
                return false;
            }
            ids.addAll(grupo.getValue());
        }
        int total = nuevos.size() * clasificados;
        if (total < 2 || (total & (total - 1)) != 0 || ids.size() != new HashSet<>(ids).size()) {
            return false;
        }

        if (recuperar) {
            if (!participantes.keySet().containsAll(ids)) {
                return false;
            }
        } else {
            Set<String> activos = new HashSet<>(participantes.values().stream().filter(ParticipanteCompetencia::activo)
                    .map(ParticipanteCompetencia::id).toList());
            if (!new HashSet<>(ids).equals(activos)) {
                return false;
            }
        }
        nuevos.forEach((nombre, miembros) -> grupos.put(nombre, List.copyOf(miembros)));
        clasificadosPorGrupo = clasificados;
        int numero = 1;
        for (var g : grupos.entrySet()) {
            for (int i = 0; i < g.getValue().size(); i++) {
                for (int j = i + 1; j < g.getValue().size(); j++) {
                    agregarEncuentro(new EncuentroTorneo("GRUPO-" + numero++, g.getValue().get(i), g.getValue().get(j),
                            "GRUPOS", g.getKey(), null, null, null, false));
                }
            }
        }
        return true;
    }

    @Override
    public boolean registrarParticipante(ParticipanteCompetencia p) {
        return grupos.isEmpty() && super.registrarParticipante(p);
    }

    @Override
    public boolean registrarEncuentro(String id, String local, String visitante) {
        return false;
    }

    public List<Fila> getTablaGrupo(String grupo) {
        if (grupos.containsKey(grupo)) {
            return tabla(encuentros.values().stream().filter(e -> e.grupo().equals(grupo)).toList(), grupos.get(grupo));
        } else {
            return List.of();
        }
    }

    @Override
    public boolean avanzar() {
        if (grupos.isEmpty() || campeon != null) {
            return false;
        }
        List<String> siguientes = new ArrayList<>();
        if (ronda == 0) {
            if (encuentros.values().stream().filter(e -> e.fase().equals("GRUPOS")).anyMatch(e -> !e.completo())) {
                return false;
            }
            for (int puesto = 0; puesto < clasificadosPorGrupo; puesto++) {
                for (String grupo : grupos.keySet()) {
                    siguientes.add(getTablaGrupo(grupo).get(puesto).participanteId());
                }
            }
        } else {
            List<EncuentroTorneo> actuales = encuentros.values().stream()
                    .filter(e -> e.fase().equals("ELIMINACION-" + ronda)).toList();
            if (actuales.isEmpty() || actuales.stream().anyMatch(e -> e.ganador() == null)) {
                return false;
            }
            for (EncuentroTorneo e : actuales) {
                siguientes.add(e.ganador());
            }
            if (siguientes.size() == 1) {
                campeon = siguientes.get(0);
                return true;
            }
        }
        ronda++;
        for (int i = 0; i < siguientes.size() / 2; i++) {
            agregarEncuentro(new EncuentroTorneo("ELIM-" + ronda + "-" + (i + 1), siguientes.get(i),
                    siguientes.get(siguientes.size() - 1 - i), "ELIMINACION-" + ronda, "", null, null, null, false));
        }
        return true;
    }
}
