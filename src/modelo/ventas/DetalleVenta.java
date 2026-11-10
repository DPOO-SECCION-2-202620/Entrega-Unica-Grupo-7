package modelo.ventas;

public class DetalleVenta {

    private Producto producto;
    private int cantidad;
    private double precioUnitario;

    public DetalleVenta(Producto producto, int cantidad) {
        this(producto, cantidad, precioActual(producto));
    }

    public DetalleVenta(Producto producto, int cantidad, double precioUnitario) {
        if (producto == null || cantidad <= 0 || !Double.isFinite(precioUnitario) || precioUnitario <= 0
                || !Double.isFinite(precioUnitario * cantidad)) {
            throw new IllegalArgumentException("Detalle de venta inválido.");
        }
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    private static double precioActual(Producto producto) {
        if (producto == null) {
            return 0;
        }
        return producto.getPrecio();
    }

    public double getSubtotal() {
        return precioUnitario * cantidad;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }
}
