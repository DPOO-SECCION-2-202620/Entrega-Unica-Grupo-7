package pruebas;

import aplicacion.EstadoClub;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import modelo.usuarios.Usuario;
import modelo.ventas.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaVentasJson {

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: crear|verificar archivo.json");
        }
        Path archivo = Path.of(args[1]);
        PersistenciaClubJson p = new PersistenciaClubJson();
        LocalDate fecha = LocalDate.of(2026, 10, 19);
        if (args[0].equals("crear")) {
            EstadoClub c = new EstadoClub();
            gestores.GestorAcceso.Sesion operador = SoporteComercial.administrador(c);
            c.registrarJugador("J-COMP-01", "Comprador de prueba", LocalDate.of(2000, 1, 1));

            Usuario comprador = c.buscarJugador("J-COMP-01").getUsuario();
            Articulo balon = new Articulo("A1", "Balón", "Deportes", 100);
            Bebida cafe = new Bebida("B1", "Café", "Bebidas", 4000, true);
            Snack snack = new Snack("S1", "Sándwich", "Comida", 12000, List.of("gluten"));
            c.getInventario().registrarProducto(balon, 10);
            c.getInventario().registrarProducto(cafe, 10);
            c.getInventario().registrarProducto(snack, 5);
            boolean rechazo = false;
            try {
                new DetalleVenta(balon, -1);
            } catch (IllegalArgumentException e) {
                rechazo = true;
            }
            verificar(rechazo && c.getInventario().consultarStock(balon) == 10,
                    "Cantidad negativa rechazada sin cambiar existencias");
            verificar(!c.getVentas().procesarVenta(operador, "X", comprador, "TIENDA", fecha,
                    List.of(new DetalleVenta(balon, 6), new DetalleVenta(balon, 6)), null, 0)
                    && c.getInventario().consultarStock(balon) == 10, "Líneas repetidas no pueden superar existencias");
            verificar(!c.getVentas().procesarVenta(operador, "X", comprador, "TIENDA", fecha,
                    List.of(new DetalleVenta(cafe, 1)), null, 0), "Rubro incompatible rechazado");
            verificar(!c.getVentas().procesarVenta(operador, "X", comprador, "TIENDA", fecha,
                    List.of(new DetalleVenta(balon, 1)), null, -1), "Propina negativa rechazada");
            verificar(!c.getVentas().procesarVenta(operador, "X", comprador, "TIENDA", fecha, List.of(), null, 0),
                    "Venta vacía rechazada");
            verificar(
                    c.getVentas().procesarVenta(operador, "V1", comprador, "TIENDA", fecha,
                            List.of(new DetalleVenta(balon, 2), new DetalleVenta(balon, 3)), null, 0),
                    "Venta de tienda con dos líneas del mismo producto");
            verificar(
                    c.getInventario().consultarStock(balon) == 5
                            && Math.abs(c.getVentas().buscarVenta("V1").getValorFinalPagado() - 595) < 0.001,
                    "Stock y total con impuesto de tienda correctos");
            verificar(!c.getVentas().procesarVenta(operador, "V1", comprador, "TIENDA", fecha,
                    List.of(new DetalleVenta(balon, 1)), null, 0) && c.getInventario().consultarStock(balon) == 5,
                    "Venta duplicada rechazada");
            verificar(
                    c.getVentas().procesarVenta(operador, "V2", comprador, "CAFETERIA", fecha,
                            List.of(new DetalleVenta(cafe, 2), new DetalleVenta(snack, 1)), null, 2000),
                    "Venta de cafetería con propina");
            verificar(c.getVentas().buscarVenta("V2").getValorFinalPagado() == 23600, "Total cafetería correcto");
            c.getInventario().reabastecer(balon, 3);
            verificar(c.getInventario().consultarStock(balon) == 8, "Reabastecimiento");
            p.salvar(archivo, c);
        } else if (args[0].equals("verificar")) {
            EstadoClub c = p.cargar(archivo);
            verificar(c.getInventario().getProductos().size() == 3, "Productos recuperados en otra JVM");
            verificar(c.getVentas().getVentasRegistradas().size() == 2, "Ventas recuperadas");
            Producto balon = c.getInventario().buscarProducto("A1");
            verificar(
                    c.getInventario().consultarStock(balon) == 8
                            && c.getInventario().consultarStock(c.getInventario().buscarProducto("B1")) == 8
                            && c.getInventario().consultarStock(c.getInventario().buscarProducto("S1")) == 4,
                    "Cargar ventas no descuenta existencias otra vez");
            verificar(c.getVentas().buscarVenta("V1").getDetalles().get(0).getProducto() == balon,
                    "Detalle e inventario comparten el producto");
            verificar(
                    c.getVentas().buscarVenta("V2").getPropina() == 2000
                            && c.getVentas().buscarVenta("V2").getValorFinalPagado() == 23600,
                    "Importes y propina conservados");
            verificar(
                    ((Bebida) c.getInventario().buscarProducto("B1")).esCaliente()
                            && ((Snack) c.getInventario().buscarProducto("S1")).getAlergenos().contains("gluten"),
                    "Tipos y datos específicos conservados");
            p.salvar(archivo, c);
            EstadoClub otra = p.cargar(archivo);
            verificar(otra.getInventario().consultarStock(otra.getInventario().buscarProducto("A1")) == 8,
                    "Guardar y cargar nuevamente mantiene stock");
            JSONObject raiz = new JSONObject(Files.readString(archivo));
            raiz.getJSONArray("ventas").getJSONObject(0).getJSONArray("detalles").getJSONObject(0).put("productoId",
                    "NO_EXISTE");
            Path invalido = archivo.resolveSibling("venta-invalida.json");
            Files.writeString(invalido, raiz.toString());
            String antes = Files.readString(invalido);
            boolean rechazo = false;
            try {
                p.cargar(invalido);
            } catch (IOException e) {
                rechazo = true;
            }
            verificar(rechazo && antes.equals(Files.readString(invalido)),
                    "Venta con producto inexistente rechazada sin sobrescribir");
        } else {
            throw new IllegalArgumentException("Modo desconocido");
        }
    }
}
