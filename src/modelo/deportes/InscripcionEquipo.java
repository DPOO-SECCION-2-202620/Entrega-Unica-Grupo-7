package modelo.deportes;

import modelo.usuarios.Jugador;

public class InscripcionEquipo {

    private boolean activa = true;
    private String motivoRetiro = "";
    private Jugador jugador;
    private Equipo equipo;
    private String posicion;
    private int numeroCamiseta;

    public InscripcionEquipo(Jugador jugador, Equipo equipo, String posicion, int numeroCamiseta) {
        super();
        this.jugador = jugador;
        this.equipo = equipo;
        this.posicion = posicion;
        this.numeroCamiseta = numeroCamiseta;
    }

    public boolean estaActiva() {
        return activa;
    }

    public String getMotivoRetiro() {
        return motivoRetiro;
    }

    public boolean retirar(String motivo) {
        if (!activa || motivo == null || motivo.isBlank()) {
            return false;
        }
        activa = false;
        motivoRetiro = motivo;
        return true;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public String getPosicion() {
        return posicion;
    }

    public int getNumeroCamiseta() {
        return numeroCamiseta;
    }
}
