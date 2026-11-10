package modelo.competencias;

public record ParticipanteCompetencia(String id, String nombre, String equipoClubId, boolean activo) {
    public ParticipanteCompetencia {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()
                || (equipoClubId != null && !equipoClubId.equals(id))) {
            throw new IllegalArgumentException("Participante inválido.");
        }
    }
}
