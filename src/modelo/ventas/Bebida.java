package modelo.ventas;

public class Bebida extends Producto {

    private boolean esCaliente;

    public Bebida(
        String id,
        String nombre,
        String categoria,
        double precio,
        boolean esCaliente
    ) {
        super(id, nombre, categoria, precio, "CAFETERIA");
        this.esCaliente = esCaliente;
    }

    public boolean esCaliente() {
        return esCaliente;
    }
}
