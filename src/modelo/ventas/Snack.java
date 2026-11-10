package modelo.ventas;

import java.util.List;

public class Snack extends Producto {

    private List<String> alergenos;

    public Snack(String id, String nombre, String categoria, double precio, List<String> alergenos) {
        super(id, nombre, categoria, precio, "CAFETERIA");
        if (alergenos != null && alergenos.stream().anyMatch(a -> a == null || a.isBlank())) {
            throw new IllegalArgumentException("La declaración contiene alérgenos vacíos.");
        }

        if (alergenos == null) {
            this.alergenos = null;
        } else {
            this.alergenos = List.copyOf(alergenos);
        }
    }

    public boolean tieneDeclaracionAlergenos() {
        return alergenos != null;
    }

    public List<String> getAlergenos() {
        if (alergenos == null) {
            return null;
        } else {
            return List.copyOf(alergenos);
        }
    }
}
