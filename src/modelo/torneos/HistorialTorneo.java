package modelo.torneos;

import java.util.List;
import modelo.competencias.EncuentroTorneo;
import modelo.competencias.FormatoLiga;
import modelo.competencias.ParticipanteCompetencia;

public record HistorialTorneo(
    String torneoId,
    String nombre,
    EstadoTorneo estado,
    List<ParticipanteCompetencia> participantes,
    List<EncuentroTorneo> encuentros,
    List<FormatoLiga.Fila> posiciones,
    String ganadorId,
    String mensaje
) {
    public HistorialTorneo {
        participantes = participantes == null
            ? List.of()
            : List.copyOf(participantes);
        encuentros = encuentros == null ? List.of() : List.copyOf(encuentros);
        posiciones = posiciones == null ? List.of() : List.copyOf(posiciones);
    }
}
