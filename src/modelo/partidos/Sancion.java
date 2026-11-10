package modelo.partidos;

import modelo.usuarios.Jugador;

public class Sancion {

    private Jugador jugador;
    private Partido partido;
    private TipoTarjeta tipo;

    public Sancion(Jugador jugador, Partido partido, TipoTarjeta tipo) {
        super();
        this.jugador = jugador;
        this.partido = partido;
        this.tipo = tipo;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Partido getPartido() {
        return partido;
    }

    public TipoTarjeta getTipo() {
        return tipo;
    }
}
