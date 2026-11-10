package pruebas;

import gestores.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import modelo.deportes.Deporte;
import modelo.reservas.Instalacion;
import modelo.usuarios.Usuario;
import modelo.ventas.*;

public class PruebaReportes {

    public static void main(String[] args) {
        System.out.println(
            "=================================================="
        );
        System.out.println(
            "   INICIANDO PRUEBAS DEL MÓDULO DE REPORTES       "
        );
        System.out.println(
            "=================================================="
        );

        aplicacion.EstadoClub estado = new aplicacion.EstadoClub();
        Inventario inventario = estado.getInventario();
        gestores.GestorAcceso.Sesion operador = SoporteComercial.administrador(
            estado
        );
        GestorVentas gestorVentas = estado.getVentas();
        GestorSocios gestorSocios = new GestorSocios();
        GestorPartidos gestorPartidos = new GestorPartidos();
        GestorEntrenamientos gestorEntrenamientos = new GestorEntrenamientos();
        GestorReservas gestorReservas = new GestorReservas();

        GestorReportes gestorReportes = new GestorReportes(
            gestorVentas,
            gestorSocios,
            gestorPartidos,
            gestorEntrenamientos,
            gestorReservas
        );

        LocalDate inicioPeriodo = LocalDate.of(2026, 1, 1);
        LocalDate finPeriodo = LocalDate.of(2026, 12, 31);

        try {
            Articulo itemPrueba = new Articulo(
                "A-01",
                "Balon",
                "Deportes",
                50000.0
            );
            inventario.registrarProducto(itemPrueba, 5);
            estado.registrarUsuario("U-REP-01", "Comprador de prueba");
            Usuario comprador = estado.buscarUsuario("U-REP-01");

            DetalleVenta detalle = new DetalleVenta(itemPrueba, 1);
            boolean registrada = gestorVentas.procesarVenta(
                operador,
                "V-01",
                comprador,
                "TIENDA",
                LocalDate.of(2026, 3, 15),
                new ArrayList<>(java.util.List.of(detalle)),
                null,
                0.0
            );

            if (!registrada) {
                throw new IllegalStateException(
                    "No se pudo registrar la venta de prueba."
                );
            }

            System.out.println("[Datos Mock] Venta registrada correctamente.");
        } catch (Exception e) {
            System.out.println("[Aviso Venta] " + e.getMessage());
        }

        try {
            Deporte tenis = new Deporte("D01", "Tenis", false);
            Instalacion canchaTenis = new Instalacion(
                "I01",
                "Cancha 1",
                tenis,
                4
            );
            gestorReservas.registrarInstalacion(canchaTenis);

            boolean resExitosa = gestorReservas.realizarReserva(
                "RES-01",
                null,
                canchaTenis,
                LocalDateTime.of(2026, 3, 10, 10, 0),
                LocalDateTime.of(2026, 3, 10, 12, 0),
                2,
                "Sencillos"
            );

            if (resExitosa) {
                System.out.println(
                    "[Datos Mock] Reserva de instalación registrada correctamente."
                );
            }
        } catch (Exception e) {
            System.out.println("[Aviso Reserva] " + e.getMessage());
        }

        System.out.println("\n----------------------------------");
        System.out.println("EJECUTANDO CONSULTAS DE REPORTES:");
        System.out.println("----------------------------------");

        double totalTienda = gestorReportes.consultarReporteFinancieroVentas(
            "TIENDA",
            inicioPeriodo,
            finPeriodo
        );
        System.out.println(
            "-> Total Reporte Financiero [TIENDA]: $" + totalTienda
        );

        var historialReservas = gestorReportes.consultarHistorialReservas(
            inicioPeriodo,
            finPeriodo
        );
        System.out.println(
            "-> Historial de Reservas encontradas en el periodo: " +
                historialReservas.size()
        );

        var historialPartidos = gestorReportes.consultarHistorialPartidos(
            inicioPeriodo,
            finPeriodo
        );
        System.out.println(
            "-> Historial de Partidos encontrados en el periodo: " +
                historialPartidos.size()
        );

        System.out.println(
            "\n=================================================="
        );
        System.out.println(
            "   ¡PRUEBAS DE REPORTES FINALIZADAS CON ÉXITO!    "
        );
        System.out.println(
            "=================================================="
        );
    }
}
