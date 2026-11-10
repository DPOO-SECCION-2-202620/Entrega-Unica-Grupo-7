package pruebas;

import aplicacion.EstadoClub;
import gestores.*;
import gestores.GestorAcceso.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.reservas.*;
import modelo.usuarios.*;
import modelo.ventas.*;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaCierreBloqueD {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-06T15:30:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 6);

    private static void ok(boolean valor, String mensaje) {
        if (!valor) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    private static List<DetalleVenta> carrito(Producto p) {
        return List.of(new DetalleVenta(p, 1));
    }

    private static void reservar(EstadoClub c, String usuario, String instalacion, int cantidad) {
        SoporteComercial.academiaHistorica(c, usuario,
                c.getReservas().buscarInstalacion(instalacion).getDeporte().getId());
        String modalidadSeleccionada;
        if (cantidad == 4) {
            modalidadSeleccionada = "Dobles";
        } else {
            modalidadSeleccionada = "Sencillos";
        }
        ok(c.getReservas().realizarReserva("R-" + usuario, c.buscarJugador(usuario),
                c.getReservas().buscarInstalacion(instalacion), LocalDateTime.of(2026, 11, 6, 10, 0),
                LocalDateTime.of(2026, 11, 6, 11, 0), cantidad, modalidadSeleccionada), "reserva " + usuario);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: crear|verificar archivo.json");
        }
        Path ruta = Path.of(args[1]);
        PersistenciaClubJson p = new PersistenciaClubJson(RELOJ);
        if (args[0].equals("crear")) {
            EstadoClub c = new EstadoClub(RELOJ);
            Sesion admin = SoporteComercial.administrador(c);
            ok(!c.crearAdministradorInicial("OTRO", "Otro", "OtraClave2026"),
                    "bootstrap no permite segundo administrador");
            ok(c.getAcceso().autenticar("ADMIN-TEST", "incorrecta") == null, "clave errónea rechazada");
            ok(c.altaEmpleado(admin, "E", "Caja") && c.altaEmpleado(admin, "E2", "Otro empleado"),
                    "altas de empleado autorizadas");
            Sesion empleado = SoporteComercial.habilitar(c, admin, "E", Perfil.EMPLEADO);
            Sesion otroEmpleado = SoporteComercial.habilitar(c, admin, "E2", Perfil.EMPLEADO);
            for (String id : List.of("S", "P", "T", "A", "B")) {
                c.registrarJugador(id, id, LocalDate.of(2000, 1, 1));
                SoporteComercial.habilitar(c, admin, id, Perfil.SOCIO);
            }
            c.registrarUsuario("N", "Cliente sin perfil socio");
            Sesion socio = c.getAcceso().autenticar("S", "ClavePrueba2026");
            GestorVentas g = c.getVentas();
            Inventario inv = c.getInventario();
            Producto articulo = new Articulo("ART", "Artículo", "Deportes", 10000),
                    cafe = new Bebida("HOT", "Café", "Bebidas", 1000, true),
                    fria = new Bebida("COLD", "Agua", "Bebidas", 100, false),
                    barato = new Snack("SN", "Snack sin alérgenos", "Comida", 100, List.of());
            ok(!g.registrarUbicacion(empleado, "SECUNDARIA") && !c.altaEmpleado(socio, "NO", "No"),
                    "permisos de administración comprobados en servicios");
            ok(!c.habilitarCuenta(empleado, "N", "ClavePrueba2026", Set.of(Perfil.ADMINISTRADOR)),
                    "empleado no concede permisos");
            ok(g.registrarUbicacion(admin, "SECUNDARIA"), "alta de ubicación");
            for (Producto producto : List.of(articulo, cafe, fria, barato)) {
                ok(g.registrarProducto(admin, Inventario.PRINCIPAL, producto, 20),
                        "alta de producto " + producto.getId());
            }
            ok(!g.procesarVenta(socio, "NO", c.buscarUsuario("S"), "TIENDA", FECHA, carrito(articulo), null, 0),
                    "socio no opera caja");
            Sesion ajena = SoporteComercial.administrador(new EstadoClub());
            ok(!g.procesarVenta(ajena, "NO", c.buscarUsuario("S"), "TIENDA", FECHA, carrito(articulo), null, 0),
                    "sesión de otro estado rechazada");
            int stock = inv.consultarStock(articulo);
            ok(!g.procesarVenta(empleado, "NO", c.buscarUsuario("N"), "TIENDA", FECHA, carrito(articulo), "EMP-E", 0),
                    "cliente sin condición de socio no recibe código compartido");
            ok(!g.procesarVenta(empleado, "NO", new Usuario("S", "Suplantación", "s", "x"), "TIENDA", FECHA,
                    carrito(articulo), "EMP-E", 0), "comprador con mismo ID pero otra identidad rechazado");
            ok(inv.consultarStock(articulo) == stock && g.getVentasRegistradas().isEmpty(),
                    "rechazos de identidad sin cambios");
            ok(g.procesarVenta(empleado, "SOCIO", c.buscarUsuario("S"), "TIENDA", FECHA, carrito(articulo), "EMP-E", 0),
                    "socio verificado obtiene 8 %");
            ok(g.buscarVenta("SOCIO").getDescuento() == 800, "descuento compartido exacto");
            ok(!g.trasladarProducto(empleado, "ART", Inventario.PRINCIPAL, "SECUNDARIA"),
                    "traslado exige administrador");
            inv.registrarProducto("SECUNDARIA", articulo, 0);
            ok(!g.trasladarProducto(admin, "ART", Inventario.PRINCIPAL, "SECUNDARIA")
                    && inv.consultarStock(articulo) == 19 && inv.consultarStock("SECUNDARIA", articulo) == 0,
                    "destino ya presente con stock cero rechaza sin cambios");
            Producto nuevo = new Snack("SUG", "Sugerido", "Comida", 200, null);
            Empleado autor = g.buscarEmpleadoPorCodigo("EMP-E");
            ok(!g.sugerirProducto(socio, "SG", autor, nuevo, Inventario.PRINCIPAL, 2), "solo empleado autor propone");
            ok(g.sugerirProducto(empleado, "SG", autor, nuevo, Inventario.PRINCIPAL, 2), "sugerencia propia");
            ok(!g.resolverSugerencia(empleado, "SG", true) && inv.buscarProductoCatalogo("SUG") == null,
                    "empleado no puede aprobar");
            ok(!g.resolverSugerencia(admin, "SG", true), "administrador tampoco aprueba sin declaración");
            ok(!g.completarAlergenosSugerencia(otroEmpleado, "SG", autor, List.of()),
                    "otro empleado no edita declaración");
            ok(g.completarAlergenosSugerencia(empleado, "SG", autor, List.of())
                    && g.resolverSugerencia(admin, "SG", true), "declaración explícita ninguno habilita aprobación");
            ok(!g.resolverSugerencia(admin, "SG", false) && g.consultarSugerencias(otroEmpleado, "E").isEmpty(),
                    "decisión final y privacidad por autor");
            ok(g.sugerirProducto(otroEmpleado, "RECH", g.buscarEmpleadoPorCodigo("EMP-E2"),
                    new Articulo("RECH", "Rechazado", "Otros", 10), Inventario.PRINCIPAL, 1)
                    && g.resolverSugerencia(admin, "RECH", false), "rechazo de sugerencia");
            ok(g.consultarSugerencias(otroEmpleado, "E2").get(0).getEstado() == SugerenciaProducto.Estado.RECHAZADA
                    && inv.buscarProductoCatalogo("RECH") == null, "autor ve rechazo sin alta en catálogo");
            for (Instalacion i : List.of(new Instalacion("PA", "Pádel", c.buscarDeporte("PADEL"), 4, false),
                    new Instalacion("MT", "Mesa cubierta", c.buscarDeporte("MESA"), 4, true),
                    new Instalacion("MA", "Mesa exterior", c.buscarDeporte("MESA"), 4, false),
                    new Instalacion("TE", "Tenis", c.buscarDeporte("TENIS"), 4, true))) {
                ok(c.getReservas().registrarInstalacion(i), "instalación " + i.getId());
            }
            reservar(c, "S", "MT", 2);
            reservar(c, "P", "PA", 4);
            reservar(c, "T", "TE", 2);
            reservar(c, "A", "MA", 2);
            ok(g.procesarVenta(empleado, "CAFE", c.buscarUsuario("S"), "CAFETERIA", FECHA, carrito(cafe), null, 0),
                    "comprar caliente registra posesión");
            ok(!c.getSeguridad().iniciarUso(socio, "R-S"), "caliente impide ingreso a mesa techada");
            ok(c.getSeguridad().finalizarBebida(socio, "S") && c.getSeguridad().iniciarUso(socio, "R-S"),
                    "consumir permite ingresar");
            stock = inv.consultarStock(cafe);
            long puntos = g.consultarPuntos("S");
            int ventas = g.getVentasRegistradas().size();
            ok(!g.procesarVenta(empleado, "NO", c.buscarUsuario("S"), "CAFETERIA", FECHA.minusDays(1), carrito(cafe),
                    null, 0), "uso activo impide despacho aunque alteren fecha de venta");
            ok(inv.consultarStock(cafe) == stock && g.consultarPuntos("S") == puntos
                    && g.getVentasRegistradas().size() == ventas, "rechazo de seguridad atómico");
            Sesion acompanante = c.getAcceso().autenticar("B", "ClavePrueba2026");
            ok(!c.getSeguridad().iniciarUso(acompanante, "R-S", "B"), "acompañante requiere registro del personal");
            ok(g.procesarVenta(empleado, "ACOMP-CAFE", c.buscarUsuario("B"), "CAFETERIA", FECHA, carrito(cafe), null, 0)
                    && !c.getSeguridad().iniciarUso(empleado, "R-S", "B"),
                    "bebida también impide ingreso de acompañante");
            ok(c.getSeguridad().finalizarBebida(acompanante, "B") && c.getSeguridad().iniciarUso(empleado, "R-S", "B"),
                    "personal registra acompañante sin bebida");
            ok(!g.procesarVenta(empleado, "NO", c.buscarUsuario("B"), "CAFETERIA", FECHA, carrito(cafe), null, 0)
                    && !c.getSeguridad().iniciarUso(empleado, "R-S", "E"),
                    "acompañante protegido y cupo de ingreso respetado");
            ok(g.procesarVenta(empleado, "AGUA", c.buscarUsuario("S"), "CAFETERIA", FECHA, carrito(fria), null, 0),
                    "fría permitida dentro");
            ok(c.getSeguridad().iniciarUso(empleado, "R-P") && !g.procesarVenta(empleado, "NO", c.buscarUsuario("P"),
                    "CAFETERIA", FECHA, carrito(cafe), null, 0), "pádel restringe incluso sin techo");
            ok(g.procesarVenta(empleado, "TENIS", c.buscarUsuario("T"), "CAFETERIA", FECHA, carrito(cafe), null, 0)
                    && c.getSeguridad().iniciarUso(empleado, "R-T"), "tenis permite caliente");
            ok(g.procesarVenta(empleado, "AIRE", c.buscarUsuario("A"), "CAFETERIA", FECHA, carrito(cafe), null, 0)
                    && c.getSeguridad().iniciarUso(empleado, "R-A"), "mesa exterior permite caliente");
            ok(!c.getSeguridad().finalizarUso(socio, "P") && !c.getSeguridad().finalizarBebida(socio, "T"),
                    "socio no altera uso o posesión ajenos");
            ok(g.procesarVenta(empleado, "ACUMULA", c.buscarUsuario("N"), "TIENDA", FECHA, carrito(articulo), null, 0),
                    "cliente acumula puntos sin descuento");
            ok(g.consultarPuntos("N") == 238, "acumulación inicial");
            ok(!g.procesarVenta(empleado, "NO", c.buscarUsuario("N"), "CAFETERIA", FECHA, carrito(barato), null, 50,
                    109, Inventario.PRINCIPAL), "puntos no pagan propina aun con saldo suficiente");
            ok(g.procesarVenta(empleado, "REDIME", c.buscarUsuario("N"), "CAFETERIA", FECHA, carrito(barato), null, 50,
                    108, Inventario.PRINCIPAL), "redención comercial completa");
            ok(g.buscarVenta("REDIME").getValorFinalPagado() == 50 && g.buscarVenta("REDIME").getPuntosGanados() == 0
                    && g.consultarPuntos("N") == 130, "solo paga propina y no gana puntos");
            Producto decimal = new Articulo("DEC", "Decimal", "Otros", 0.10);
            g.registrarProducto(admin, Inventario.PRINCIPAL, decimal, 5);
            ok(g.procesarVenta(empleado, "DECIMAL", c.buscarUsuario("N"), "TIENDA", FECHA,
                    List.of(new DetalleVenta(decimal, 3)), null, 0), "venta con centavos");
            ok(g.buscarVenta("DECIMAL").getSubtotal() == 0.30 && g.buscarVenta("DECIMAL").getImpuesto() == 0.06
                    && g.buscarVenta("DECIMAL").getValorFinalPagado() == 0.36, "redondeo monetario reproducible");
            c.getAcceso().cerrarSesion(otroEmpleado);
            ok(!g.resolverSugerencia(otroEmpleado, "RECH", true), "sesión cerrada sin permisos");
            p.salvar(ruta, c);
            String contenido = Files.readString(ruta);
            ok(!contenido.contains("ClavePrueba2026") && !contenido.contains("password")
                    && !contenido.contains("\"sesiones\""), "sin contraseñas legibles ni sesiones persistidas");
        } else if (args[0].equals("verificar")) {
            EstadoClub c = p.cargar(ruta);
            GestorVentas g = c.getVentas();
            Sesion admin = SoporteComercial.administrador(c);
            Sesion empleado = c.getAcceso().autenticar("E", "ClavePrueba2026"),
                    socio = c.getAcceso().autenticar("S", "ClavePrueba2026");
            ok(empleado != null && socio != null && c.getAcceso().tienePerfil("S", Perfil.SOCIO),
                    "credenciales y perfiles sobreviven reinicio");
            ok(c.getSeguridad().getUsos().size() == 5 && c.getSeguridad().tieneBebidaCaliente("T")
                    && c.getSeguridad().tieneBebidaCaliente("A"), "usos y posesiones persistentes");
            int stock = c.getInventario().consultarStock(c.getInventario().buscarProducto("HOT"));
            long saldo = g.consultarPuntos("S");
            ok(!g.procesarVenta(empleado, "NO", c.buscarUsuario("S"), "CAFETERIA", FECHA,
                    carrito(c.getInventario().buscarProducto("HOT")), null, 0), "restricción vigente tras recarga");
            ok(stock == c.getInventario().consultarStock(c.getInventario().buscarProducto("HOT"))
                    && saldo == g.consultarPuntos("S"), "sin efectos al recargar o rechazar");
            ok(!c.getReservas().buscarInstalacion("MA").esTechada()
                    && c.getReservas().buscarInstalacion("MT").esTechada(), "condición techada persistente");
            ok(g.consultarPuntos("N") == 130 && g.buscarVenta("REDIME").getPuntosRedimidos() == 108,
                    "saldo y movimientos recuperados");
            ok(!g.resolverSugerencia(empleado, "SG", true)
                    && g.consultarSugerencias(admin, "E2").get(0).getEstado() == SugerenciaProducto.Estado.RECHAZADA,
                    "permisos y decisiones recuperados");
            p.salvar(ruta, c);
            EstadoClub segunda = p.cargar(ruta);
            ok(segunda.getVentas().consultarPuntos("N") == 130 && segunda.getSeguridad().getUsos().size() == 5,
                    "segunda recarga estable");
            EstadoClub despues = new PersistenciaClubJson(
                    Clock.fixed(Instant.parse("2026-11-06T16:00:00Z"), ZoneId.of("America/Bogota"))).cargar(ruta);
            ok(despues.getSeguridad().puedeDespacharCaliente("S"), "fin exacto de reserva libera uso automáticamente");
            Sesion operadorDespues = despues.getAcceso().autenticar("E", "ClavePrueba2026");
            ok(!despues.getSeguridad().iniciarUso(operadorDespues, "R-S"), "no ingresa al terminar la reserva");
            JSONObject original = new JSONObject(Files.readString(ruta));
            JSONObject corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("saldos").getJSONObject(0).put("saldo", 999999);
            rechazar(p, ruta, corrupto, "saldo manipulado");
            corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("usosInstalaciones").getJSONObject(0).put("reservaId", "NO_EXISTE");
            rechazar(p, ruta, corrupto, "uso huérfano");
            corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("posesionesCalientes").put("NO_EXISTE");
            rechazar(p, ruta, corrupto, "posesión huérfana");
            corrupto = new JSONObject(original.toString());
            corrupto.getJSONArray("cuentas").getJSONObject(0).put("usuarioId", "NO_EXISTE");
            rechazar(p, ruta, corrupto, "cuenta huérfana");
        } else {
            throw new IllegalArgumentException("Modo desconocido");
        }
    }

    private static void rechazar(PersistenciaClubJson p, Path ruta, JSONObject corrupto, String caso) throws Exception {
        Path invalido = ruta.resolveSibling("cierre-invalido.json");
        String texto = corrupto.toString();
        Files.writeString(invalido, texto);
        boolean rechazo = false;
        try {
            p.cargar(invalido);
        } catch (java.io.IOException e) {
            rechazo = true;
        }
        ok(rechazo && texto.equals(Files.readString(invalido)), caso + " rechazado sin sobrescribir");
    }
}
