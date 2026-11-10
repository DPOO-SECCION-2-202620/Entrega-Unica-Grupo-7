package modelo.administracion;

import java.time.*;

public record HorarioAtencion(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
    public HorarioAtencion {
        if (dia == null || inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("Horario de atención inválido.");
        }
    }
}
