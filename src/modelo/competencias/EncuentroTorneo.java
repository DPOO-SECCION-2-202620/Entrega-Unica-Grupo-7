package modelo.competencias;

public record EncuentroTorneo(String id, String localId, String visitanteId, String fase, String grupo, Integer local,
        Integer visitante, String ganadorDesempate, boolean administrativo) {
    public EncuentroTorneo {
        if (id == null || id.isBlank() || localId == null || visitanteId == null || localId.equals(visitanteId)
                || fase == null || grupo == null || (local == null) != (visitante == null)) {
            throw new IllegalArgumentException("Encuentro de torneo inválido.");
        }
    }

    public boolean completo() {
        return local != null;
    }

    public String ganador() {
        if (!completo()) {
            return null;
        } else if (local > visitante) {
            return localId;
        } else if (visitante > local) {
            return visitanteId;
        } else {
            return ganadorDesempate;
        }
    }
}
