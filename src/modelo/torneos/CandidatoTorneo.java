package modelo.torneos;

import java.util.List;
import java.util.Objects;

public record CandidatoTorneo(
    String id,
    String nombre,
    List<String> jugadores,
    String equipoId,
    double puntaje
) {
    public CandidatoTorneo {
        if (
            id == null ||
            id.isBlank() ||
            nombre == null ||
            nombre.isBlank() ||
            jugadores == null ||
            jugadores.isEmpty() ||
            jugadores.stream().anyMatch(j -> j == null || j.isBlank()) ||
            jugadores.stream().distinct().count() != jugadores.size() ||
            !Double.isFinite(puntaje) ||
            puntaje < 0
        ) {
            throw new IllegalArgumentException(
                "Participante o puntaje inválidos."
            );
        }
        jugadores = List.copyOf(jugadores);
        if (equipoId != null && !Objects.equals(id, equipoId)) {
            throw new IllegalArgumentException(
                "El identificador del equipo no coincide."
            );
        }
    }
}
