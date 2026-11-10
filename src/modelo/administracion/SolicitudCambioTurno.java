package modelo.administracion;

public record SolicitudCambioTurno(String id, String empleadoId, Turno anterior, Turno propuesto, Estado estado,
        String motivo) {
    public enum Estado {
        PENDIENTE, APROBADA, RECHAZADA,
    }

    public SolicitudCambioTurno {
        if (id == null || id.isBlank() || empleadoId == null || anterior == null || propuesto == null || estado == null
                || !anterior.empleadoId().equals(empleadoId) || !propuesto.empleadoId().equals(empleadoId)
                || !anterior.id().equals(propuesto.id())) {
            throw new IllegalArgumentException("Solicitud inválida.");
        }

        if (motivo == null) {
            motivo = "";
        } else {
            motivo = motivo;
        }
    }
}
