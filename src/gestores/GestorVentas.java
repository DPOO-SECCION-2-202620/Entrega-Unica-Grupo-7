package gestores;

import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import modelo.usuarios.*;
import modelo.ventas.*;

public class GestorVentas {

    private final Inventario inventario;
    private final Map<String, Venta> ventas = new LinkedHashMap<>();
    private final Map<String, Empleado> empleados = new LinkedHashMap<>();
    private final Map<String, Long> saldos = new LinkedHashMap<>();
    private final Map<String, SugerenciaProducto> sugerencias = new LinkedHashMap<>();
    private final GestorAcceso acceso;
    private final GestorSeguridadInstalaciones seguridad;
    private final Function<String, Usuario> usuarios;

    public GestorVentas(Inventario inventario, GestorAcceso acceso, GestorSeguridadInstalaciones seguridad,
            Function<String, Usuario> usuarios) {
        this.inventario = inventario;
        this.acceso = acceso;
        this.seguridad = seguridad;
        this.usuarios = usuarios;
    }

    private boolean esOperador(Sesion sesion) {
        return (acceso.tienePerfilExplicito(sesion, Perfil.EMPLEADO)
                || acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR));
    }

    public boolean registrarUbicacion(Sesion sesion, String ubicacion) {
        if (!acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR)) {
            return false;
        }
        try {
            inventario.registrarUbicacion(ubicacion);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean registrarProducto(Sesion sesion, String ubicacion, Producto producto, int cantidad) {
        if (!acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR) || producto == null
                || !Double.isFinite(producto.getPrecio())
                || producto.getPrecio() != Dinero.redondear(producto.getPrecio())
                || (producto instanceof Snack && !((Snack) producto).tieneDeclaracionAlergenos())) {
            return false;
        }
        try {
            inventario.registrarProducto(ubicacion, producto, cantidad);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean reabastecer(Sesion sesion, String ubicacion, Producto producto, int cantidad) {
        if (!esOperador(sesion)) {
            return false;
        }
        try {
            inventario.reabastecer(ubicacion, producto, cantidad);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean trasladarProducto(Sesion sesion, String id, String origen, String destino) {
        return (acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR) && inventario.trasladarProducto(id, origen, destino));
    }

    public boolean registrarEmpleadoConDescuento(Sesion administrador, Empleado empleado) {
        return (acceso.tienePerfil(administrador, Perfil.ADMINISTRADOR) && cargarEmpleado(empleado));
    }

    public boolean cargarEmpleado(Empleado empleado) {
        if (empleado == null || empleados.containsKey(empleado.getCodigoDescuento())) {
            return false;
        }
        empleados.put(empleado.getCodigoDescuento(), empleado);
        return true;
    }

    public Empleado buscarEmpleadoPorCodigo(String codigo) {
        return empleados.get(codigo);
    }

    public List<Empleado> getEmpleados() {
        return new ArrayList<>(empleados.values());
    }

    public Venta buscarVenta(String id) {
        return ventas.get(id);
    }

    public List<Venta> getVentasRegistradas() {
        return new ArrayList<>(ventas.values());
    }

    public long consultarPuntos(String id) {
        return saldos.getOrDefault(id, 0L);
    }

    public Map<String, Long> getSaldos() {
        return new LinkedHashMap<>(saldos);
    }

    public void cargarSaldo(String id, long saldo) {
        if (id == null || id.isBlank() || saldo < 0 || saldos.containsKey(id)) {
            throw new IllegalArgumentException("Saldo inválido o duplicado.");
        }
        saldos.put(id, saldo);
    }

    public void validarSaldos() {
        Map<String, Long> calculados = new HashMap<>();
        for (Venta v : ventas.values()) {
            long previo = calculados.getOrDefault(v.getComprador().getId(), 0L);
            if (v.getPuntosRedimidos() > previo
                    || previo - v.getPuntosRedimidos() > Long.MAX_VALUE - v.getPuntosGanados()) {
                throw new IllegalArgumentException("Movimientos de fidelidad inválidos.");
            }
            calculados.put(v.getComprador().getId(), previo - v.getPuntosRedimidos() + v.getPuntosGanados());
        }
        Set<String> ids = new HashSet<>(calculados.keySet());
        ids.addAll(saldos.keySet());
        for (String id : ids) {
            if (consultarPuntos(id) != calculados.getOrDefault(id, 0L)) {
                throw new IllegalArgumentException("Saldo no coincide con movimientos: " + id);
            }
        }
    }

    public boolean procesarVenta(Sesion operador, String id, Usuario comprador, String rubro, LocalDate fecha,
            List<DetalleVenta> detalles, String codigo, double propina) {
        return procesarVenta(operador, id, comprador, rubro, fecha, detalles, codigo, propina, 0, Inventario.PRINCIPAL);
    }

    public boolean procesarVenta(Sesion operador, String id, Usuario comprador, String rubro, LocalDate fecha,
            List<DetalleVenta> detalles, String codigo, double propina, long redimidos, String ubicacion) {
        if (!esOperador(operador) || id == null || id.isBlank() || ventas.containsKey(id) || comprador == null
                || !comprador.esIdentificado() || !comprador.estaActivo() || comprador.getId() == null
                || comprador.getId().isBlank() || usuarios.apply(comprador.getId()) != comprador || fecha == null
                || rubro == null || (!rubro.equalsIgnoreCase("TIENDA") && !rubro.equalsIgnoreCase("CAFETERIA"))
                || detalles == null || detalles.isEmpty() || !Double.isFinite(propina) || propina < 0) {
            return false;
        }
        Empleado titular = null;
        if (codigo != null && !codigo.isBlank()) {
            titular = buscarEmpleadoPorCodigo(codigo);
            if (titular == null || !rubro.equalsIgnoreCase("TIENDA") || (!titular.getId().equals(comprador.getId())
                    && !acceso.tienePerfil(comprador.getId(), Perfil.SOCIO))) {
                return false;
            }
        }
        boolean llevaCaliente = detalles.stream().filter(Objects::nonNull)
                .anyMatch(d -> d.getProducto() instanceof Bebida && ((Bebida) d.getProducto()).esCaliente());
        if (llevaCaliente && !seguridad.puedeDespacharCaliente(comprador.getId())) {
            return false;
        }
        Map<Producto, Integer> cantidades = new HashMap<>();
        double subtotal = 0;
        for (DetalleVenta d : detalles) {
            if (d == null || inventario.buscarProducto(ubicacion, d.getProducto().getId()) != d.getProducto()
                    || !d.getProducto().getRubro().equalsIgnoreCase(rubro)
                    || d.getPrecioUnitario() != d.getProducto().getPrecio()) {
                return false;
            }
            long cantidad = (long) cantidades.getOrDefault(d.getProducto(), 0) + d.getCantidad();
            if (cantidad > Integer.MAX_VALUE) {
                return false;
            }
            cantidades.put(d.getProducto(), (int) cantidad);
            subtotal += d.getSubtotal();
        }
        for (Map.Entry<Producto, Integer> e : cantidades.entrySet()) {
            if (!inventario.hayExistencias(ubicacion, e.getKey(), e.getValue())) {
                return false;
            }
        }
        if (!Double.isFinite(subtotal)) {
            return false;
        }
        subtotal = Dinero.subtotal(detalles);
        propina = Dinero.redondear(propina);
        if (subtotal <= 0) {
            return false;
        }
        double descuento;
        if (titular == null) {
            descuento = 0;
        } else {
            String tasaDescuento;
            if (titular.getId().equals(comprador.getId())) {
                tasaDescuento = "0.15";
            } else {
                tasaDescuento = "0.08";
            }
            descuento = Dinero.porcentaje(subtotal, tasaDescuento);
        }
        String tasaImpuesto;
        if (rubro.equalsIgnoreCase("TIENDA")) {
            tasaImpuesto = "0.19";
        } else {
            tasaImpuesto = "0.08";
        }
        double impuesto = Dinero.porcentaje(Dinero.redondear(subtotal - descuento), tasaImpuesto);
        double comercial = Dinero.redondear(subtotal - descuento + impuesto);
        if (!Double.isFinite(comercial + propina) || redimidos < 0 || redimidos > comercial
                || redimidos > consultarPuntos(comprador.getId())) {
            return false;
        }
        double pagado = Dinero.redondear(comercial - redimidos);
        if (pagado * 0.02 >= Long.MAX_VALUE) {
            return false;
        }
        long ganados = Dinero.puntos(pagado), saldo = consultarPuntos(comprador.getId()) - redimidos;
        if (saldo > Long.MAX_VALUE - ganados) {
            return false;
        }
        Venta venta = new Venta(id, comprador, rubro.toUpperCase(Locale.ROOT), fecha, detalles, ubicacion, codigo,
                subtotal, descuento, impuesto, propina, redimidos, ganados);
        for (Map.Entry<Producto, Integer> e : cantidades.entrySet()) {
            inventario.reducirStock(ubicacion, e.getKey(), e.getValue());
        }
        saldos.put(comprador.getId(), saldo + ganados);
        ventas.put(id, venta);
        if (llevaCaliente) {
            seguridad.registrarPosesion(operador, comprador.getId());
        }
        return true;
    }

    public void cargarVenta(Venta v) {
        if (v == null || v.getComprador() == null || v.getIdVenta() == null || v.getIdVenta().isBlank()
                || ventas.containsKey(v.getIdVenta()) || v.getFecha() == null
                || (!"TIENDA".equals(v.getRubro()) && !"CAFETERIA".equals(v.getRubro()))
                || !inventario.getUbicaciones().contains(v.getUbicacion()) || v.getDetalles().isEmpty()
                || !Double.isFinite(v.getValorFinalPagado()) || v.getValorFinalPagado() < 0
                || !Double.isFinite(v.getPropina()) || v.getPropina() < 0 || !Double.isFinite(v.getSubtotal())
                || v.getSubtotal() <= 0 || !Double.isFinite(v.getDescuento()) || v.getDescuento() < 0
                || v.getDescuento() > v.getSubtotal() || !Double.isFinite(v.getImpuesto()) || v.getImpuesto() < 0
                || v.getPuntosRedimidos() < 0 || v.getPuntosGanados() < 0
                || v.getPuntosRedimidos() > v.getBaseImponible() + v.getImpuesto()) {
            throw new IllegalArgumentException("Venta guardada inválida o duplicada.");
        }
        double subtotal = 0;
        for (DetalleVenta d : v.getDetalles()) {
            if (inventario.buscarProductoCatalogo(d.getProducto().getId()) != d.getProducto()
                    || !d.getProducto().getRubro().equals(v.getRubro())) {
                throw new IllegalArgumentException("Producto de venta inexistente o incompatible.");
            }
            subtotal += d.getSubtotal();
        }
        if (Math.abs(Dinero.subtotal(v.getDetalles()) - v.getSubtotal()) > 0.001) {
            throw new IllegalArgumentException("Subtotal inconsistente.");
        }
        ventas.put(v.getIdVenta(), v);
    }

    public double consultarVentasPorRubroYPeriodo(String rubro, LocalDate inicio, LocalDate fin) {
        double total = 0;
        for (Venta v : ventas.values()) {
            if (v.getRubro().equalsIgnoreCase(rubro) && !v.getFecha().isBefore(inicio) && !v.getFecha().isAfter(fin)) {
                total += v.getValorFinalPagado();
            }
        }
        return total;
    }

    public boolean sugerirProducto(Sesion sesion, String id, Empleado autor, Producto producto, String ubicacion,
            int cantidad) {
        if (!acceso.tienePerfil(sesion, Perfil.EMPLEADO) || autor == null
                || !sesion.getUsuarioId().equals(autor.getId())) {
            return false;
        }
        try {
            cargarSugerencia(new SugerenciaProducto(id, autor, producto, ubicacion, cantidad,
                    SugerenciaProducto.Estado.PENDIENTE));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void cargarSugerencia(SugerenciaProducto s) {
        if (s == null || sugerencias.containsKey(s.getId())
                || buscarEmpleadoPorCodigo(s.getAutor().getCodigoDescuento()) != s.getAutor()
                || !inventario.getUbicaciones().contains(s.getUbicacion())) {
            throw new IllegalArgumentException("Sugerencia inválida o duplicada.");
        }
        sugerencias.put(s.getId(), s);
    }

    public boolean completarAlergenosSugerencia(Sesion sesion, String id, Empleado autor, List<String> alergenos) {
        SugerenciaProducto s = sugerencias.get(id);
        if (!acceso.tienePerfil(sesion, Perfil.EMPLEADO) || autor == null
                || !sesion.getUsuarioId().equals(autor.getId()) || s == null || s.getAutor() != autor
                || s.getEstado() != SugerenciaProducto.Estado.PENDIENTE || !(s.getProducto() instanceof Snack)
                || alergenos == null) {
            return false;
        }
        Producto p = s.getProducto();
        try {
            Snack snack = new Snack(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), alergenos);
            sugerencias.put(id,
                    new SugerenciaProducto(id, autor, snack, s.getUbicacion(), s.getCantidadInicial(), s.getEstado()));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean resolverSugerencia(Sesion sesion, String id, boolean aprobar) {
        SugerenciaProducto s = sugerencias.get(id);
        if (!acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR) || s == null
                || s.getEstado() != SugerenciaProducto.Estado.PENDIENTE) {
            return false;
        }

        if (aprobar) {
            if (s.getProducto() instanceof Snack && !((Snack) s.getProducto()).tieneDeclaracionAlergenos()) {
                return false;
            }

            if (inventario.buscarProductoCatalogo(s.getProducto().getId()) != null) {
                return false;
            }
            try {
                inventario.registrarProducto(s.getUbicacion(), s.getProducto(), s.getCantidadInicial());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        modelo.ventas.SugerenciaProducto.Estado estadoSugerencia;
        if (aprobar) {
            estadoSugerencia = SugerenciaProducto.Estado.APROBADA;
        } else {
            estadoSugerencia = SugerenciaProducto.Estado.RECHAZADA;
        }
        sugerencias.put(id, new SugerenciaProducto(id, s.getAutor(), s.getProducto(), s.getUbicacion(),
                s.getCantidadInicial(), estadoSugerencia));
        return true;
    }

    public List<SugerenciaProducto> getSugerencias() {
        return new ArrayList<>(sugerencias.values());
    }

    public List<SugerenciaProducto> consultarSugerencias(Sesion sesion, String empleadoId) {
        List<SugerenciaProducto> resultado = new ArrayList<>();
        if (!acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR)
                && (!acceso.tienePerfil(sesion, Perfil.EMPLEADO) || !sesion.getUsuarioId().equals(empleadoId))) {
            return resultado;
        }
        for (SugerenciaProducto s : sugerencias.values()) {
            if (s.getAutor().getId().equals(empleadoId)) {
                resultado.add(s);
            }
        }
        return resultado;
    }
}
