package modelo.ventas;

public abstract class Producto {

    private String id;
    private String nombre;
    private String categoria;
    private double precio;
    private String rubro;

    public Producto(
        String id,
        String nombre,
        String categoria,
        double precio,
        String rubro
    ) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.rubro = rubro;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public double getPrecio() {
        return precio;
    }

    public String getRubro() {
        return rubro;
    }
}
