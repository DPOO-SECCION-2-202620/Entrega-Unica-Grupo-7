package gestores;

import java.util.*;
import modelo.competencias.*;

public class GestorCompetencias {

    private final Map<String, Competencia> competencias = new LinkedHashMap<>();
    private final Map<String, NominaCompetencia> nominas = new LinkedHashMap<>();

    public Competencia buscarCompetencia(String id) {
        return competencias.get(id);
    }

    public List<Competencia> getCompetencias() {
        return List.copyOf(competencias.values());
    }

    public List<NominaCompetencia> getNominas() {
        return List.copyOf(nominas.values());
    }

    private String clave(String competencia, String equipo) {
        return competencia + "\0" + equipo;
    }

    public NominaCompetencia buscarNomina(String competencia, String equipo) {
        return nominas.get(clave(competencia, equipo));
    }

    public boolean registrarCompetencia(Competencia c) {
        if (c == null || c.getId() == null || c.getId().isBlank() || c.getNombre() == null || c.getNombre().isBlank()
                || c.getDeporte() == null || c.getFormato() == null || competencias.containsKey(c.getId())) {
            return false;
        }
        competencias.put(c.getId(), c);
        return true;
    }

    public boolean registrarNomina(NominaCompetencia n) {
        return agregarNomina(n, false);
    }

    public boolean cargarNomina(NominaCompetencia n) {
        return agregarNomina(n, true);
    }

    private boolean agregarNomina(NominaCompetencia n, boolean historica) {
        if (n == null || n.getCompetencia() == null || n.getEquipo() == null
                || competencias.get(n.getCompetencia().getId()) != n.getCompetencia()
                || !n.getEquipo().getDeporte().getId().equals(n.getCompetencia().getDeporte().getId())
                || !n.getEquipo().getCategoria().equals(n.getCompetencia().getCategoria())
                || buscarNomina(n.getCompetencia().getId(), n.getEquipo().getId()) != null) {
            return false;
        }

        if (!historica && !n.cumpleMinimo()) {
            return false;
        }

        if (!historica && n.getCompetencia().getFormato() instanceof FormatoLiga f) {
            ParticipanteCompetencia existente = f.buscarParticipante(n.getEquipo().getId());
            if (existente != null && !n.getEquipo().getId().equals(existente.equipoClubId())) {
                return false;
            }

            if (existente == null && !f.registrarParticipante(new ParticipanteCompetencia(n.getEquipo().getId(),
                    n.getEquipo().getNombre(), n.getEquipo().getId(), true))) {
                return false;
            }
        }
        nominas.put(clave(n.getCompetencia().getId(), n.getEquipo().getId()), n);
        return true;
    }
}
