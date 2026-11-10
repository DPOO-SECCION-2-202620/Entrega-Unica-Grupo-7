package modelo.reservas;

import java.time.LocalDateTime;

public record CambioAgenda(String instalacionAnteriorId, LocalDateTime inicioAnterior, LocalDateTime finAnterior,
        String instalacionNuevaId, LocalDateTime inicioNuevo, LocalDateTime finNuevo, String motivo,
        boolean cancelacion) {
    public CambioAgenda {
        if (inicioAnterior == null || finAnterior == null || inicioNuevo == null || finNuevo == null || motivo == null
                || motivo.isBlank() || finAnterior.isBefore(inicioAnterior) || finNuevo.isBefore(inicioNuevo)) {
            throw new IllegalArgumentException("Cambio de agenda inválido.");
        }
    }
}
