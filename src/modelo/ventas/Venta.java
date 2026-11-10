package modelo.ventas;

import java.time.LocalDate;
import java.util.*;
import modelo.usuarios.Usuario;

public class Venta {

    private final String idVenta, rubro, ubicacion, codigoDescuento, bonoId;
    private final LocalDate fecha;
    private final List<DetalleVenta> detalles;
    private final Usuario comprador;
    private final double subtotal, descuento, impuesto, propina, valorFinalPagado;
    private final long puntosRedimidos, puntosGanados;

    public Venta(String id, Usuario comprador, String rubro, LocalDate fecha, List<DetalleVenta> detalles, double total,
            double propina) {
        this(id, comprador, rubro, fecha, detalles, Inventario.PRINCIPAL, null,
                Dinero.redondear(detalles.stream().mapToDouble(DetalleVenta::getSubtotal).sum()),
                Dinero.redondear(detalles.stream().mapToDouble(DetalleVenta::getSubtotal).sum()
                        - baseLegada(rubro, total, propina)),
                Dinero.redondear(total - propina - baseLegada(rubro, total, propina)), propina, 0, 0);
    }

    public Venta(String id, Usuario comprador, String rubro, LocalDate fecha, List<DetalleVenta> detalles,
            String ubicacion, String codigo, double subtotal, double descuento, double impuesto, double propina,
            long redimidos, long ganados) {
        this(id, comprador, rubro, fecha, detalles, ubicacion, codigo, null, subtotal, descuento, impuesto, propina,
                redimidos, ganados);
    }

    public Venta(String id, Usuario comprador, String rubro, LocalDate fecha, List<DetalleVenta> detalles,
            String ubicacion, String codigo, String bonoId, double subtotal, double descuento, double impuesto,
            double propina, long redimidos, long ganados) {
        this.idVenta = id;
        this.bonoId = bonoId;
        this.comprador = comprador;
        this.rubro = rubro;
        this.fecha = fecha;
        this.detalles = List.copyOf(detalles);
        this.ubicacion = ubicacion;
        this.codigoDescuento = codigo;
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.impuesto = impuesto;
        this.propina = propina;
        this.puntosRedimidos = redimidos;
        this.puntosGanados = ganados;
        this.valorFinalPagado = Dinero.redondear(getBaseImponible() + impuesto - redimidos + propina);
    }

    private static double baseLegada(String rubro, double total, double propina) {
        double factorImpuesto;
        if ("TIENDA".equalsIgnoreCase(rubro)) {
            factorImpuesto = 1.19;
        } else {
            factorImpuesto = 1.08;
        }
        return Dinero.redondear((total - propina) / (factorImpuesto));
    }

    public String getIdVenta() {
        return idVenta;
    }

    public Usuario getComprador() {
        return comprador;
    }

    public String getRubro() {
        return rubro;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getCodigoDescuento() {
        return codigoDescuento;
    }

    public String getBonoId() {
        return bonoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public List<DetalleVenta> getDetalles() {
        return new ArrayList<>(detalles);
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDescuento() {
        return descuento;
    }

    public double getBaseImponible() {
        return Dinero.redondear(subtotal - descuento);
    }

    public double getImpuesto() {
        return impuesto;
    }

    public double getPropinaSugerida() {
        return Dinero.porcentaje(getBaseImponible(), "0.10");
    }

    public long getPuntosRedimidos() {
        return puntosRedimidos;
    }

    public long getPuntosGanados() {
        return puntosGanados;
    }

    public double getValorFinalPagado() {
        return valorFinalPagado;
    }

    public double getPropina() {
        return propina;
    }
}
