package modelo.ventas;

import java.util.*;

public class Inventario {

    public static final String PRINCIPAL = "PRINCIPAL";
    private final Map<String, Producto> catalogo = new LinkedHashMap<>();
    private final Map<String, Map<Producto, Integer>> ubicaciones = new LinkedHashMap<>();

    public Inventario() {
        ubicaciones.put(PRINCIPAL, new LinkedHashMap<>());
    }

    public void registrarUbicacion(String id) {
        if (id == null || id.isBlank() || ubicaciones.containsKey(id)) {
            throw new IllegalArgumentException("Ubicación inválida o duplicada.");
        }
        ubicaciones.put(id, new LinkedHashMap<>());
    }

    public List<String> getUbicaciones() {
        return new ArrayList<>(ubicaciones.keySet());
    }

    public void registrarProducto(Producto p, int cantidad) {
        registrarProducto(PRINCIPAL, p, cantidad);
    }

    public void registrarProducto(String ubicacion, Producto p, int cantidad) {
        Map<Producto, Integer> stock = ubicaciones.get(ubicacion);
        if (stock == null || p == null || p.getId() == null || p.getId().isBlank() || p.getNombre() == null
                || p.getNombre().isBlank() || !Double.isFinite(p.getPrecio()) || p.getPrecio() <= 0 || cantidad < 0
                || stock.containsKey(p) || (catalogo.containsKey(p.getId()) && catalogo.get(p.getId()) != p)) {
            throw new IllegalArgumentException("Producto, ubicación o cantidad inválidos.");
        }
        catalogo.put(p.getId(), p);
        stock.put(p, cantidad);
    }

    public Producto buscarProductoCatalogo(String id) {
        return catalogo.get(id);
    }

    public Producto buscarProducto(String id) {
        return buscarProducto(PRINCIPAL, id);
    }

    public Producto buscarProducto(String ubicacion, String id) {
        Producto p = catalogo.get(id);
        if (ubicaciones.containsKey(ubicacion) && ubicaciones.get(ubicacion).containsKey(p)) {
            return p;
        } else {
            return null;
        }
    }

    public List<Producto> getProductos() {
        return getProductos(PRINCIPAL);
    }

    public List<Producto> getCatalogo() {
        return new ArrayList<>(catalogo.values());
    }

    public List<Producto> getProductos(String ubicacion) {
        if (!ubicaciones.containsKey(ubicacion)) {
            throw new IllegalArgumentException("Ubicación inexistente.");
        }
        return new ArrayList<>(ubicaciones.get(ubicacion).keySet());
    }

    public boolean trasladarProducto(String id, String origen, String destino) {
        Producto p = buscarProducto(origen, id);
        if (p == null || !ubicaciones.containsKey(destino) || buscarProducto(destino, id) != null) {
            return false;
        }
        int cantidad = ubicaciones.get(origen).get(p);
        ubicaciones.get(destino).put(p, cantidad);
        ubicaciones.get(origen).remove(p);
        return true;
    }

    public void reabastecer(Producto p, int cantidad) {
        reabastecer(PRINCIPAL, p, cantidad);
    }

    public void reabastecer(String ubicacion, Producto p, int cantidad) {
        if (p == null || buscarProducto(ubicacion, p.getId()) != p || cantidad <= 0
                || (long) consultarStock(ubicacion, p) + cantidad > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Reposición inválida.");
        }
        ubicaciones.get(ubicacion).put(p, consultarStock(ubicacion, p) + cantidad);
    }

    public int consultarStock(Producto p) {
        return consultarStock(PRINCIPAL, p);
    }

    public int consultarStock(String ubicacion, Producto p) {
        if (ubicaciones.containsKey(ubicacion)) {
            return ubicaciones.get(ubicacion).getOrDefault(p, 0);
        } else {
            return 0;
        }
    }

    public boolean hayExistencias(Producto p, int cantidad) {
        return hayExistencias(PRINCIPAL, p, cantidad);
    }

    public boolean hayExistencias(String ubicacion, Producto p, int cantidad) {
        return (p != null && buscarProducto(ubicacion, p.getId()) == p && cantidad > 0
                && consultarStock(ubicacion, p) >= cantidad);
    }

    public void reducirStock(Producto p, int cantidad) {
        reducirStock(PRINCIPAL, p, cantidad);
    }

    public void reducirStock(String ubicacion, Producto p, int cantidad) {
        if (!hayExistencias(ubicacion, p, cantidad)) {
            throw new IllegalArgumentException("Existencias insuficientes.");
        }
        ubicaciones.get(ubicacion).put(p, consultarStock(ubicacion, p) - cantidad);
    }
}
