package modelo.entrenamientos;

import modelo.usuarios.Jugador;

public class AsistenciaEvaluacion {

    private Jugador jugador;
    private Entrenamiento entrenamiento;
    private EstadoAsistencia estadoAsistencia;
    private Double calificacion;
    private String comentario;

    public AsistenciaEvaluacion(
        Jugador jugador,
        Entrenamiento entrenamiento,
        EstadoAsistencia estadoAsistencia,
        Double calificacion,
        String comentario
    ) {
        super();
        this.jugador = jugador;
        this.entrenamiento = entrenamiento;
        this.estadoAsistencia = estadoAsistencia;
        this.calificacion = calificacion;
        this.comentario = comentario;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Entrenamiento getEntrenamiento() {
        return entrenamiento;
    }

    public EstadoAsistencia getEstadoAsistencia() {
        return estadoAsistencia;
    }

    public Double getCalificacion() {
        return calificacion;
    }

    public String getComentario() {
        return comentario;
    }
}
