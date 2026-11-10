package pruebas;

import gestores.GestorVentas;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import modelo.usuarios.Usuario;
import modelo.ventas.*;

public class PruebaVentas {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   INICIANDO PRUEBAS DEL MÓDULO DE VENTAS         ");
        System.out.println("==================================================");

        aplicacion.EstadoClub estado = new aplicacion.EstadoClub();
        Inventario inventario = estado.getInventario();
        gestores.GestorAcceso.Sesion operador = SoporteComercial.administrador(estado);

        Articulo raqueta = new Articulo("ART-01", "Raqueta Tenis Wilson", "Raquetas", 250000.0);
        Bebida cafe = new Bebida("BEB-01", "Café Americano", "Bebidas Calientes", 4000.0, true);
        Snack sandwich = new Snack("SNA-01", "Sándwich de Pollo", "Snacks", 12000.0,
                Arrays.asList("gluten", "lácteos"));

        inventario.registrarProducto(raqueta, 10);
        inventario.registrarProducto(cafe, 50);
        inventario.registrarProducto(sandwich, 20);

        estado.registrarEmpleado("U-EMP-01", "Carlos");
        Usuario compradorEmpleado = estado.buscarUsuario("U-EMP-01");

        estado.registrarUsuario("U-SOC-01", "Ana");
        Usuario compradorSocio = estado.buscarUsuario("U-SOC-01");

        System.out.println("[OK] Productos registrados en el inventario.");

        GestorVentas gestorVentas = estado.getVentas();

        System.out.println("\n----------------------------------");
        System.out.println("PRUEBA 1: Compra en Tienda (Empleado)");
        System.out.println("----------------------------------");

        DetalleVenta detalleTienda = new DetalleVenta(raqueta, 1);
        List<DetalleVenta> carritoTienda = Arrays.asList(detalleTienda);

        LocalDate fechaHoy = LocalDate.now();

        boolean ventaTiendaExitosa = gestorVentas.procesarVenta(operador, "V-001", compradorEmpleado, "TIENDA",
                fechaHoy, carritoTienda, new modelo.usuarios.Empleado(compradorEmpleado).getCodigoDescuento(), 0.0);

        if (!ventaTiendaExitosa || Math.abs(gestorVentas.buscarVenta("V-001").getValorFinalPagado() - 252875) > 0.001
                || inventario.consultarStock(raqueta) != 9) {
            throw new AssertionError("Venta de tienda incorrecta");
        }

        if (ventaTiendaExitosa) {
            System.out.println("-> ¡Venta de tienda procesada con éxito!");
            System.out.println("Comprador de V-001: " + gestorVentas.buscarVenta("V-001").getComprador().getNombre());
            System.out.println("-> Stock restante de raquetas: " + inventario.consultarStock(raqueta));
        } else {
            System.out.println("-> Error: No se pudo procesar la venta de tienda.");
        }

        System.out.println("\n----------------------------------");
        System.out.println("PRUEBA 2: Compra en Cafetería (Socio con Propina)");
        System.out.println("----------------------------------");

        DetalleVenta detalleCafe = new DetalleVenta(cafe, 2);
        DetalleVenta detalleSnack = new DetalleVenta(sandwich, 1);
        List<DetalleVenta> carritoCafeteria = Arrays.asList(detalleCafe, detalleSnack);

        boolean ventaCafeExitosa = gestorVentas.procesarVenta(operador, "V-002", compradorSocio, "CAFETERIA", fechaHoy,
                carritoCafeteria, null, 2000.0);

        if (!ventaCafeExitosa || gestorVentas.buscarVenta("V-002").getValorFinalPagado() != 23600
                || inventario.consultarStock(cafe) != 48 || inventario.consultarStock(sandwich) != 19) {
            throw new AssertionError("Venta de cafetería incorrecta");
        }

        if (ventaCafeExitosa) {
            System.out.println("-> ¡Venta de cafetería procesada con éxito!");
            System.out.println("-> Stock restante de café: " + inventario.consultarStock(cafe));
            System.out.println("-> Stock restante de sándwiches: " + inventario.consultarStock(sandwich));
        } else {
            System.out.println("-> Error: No se pudo procesar la venta de cafetería.");
        }

        System.out.println("\n----------------------------------");
        System.out.println("PRUEBA 3: Consulta de Reporte Financiero");
        System.out.println("----------------------------------");

        LocalDate inicioPeriodo = fechaHoy.minusDays(1);
        LocalDate finPeriodo = fechaHoy.plusDays(1);

        double totalTienda = gestorVentas.consultarVentasPorRubroYPeriodo("TIENDA", inicioPeriodo, finPeriodo);
        double totalCafeteria = gestorVentas.consultarVentasPorRubroYPeriodo("CAFETERIA", inicioPeriodo, finPeriodo);

        System.out.println("-> Total recaudado en TIENDA: $" + totalTienda);
        System.out.println("-> Total recaudado en CAFETERIA: $" + totalCafeteria);

        System.out.println("\n==================================================");
        System.out.println("   ¡PRUEBAS DE VENTAS FINALIZADAS CON ÉXITO!      ");
        System.out.println("==================================================");
    }
}
