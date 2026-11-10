package modelo.torneos;

public record InscripcionTorneo(
    String id,
    CandidatoTorneo participante,
    TipoCupo cupo,
    boolean activa
) {
    public InscripcionTorneo {
        if (
            id == null || id.isBlank() || participante == null || cupo == null
        ) {
            throw new IllegalArgumentException("Inscripción inválida.");
        }
    }
}
