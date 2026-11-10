package modelo.usuarios;

public class Empleado {

    private final Usuario usuario;
    private final String codigoDescuento;

    public Empleado(Usuario usuario) {
        if (
            usuario == null ||
            usuario.getId() == null ||
            usuario.getId().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El empleado debe tener un usuario con ID válido."
            );
        }

        this.usuario = usuario;
        this.codigoDescuento = "EMP-" + usuario.getId();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getId() {
        return usuario.getId();
    }

    public String getCodigoDescuento() {
        return codigoDescuento;
    }
}
