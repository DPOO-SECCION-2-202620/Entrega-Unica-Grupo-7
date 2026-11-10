package modelo.ventas;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BonoDescuento {

    private final String id;
    private final String titularId;
    private final String torneoId;
    private final BigDecimal porcentaje;
    private final LocalDate fechaEmision;
    private boolean usado;
    private String ventaUso;

    public BonoDescuento(
        String id,
        String titularId,
        String torneoId,
        BigDecimal porcentaje,
        LocalDate fechaEmision
    ) {
        if (
            id == null ||
            id.isBlank() ||
            titularId == null ||
            titularId.isBlank() ||
            torneoId == null ||
            torneoId.isBlank() ||
            porcentaje == null ||
            fechaEmision == null
        ) {
            throw new IllegalArgumentException("Datos del bono incompletos.");
        }
        this.id = id;
        this.titularId = titularId;
        this.torneoId = torneoId;
        this.porcentaje = porcentaje;
        this.fechaEmision = fechaEmision;
    }

    public String getId() {
        return id;
    }

    public String getTitularId() {
        return titularId;
    }

    public String getTorneoId() {
        return torneoId;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public boolean isUsado() {
        return usado;
    }

    public String getVentaUso() {
        return ventaUso;
    }

    public void registrarUso(String ventaId){
        if (usado) {
            throw new IllegalArgumentException("El bono ya ha sido utilizado");
        }
        if (ventaId == null || ventaId.isBlank()) {
            throw new IllegalArgumentException("Se necesita la venta en la que se usa el bono");
        }
        usado = true;
        ventaUso = ventaId;
    }
}
