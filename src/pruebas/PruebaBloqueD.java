package pruebas;

import aplicacion.EstadoClub;
import gestores.GestorVentas;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import modelo.usuarios.*;
import modelo.ventas.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaBloqueD {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 6);

    private static void ok(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static void igual(double observado, double esperado) {
        ok(Math.abs(observado - esperado) < 0.001, "importe " + esperado);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: crear|verificar archivo.json");
        }
        Path ruta = Path.of(args[1]);
        PersistenciaClubJson persistencia = new PersistenciaClubJson();
        if (args[0].equals("crear")) {
            EstadoClub club = new EstadoClub();
            gestores.GestorAcceso.Sesion operador = SoporteComercial.administrador(club);
            club.registrarJugador("S", "Socia", LocalDate.of(2000, 1, 1));
            club.registrarEmpleado("E", "Empleada");
            gestores.GestorAcceso.Sesion empleadoSesion = SoporteComercial.habilitar(club, operador, "E",
                    gestores.GestorAcceso.Perfil.EMPLEADO);
            SoporteComercial.habilitar(club, operador, "S", gestores.GestorAcceso.Perfil.SOCIO);
            GestorVentas g = club.getVentas();
            Inventario inv = club.getInventario();
            Empleado empleada = g.buscarEmpleadoPorCodigo("EMP-E");
            Usuario socio = club.buscarUsuario("S"), empleado = club.buscarUsuario("E");
            ok(!g.registrarEmpleadoConDescuento(operador, new Empleado(empleado)), "código duplicado rechazado");
            Articulo a = new Articulo("A", "Raqueta", "Deporte", 10000);
            inv.registrarProducto(a, 20);
            inv.registrarUbicacion("SEDE2");
            List<DetalleVenta> carrito = List.of(new DetalleVenta(a, 1));
            ok(g.procesarVenta(operador, "V1", empleado, "TIENDA", FECHA, carrito, "EMP-E", 0), "descuento propio");
            Venta v = g.buscarVenta("V1");
            igual(v.getDescuento(), 1500);
            igual(v.getImpuesto(), 1615);
            igual(v.getValorFinalPagado(), 10115);
            ok(g.consultarPuntos("E") == 202, "2 % redondeado abajo");
            ok(g.procesarVenta(operador, "V2", socio, "TIENDA", FECHA, carrito, "EMP-E", 100), "código compartido");
            igual(g.buscarVenta("V2").getValorFinalPagado(), 11048);
            ok(g.consultarPuntos("S") == 218, "propina excluida de puntos");
            ok(!g.procesarVenta(operador, "X", socio, "TIENDA", FECHA, carrito, "FALSO", 0), "código inexistente");
            long saldo = g.consultarPuntos("S");
            int stock = inv.consultarStock(a);
            int ventas = g.getVentasRegistradas().size();
            ok(!g.procesarVenta(operador, "X", socio, "TIENDA", FECHA, carrito, null, 0, 219, Inventario.PRINCIPAL),
                    "saldo insuficiente");
            ok(!g.procesarVenta(operador, "V2", socio, "TIENDA", FECHA, carrito, null, 0), "ID repetido");
            ok(!g.procesarVenta(operador, "X", socio, "TIENDA", FECHA,
                    List.of(new DetalleVenta(a, 12), new DetalleVenta(a, 12)), null, 0), "stock acumulado");
            ok(!g.procesarVenta(operador, "X", socio, "TIENDA", FECHA, carrito, null, Double.NaN), "propina NaN");
            ok(!g.procesarVenta(operador, "X", socio, "TIENDA", FECHA, List.of(new DetalleVenta(a, 1, 1)), null, 0),
                    "precio arbitrario rechazado");
            ok(saldo == g.consultarPuntos("S") && stock == inv.consultarStock(a)
                    && ventas == g.getVentasRegistradas().size(), "rechazos sin cambios parciales");
            ok(g.procesarVenta(operador, "V3", socio, "TIENDA", FECHA, carrito, null, 500, 200, Inventario.PRINCIPAL),
                    "redención");
            v = g.buscarVenta("V3");
            igual(v.getValorFinalPagado(), 12200);
            ok(v.getPuntosGanados() == 234 && g.consultarPuntos("S") == 252, "redención y acreditación netas");
            ok(inv.trasladarProducto("A", Inventario.PRINCIPAL, "SEDE2"), "traslado de catálogo completo");
            ok(inv.buscarProducto("A") == null && inv.consultarStock("SEDE2", a) == 17,
                    "origen deja de vender y destino conserva cantidades");
            ok(!inv.trasladarProducto("A", Inventario.PRINCIPAL, "SEDE2")
                    && !inv.trasladarProducto("A", "SEDE2", "SEDE2"), "traslados inválidos");
            ok(!g.procesarVenta(operador, "X", socio, "TIENDA", FECHA, carrito, null, 0), "origen no vende trasladado");
            ok(g.procesarVenta(operador, "V4", socio, "TIENDA", FECHA, carrito, null, 0, 0, "SEDE2"),
                    "venta en destino");
            Snack snack = new Snack("SN", "Snack", "Comida", 1000, null);
            ok(g.sugerirProducto(empleadoSesion, "SG1", empleada, snack, "SEDE2", 4),
                    "sugerencia de empleado registrado");
            ok(!g.resolverSugerencia(operador, "SG1", true) && inv.buscarProductoCatalogo("SN") == null,
                    "snack sin declaración no se aprueba");
            ok(g.completarAlergenosSugerencia(empleadoSesion, "SG1", empleada, List.of("gluten")),
                    "completar declaración pendiente");
            ok(g.resolverSugerencia(operador, "SG1", true) && !g.resolverSugerencia(operador, "SG1", true),
                    "aprobación única");
            Producto aprobado = inv.buscarProducto("SEDE2", "SN");
            ok(!g.procesarVenta(operador, "X", socio, "CAFETERIA", FECHA, List.of(new DetalleVenta(aprobado, 1)),
                    "EMP-E", 0, 0, "SEDE2"), "código de tienda no aplica en cafetería");
            ok(g.procesarVenta(operador, "V5", socio, "CAFETERIA", FECHA, List.of(new DetalleVenta(aprobado, 1)), null,
                    0, 0, "SEDE2"), "snack aprobado disponible");
            igual(g.buscarVenta("V5").getImpuesto(), 80);
            igual(g.buscarVenta("V5").getPropinaSugerida(), 100);
            ok(g.sugerirProducto(empleadoSesion, "SG2", empleada, new Articulo("R", "Rechazado", "Otros", 100),
                    Inventario.PRINCIPAL, 2) && g.resolverSugerencia(operador, "SG2", false)
                    && inv.buscarProductoCatalogo("R") == null, "rechazo no crea producto");
            ok(g.sugerirProducto(empleadoSesion, "SG3", empleada, new Snack("N", "Sin datos", "Comida", 100, null),
                    Inventario.PRINCIPAL, 1), "pendiente sin declaración");
            ok(g.consultarSugerencias(operador, "E").size() == 3, "consulta por autor");
            persistencia.salvar(ruta, club);
        } else if (args[0].equals("verificar")) {
            EstadoClub club = persistencia.cargar(ruta);
            gestores.GestorAcceso.Sesion operador = SoporteComercial.administrador(club);
            GestorVentas g = club.getVentas();
            Inventario inv = club.getInventario();
            ok(g.buscarVenta("V1").getComprador() == g.buscarEmpleadoPorCodigo("EMP-E").getUsuario(),
                    "identidad compartida empleado comprador");
            ok(g.buscarVenta("V2").getComprador() == club.buscarJugador("S").getUsuario(),
                    "identidad compartida jugador comprador");
            ok(g.getVentasRegistradas().size() == 5 && g.consultarPuntos("E") == 202 && g.consultarPuntos("S") == 511,
                    "recarga no repite puntos");
            Producto a = inv.buscarProducto("SEDE2", "A");
            ok(inv.buscarProducto("A") == null && inv.consultarStock("SEDE2", a) == 16,
                    "ubicación y stock persistentes");
            ok(g.buscarVenta("V1").getDetalles().get(0).getProducto() == a, "historial sobrevive traslado");
            ok(g.consultarSugerencias(operador, "E").size() == 3
                    && g.consultarSugerencias(operador, "E").get(0).getEstado() == SugerenciaProducto.Estado.APROBADA,
                    "estados de sugerencias persistentes");
            ok(!((Snack) g.consultarSugerencias(operador, "E").get(2).getProducto()).tieneDeclaracionAlergenos(),
                    "ausencia de declaración persistente");
            igual(g.buscarVenta("V3").getValorFinalPagado(), 12200);
            persistencia.salvar(ruta, club);
            EstadoClub otra = persistencia.cargar(ruta);
            ok(otra.getVentas().consultarPuntos("S") == 511 && otra.getInventario().consultarStock("SEDE2",
                    otra.getInventario().buscarProductoCatalogo("A")) == 16, "segunda recarga estable");
            JSONObject j = new JSONObject(Files.readString(ruta));
            j.getJSONArray("ventas").getJSONObject(0).put("compradorId", "NO_EXISTE");
            Path invalido = ruta.resolveSibling("bloque-d-invalido.json");
            Files.writeString(invalido, j.toString());
            boolean rechazo = false;
            try {
                persistencia.cargar(invalido);
            } catch (java.io.IOException e) {
                rechazo = true;
            }
            ok(rechazo, "referencia huérfana rechazada");
        } else {
            throw new IllegalArgumentException("Modo desconocido");
        }
    }
}
