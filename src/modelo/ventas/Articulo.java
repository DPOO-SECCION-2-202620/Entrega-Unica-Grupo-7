package modelo.ventas;

public class Articulo extends Producto {

    public Articulo(String id, String nombre, String categoria, double precio) {
        super(id, nombre, categoria, precio, "TIENDA");
    }
}
