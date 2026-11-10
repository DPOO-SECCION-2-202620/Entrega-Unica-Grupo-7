package modelo.administracion;

import java.time.*;

public record Turno(String id, String empleadoId, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
    public Turno {
        if (id == null || id.isBlank() || empleadoId == null || empleadoId.isBlank() || dia == null || inicio == null
                || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("Turno inválido; divida los turnos nocturnos por día.");
        }
    }
}
