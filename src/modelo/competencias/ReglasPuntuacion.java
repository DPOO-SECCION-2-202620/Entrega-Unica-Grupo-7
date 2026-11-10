package modelo.competencias;

public record ReglasPuntuacion(int victoria, int empate, int derrota, boolean ajustadaPorSets) {
    public ReglasPuntuacion {
        if (victoria < 0 || empate < 0 || derrota < 0 || victoria < derrota) {
            throw new IllegalArgumentException("Puntuación inválida.");
        }
    }
}
