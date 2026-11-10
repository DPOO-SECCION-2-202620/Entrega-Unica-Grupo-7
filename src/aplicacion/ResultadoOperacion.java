package aplicacion;

public record ResultadoOperacion(boolean exito, String mensaje) {
    public static ResultadoOperacion correcto(String mensaje) {
        return new ResultadoOperacion(true, mensaje);
    }

    public static ResultadoOperacion rechazo(String mensaje) {
        return new ResultadoOperacion(false, mensaje);
    }
}
