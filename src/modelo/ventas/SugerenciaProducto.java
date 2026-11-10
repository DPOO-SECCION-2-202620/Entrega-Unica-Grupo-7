package modelo.ventas;

import modelo.usuarios.Empleado;

public class SugerenciaProducto {

    public enum Estado {
        PENDIENTE, APROBADA, RECHAZADA,
    }

    private final String id, ubicacion;
    private final Empleado autor;
    private final Producto producto;
    private final int cantidadInicial;
    private final Estado estado;

    public SugerenciaProducto(String id, Empleado autor, Producto producto, String ubicacion, int cantidadInicial,
            Estado estado) {
        if (id == null || id.isBlank() || autor == null || producto == null || ubicacion == null || ubicacion.isBlank()
                || cantidadInicial <= 0 || estado == null || producto.getId() == null || producto.getId().isBlank()
                || producto.getNombre() == null || producto.getNombre().isBlank() || producto.getCategoria() == null
                || producto.getCategoria().isBlank() || !Double.isFinite(producto.getPrecio())
                || producto.getPrecio() <= 0 || producto.getPrecio() != Dinero.redondear(producto.getPrecio())) {
            throw new IllegalArgumentException("Sugerencia inválida.");
        }
        this.id = id;
        this.autor = autor;
        this.producto = producto;
        this.ubicacion = ubicacion;
        this.cantidadInicial = cantidadInicial;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public Empleado getAutor() {
        return autor;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public Estado getEstado() {
        return estado;
    }
}
