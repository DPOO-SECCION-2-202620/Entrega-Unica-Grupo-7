package modelo.deportes;

public class Deporte {

    private final String id;
    private String nombre;
    private boolean colectivo;

    public Deporte(String id, String nombre, boolean colectivo) {
        super();
        this.id = id;
        this.nombre = nombre;
        this.colectivo = colectivo;
    }

    public static String clave(String valor) {
        if (valor == null) {
            return null;
        }
        String normal = java.text.Normalizer.normalize(valor.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(java.util.Locale.ROOT);
        if (normal.equals("TENIS DE MESA")) {
            return "MESA";
        } else {
            return normal;
        }
    }

    public boolean es(String clave) {
        return (java.util.Objects.equals(Deporte.clave(id), Deporte.clave(clave))
                || java.util.Objects.equals(Deporte.clave(nombre), Deporte.clave(clave)));
    }

    public int getMinimoJugadores() {
        if (es("FUTBOL")) {
            return 7;
        } else if (es("BALONCESTO")) {
            return 5;
        } else if (es("VOLEIBOL")) {
            return 6;
        } else {
            return 0;
        }
    }

    public boolean marcadorValido(int local, int visitante) {
        if (local < 0 || visitante < 0) {
            return false;
        }
        return (!es("BALONCESTO") && !es("VOLEIBOL")) || local != visitante;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isColectivo() {
        return colectivo;
    }
}
