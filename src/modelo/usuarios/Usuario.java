package modelo.usuarios;

public class Usuario {

    private final String id;
    private final boolean identificado;
    private String nombre;
    private boolean activo = true;

    public Usuario(String id, String nombre, String login, String password) {
        this(id, nombre, login, password, true);
    }

    public Usuario(
        String id,
        String nombre,
        String login,
        String password,
        boolean identificado
    ) {
        this.identificado = identificado;
        this.id = id;
        this.nombre = nombre;
    }

    public boolean estaActivo() {
        return activo;
    }

    public void restaurarActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean esIdentificado() {
        return identificado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getId() {
        return id;
    }
}
