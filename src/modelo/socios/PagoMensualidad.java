package modelo.socios;

import java.time.LocalDate;
import modelo.usuarios.Jugador;

public class PagoMensualidad {

    private String idPago;
    private Jugador jugador;
    private double importeRecibido;
    private LocalDate fechaPago;
    private String periodo;

    public PagoMensualidad(
        String idPago,
        Jugador jugador,
        double importeRecibido,
        LocalDate fechaPago,
        String periodo
    ) {
        this.idPago = idPago;
        this.jugador = jugador;
        this.importeRecibido = importeRecibido;
        this.fechaPago = fechaPago;
        this.periodo = periodo;
    }

    public String getIdPago() {
        return idPago;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public double getImporteRecibido() {
        return importeRecibido;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public String getPeriodo() {
        return periodo;
    }
}
