package persistencia;

import aplicacion.EstadoClub;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.deportes.Deporte;
import modelo.reservas.*;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;
import modelo.ventas.*;
import org.json.*;

public class PersistenciaClubJson implements IPersistenciaClub {

    private final Clock reloj;

    public PersistenciaClubJson() {
        this(Clock.system(ZoneId.of("America/Bogota")));
    }

    public PersistenciaClubJson(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public EstadoClub cargar(Path archivo) throws IOException {
        if (Files.notExists(archivo)) {
            return new EstadoClub(reloj);
        }
        try {
            JSONObject raiz = new JSONObject(Files.readString(archivo, StandardCharsets.UTF_8));
            if (raiz.getInt("version") < 1 || raiz.getInt("version") > 7) {
                throw new IllegalArgumentException("Versión de archivo no compatible.");
            }

            if (raiz.getInt("version") >= 2) {
                for (String seccion : List.of("productos", "ventas", "empleados", "usuariosComerciales", "ubicaciones",
                        "saldos", "sugerencias")) {
                    raiz.getJSONArray(seccion);
                }
            }

            if (raiz.getInt("version") >= 3) {
                for (String seccion : List.of("cuentas", "posesionesCalientes", "usosInstalaciones")) {
                    raiz.getJSONArray(seccion);
                }
            }
            EstadoClub nuevo = new EstadoClub(reloj);
            validarDeportes(raiz.getJSONArray("deportes"), nuevo, raiz.getInt("version"));
            JSONArray jugadores = raiz.getJSONArray("jugadores");
            for (int i = 0; i < jugadores.length(); i++) {
                JSONObject j = jugadores.getJSONObject(i);
                nuevo.registrarJugador(j.getString("id"), j.getString("nombre"),
                        LocalDate.parse(j.getString("fechaNacimiento")));
                Jugador jugador = nuevo.buscarJugador(j.getString("id"));
                jugador.getUsuario().restaurarActivo(j.optBoolean("activo", true));
                jugador.actualizarCorreo(j.optString("correo", ""));
                if (j.has("movimientosRanking")) {
                    JSONArray movimientos = j.getJSONArray("movimientosRanking");
                    for (int k = 0; k < movimientos.length(); k++) {
                        JSONObject m = movimientos.getJSONObject(k);
                        if (nuevo.buscarDeporte(m.getString("disciplina")) == null
                                || Math.abs(jugador.getRankingsPorDisciplina()
                                        .getOrDefault(Deporte.clave(m.getString("disciplina")), 1000.0)
                                        - m.getDouble("anterior")) > 0.001) {
                            throw new IllegalArgumentException("Movimiento de ranking inconsistente.");
                        }
                        jugador.actualizarRanking(m.getString("disciplina"), m.getDouble("nuevo"),
                                LocalDate.parse(m.getString("fecha")), m.getString("referencia"));
                    }
                }
            }
            if (raiz.has("usuariosComerciales")) {
                JSONArray usuarios = raiz.getJSONArray("usuariosComerciales");
                for (int i = 0; i < usuarios.length(); i++) {
                    JSONObject u = usuarios.getJSONObject(i);
                    if (u.optBoolean("identificado", true)) {
                        nuevo.registrarUsuario(u.getString("id"), u.getString("nombre"));
                    } else {
                        nuevo.cargarUsuarioHistorico(u.getString("id"), u.getString("nombre"));
                    }
                    nuevo.buscarUsuario(u.getString("id")).restaurarActivo(u.optBoolean("activo", true));
                }
            }

            if (raiz.has("empleados")) {
                JSONArray empleados = raiz.getJSONArray("empleados");
                for (int i = 0; i < empleados.length(); i++) {
                    JSONObject e = empleados.getJSONObject(i);
                    nuevo.registrarEmpleado(e.getString("id"), e.getString("nombre"));
                }
            }

            if (raiz.has("ubicaciones")) {
                JSONArray ubicaciones = raiz.getJSONArray("ubicaciones");
                Set<String> vistas = new HashSet<>();
                for (int i = 0; i < ubicaciones.length(); i++) {
                    String id = ubicaciones.getString(i);
                    if (!vistas.add(id)) {
                        throw new IllegalArgumentException("Ubicación duplicada.");
                    }

                    if (!Inventario.PRINCIPAL.equals(id)) {
                        nuevo.getInventario().registrarUbicacion(id);
                    }
                }
                if (!vistas.contains(Inventario.PRINCIPAL)) {
                    throw new IllegalArgumentException("Falta ubicación principal.");
                }
            }
            JSONArray instalaciones = raiz.getJSONArray("instalaciones");
            for (int i = 0; i < instalaciones.length(); i++) {
                JSONObject j = instalaciones.getJSONObject(i);
                Deporte deporte = nuevo.buscarDeporte(j.getString("deporteId"));
                String id = j.getString("id");
                String nombre = j.getString("nombre");
                int capacidad = j.getInt("capacidadMaxima");
                if (id.isBlank() || nombre.isBlank() || capacidad <= 0 || deporte == null) {
                    throw new IllegalArgumentException("Instalación inválida o duplicada: " + id);
                }
                boolean techada;
                if (raiz.getInt("version") >= 3) {
                    techada = j.getBoolean("techada");
                } else {
                    techada = j.optBoolean("techada", true);
                }

                if (!nuevo.getReservas()
                        .registrarInstalacion(new Instalacion(id, nombre, deporte, capacidad, techada))) {
                    throw new IllegalArgumentException("Instalación inválida o duplicada: " + id);
                }
            }
            JSONArray reservas = raiz.getJSONArray("reservas");
            for (int i = 0; i < reservas.length(); i++) {
                JSONObject j = reservas.getJSONObject(i);
                Jugador jugador = nuevo.buscarJugador(j.getString("jugadorId"));
                Instalacion instalacion = nuevo.getReservas().buscarInstalacion(j.getString("instalacionId"));
                Reserva reserva = new Reserva(j.getString("id"), jugador, instalacion,
                        LocalDateTime.parse(j.getString("inicio")), LocalDateTime.parse(j.getString("fin")),
                        j.getInt("cantidadParticipantes"), j.getString("modalidad"));
                reserva.restaurarCancelada(j.optBoolean("cancelada", false));
                if (raiz.getInt("version") >= 6) {
                    PersistenciaAcademiasAgenda.cargarCambios(reserva, j.getJSONArray("cambiosAgenda"), nuevo);
                }
                nuevo.getReservas().cargarReserva(reserva);
            }
            if (raiz.has("productos") != raiz.has("ventas")) {
                throw new IllegalArgumentException("Falta la sección de productos o ventas.");
            }

            if (raiz.has("productos")) {
                cargarProductos(raiz.getJSONArray("productos"), nuevo);
                cargarVentas(raiz.getJSONArray("ventas"), nuevo, raiz.getInt("version"));
            }

            if (raiz.has("saldos")) {
                JSONArray saldos = raiz.getJSONArray("saldos");
                for (int i = 0; i < saldos.length(); i++) {
                    JSONObject saldo = saldos.getJSONObject(i);
                    if (nuevo.buscarUsuario(saldo.getString("compradorId")) == null) {
                        throw new IllegalArgumentException("Saldo sin comprador.");
                    }
                    nuevo.getVentas().cargarSaldo(saldo.getString("compradorId"), saldo.getLong("saldo"));
                }
            }

            if (raiz.has("sugerencias")) {
                JSONArray sugerencias = raiz.getJSONArray("sugerencias");
                for (int i = 0; i < sugerencias.length(); i++) {
                    JSONObject j = sugerencias.getJSONObject(i);
                    modelo.usuarios.Empleado autor = nuevo.getVentas()
                            .buscarEmpleadoPorCodigo(j.getString("autorCodigo"));
                    SugerenciaProducto.Estado estado = SugerenciaProducto.Estado.valueOf(j.getString("estado"));
                    Producto producto = leerProducto(j.getJSONObject("producto"));
                    if (estado == SugerenciaProducto.Estado.APROBADA) {
                        producto = nuevo.getInventario().buscarProductoCatalogo(producto.getId());
                        if (producto == null) {
                            throw new IllegalArgumentException("Sugerencia aprobada sin producto.");
                        }
                    }
                    nuevo.getVentas().cargarSugerencia(new SugerenciaProducto(j.getString("id"), autor, producto,
                            j.getString("ubicacion"), j.getInt("cantidadInicial"), estado));
                }
            }

            if (raiz.has("cuentas")) {
                JSONArray cuentas = raiz.getJSONArray("cuentas");
                for (int i = 0; i < cuentas.length(); i++) {
                    JSONObject j = cuentas.getJSONObject(i);
                    String id = j.getString("usuarioId");
                    if (nuevo.buscarUsuario(id) == null || !nuevo.buscarUsuario(id).esIdentificado()) {
                        throw new IllegalArgumentException("Cuenta sin usuario identificado.");
                    }
                    Set<gestores.GestorAcceso.Perfil> perfiles = new HashSet<>();
                    JSONArray lista = j.getJSONArray("perfiles");
                    for (int k = 0; k < lista.length(); k++) {
                        if (!perfiles.add(gestores.GestorAcceso.Perfil.valueOf(lista.getString(k)))) {
                            throw new IllegalArgumentException("Perfil duplicado.");
                        }
                    }
                    if (perfiles.contains(gestores.GestorAcceso.Perfil.EMPLEADO)
                            && nuevo.getVentas().buscarEmpleadoPorCodigo("EMP-" + id) == null) {
                        throw new IllegalArgumentException("Cuenta de empleado sin empleado registrado.");
                    }

                    if (perfiles.contains(gestores.GestorAcceso.Perfil.ENTRENADOR)
                            && perfiles.contains(gestores.GestorAcceso.Perfil.FISIOTERAPEUTA)) {
                        throw new IllegalArgumentException(
                                "Un puesto laboral no puede cubrir dos especialidades a la vez.");
                    }

                    if ((perfiles.contains(gestores.GestorAcceso.Perfil.ENTRENADOR)
                            || perfiles.contains(gestores.GestorAcceso.Perfil.FISIOTERAPEUTA))
                            && nuevo.getVentas().buscarEmpleadoPorCodigo("EMP-" + id) == null) {
                        throw new IllegalArgumentException("Perfil laboral sin empleado.");
                    }
                    boolean activa = j.optBoolean("activa", true);
                    if (activa != nuevo.buscarUsuario(id).estaActivo()) {
                        throw new IllegalArgumentException("Estado de cuenta/usuario incompatible.");
                    }
                    nuevo.getAcceso().cargarCuenta(new gestores.GestorAcceso.Cuenta(id, j.optString("login", id),
                            perfiles, j.getString("sal"), j.getString("hash"), activa));
                }
                if (!nuevo.getAcceso().tieneAdministrador() && !nuevo.getAcceso().puedeCrearAdministradorInicial()) {
                    throw new IllegalArgumentException("Cuentas sin administrador.");
                }
            }

            if (raiz.getInt("version") >= 6) {
                PersistenciaAcademiasAgenda.cargarInscripciones(raiz, nuevo);
            }

            if (raiz.getInt("version") >= 4) {
                PersistenciaModulosClub.cargar(raiz, nuevo);
            }

            if (raiz.getInt("version") >= 6) {
                PersistenciaAcademiasAgenda.cargarMaximosYValidar(raiz, nuevo);
            }

            if (raiz.getInt("version") >= 5) {
                PersistenciaDisciplina.cargar(raiz, nuevo);
            }

            if (raiz.getInt("version") >= 7) {
                PersistenciaTorneosInternos.cargar(raiz, nuevo);
            }

            if (raiz.has("posesionesCalientes")) {
                JSONArray posesiones = raiz.getJSONArray("posesionesCalientes");
                for (int i = 0; i < posesiones.length(); i++) {
                    String id = posesiones.getString(i);
                    if (nuevo.buscarUsuario(id) == null) {
                        throw new IllegalArgumentException("Posesión sin usuario.");
                    }
                    nuevo.getSeguridad().cargarPosesion(id);
                }
            }

            if (raiz.has("usosInstalaciones")) {
                JSONArray usos = raiz.getJSONArray("usosInstalaciones");
                for (int i = 0; i < usos.length(); i++) {
                    JSONObject uso = usos.getJSONObject(i);
                    nuevo.getSeguridad().cargarUso(uso.getString("usuarioId"), uso.getString("reservaId"));
                }
            }

            if (raiz.getInt("version") >= 3) {
                nuevo.getVentas().validarSaldos();
            }
            return nuevo;
        } catch (RuntimeException e) {
            throw new IOException("No se pudo cargar " + archivo + ": " + e.getMessage(), e);
        }
    }

    private void validarDeportes(JSONArray arreglo, EstadoClub club, int version) {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject j = arreglo.getJSONObject(i);
            String id = j.getString("id");
            Deporte conocido = club.buscarDeporte(id);
            if (!vistos.add(id) || conocido == null || !conocido.getNombre().equals(j.getString("nombre"))
                    || j.getBoolean("colectivo") != conocido.isColectivo()) {
                throw new IllegalArgumentException("Deporte inválido o duplicado: " + id);
            }
        }
        if (vistos.size() != club.getDeportes().size()
                && !(version < 4 && vistos.equals(Set.of("TENIS", "PADEL", "MESA")))) {
            throw new IllegalArgumentException("El catálogo de deportes está incompleto.");
        }
    }

    @Override
    public void salvar(Path archivo, EstadoClub club) throws IOException {
        JSONObject raiz = new JSONObject();
        raiz.put("version", 7);
        JSONArray deportes = new JSONArray();
        for (Deporte d : club.getDeportes()) {
            deportes.put(new JSONObject().put("id", d.getId()).put("nombre", d.getNombre()).put("colectivo",
                    d.isColectivo()));
        }
        JSONArray jugadores = new JSONArray();
        for (Jugador j : club.getJugadores()) {
            JSONArray movimientos = new JSONArray();
            for (Jugador.MovimientoRanking m : j.getMovimientosRanking()) {
                movimientos.put(new JSONObject().put("disciplina", m.disciplina()).put("fecha", m.fecha().toString())
                        .put("anterior", m.anterior()).put("nuevo", m.nuevo()).put("referencia", m.referencia()));
            }
            jugadores.put(new JSONObject().put("id", j.getId()).put("nombre", j.getUsuario().getNombre())
                    .put("fechaNacimiento", j.getFechaNacimiento().toString()).put("correo", j.getCorreo())
                    .put("activo", j.estaActivo()).put("movimientosRanking", movimientos));
        }
        JSONArray instalaciones = new JSONArray();
        for (Instalacion i : club.getReservas().getInstalaciones()) {
            instalaciones.put(new JSONObject().put("id", i.getId()).put("nombre", i.getNombre())
                    .put("deporteId", i.getDeporte().getId()).put("capacidadMaxima", i.getCapacidadMaxima())
                    .put("techada", i.esTechada()));
        }
        JSONArray reservas = new JSONArray();
        for (Reserva r : club.getReservas().getReservas()) {
            reservas.put(new JSONObject().put("id", r.getId()).put("jugadorId", r.getJugador().getId())
                    .put("instalacionId", r.getInstalacion().getId()).put("inicio", r.getInicio().toString())
                    .put("fin", r.getFin().toString()).put("cantidadParticipantes", r.getCantidadParticipantes())
                    .put("modalidad", r.getModalidad()).put("cancelada", r.estaCancelada())
                    .put("cambiosAgenda", PersistenciaAcademiasAgenda.cambios(r)));
        }
        raiz.put("deportes", deportes).put("jugadores", jugadores).put("instalaciones", instalaciones).put("reservas",
                reservas);
        raiz.put("productos", salvarProductos(club));
        raiz.put("ventas", salvarVentas(club));
        JSONArray empleados = new JSONArray(), usuarios = new JSONArray(), saldos = new JSONArray(),
                sugerencias = new JSONArray();
        Map<String, Usuario> comerciales = new LinkedHashMap<>();
        for (Usuario u : club.getUsuarios()) {
            comerciales.put(u.getId(), u);
        }
        for (modelo.usuarios.Empleado e : club.getVentas().getEmpleados()) {
            comerciales.put(e.getId(), e.getUsuario());
            empleados.put(new JSONObject().put("id", e.getId()).put("nombre", e.getUsuario().getNombre()));
        }
        for (Venta v : club.getVentas().getVentasRegistradas()) {
            comerciales.put(v.getComprador().getId(), v.getComprador());
        }
        for (Usuario u : comerciales.values()) {
            Usuario registrado = club.buscarUsuario(u.getId());
            if (registrado != null && registrado != u) {
                throw new IllegalArgumentException("Comprador no canónico: " + u.getId());
            }

            if (club.buscarJugador(u.getId()) == null) {
                usuarios.put(new JSONObject().put("id", u.getId()).put("nombre", u.getNombre())
                        .put("identificado", u.esIdentificado()).put("activo", u.estaActivo()));
            }
        }
        for (Map.Entry<String, Long> e : club.getVentas().getSaldos().entrySet()) {
            saldos.put(new JSONObject().put("compradorId", e.getKey()).put("saldo", e.getValue()));
        }
        for (SugerenciaProducto sugerencia : club.getVentas().getSugerencias()) {
            sugerencias.put(new JSONObject().put("id", sugerencia.getId())
                    .put("autorCodigo", sugerencia.getAutor().getCodigoDescuento())
                    .put("producto", escribirProducto(sugerencia.getProducto()))
                    .put("ubicacion", sugerencia.getUbicacion()).put("cantidadInicial", sugerencia.getCantidadInicial())
                    .put("estado", sugerencia.getEstado().name()));
        }
        raiz.put("empleados", empleados).put("usuariosComerciales", usuarios).put("saldos", saldos)
                .put("ubicaciones", new JSONArray(club.getInventario().getUbicaciones()))
                .put("sugerencias", sugerencias);
        JSONArray cuentas = new JSONArray(), posesiones = new JSONArray(), usos = new JSONArray();
        for (gestores.GestorAcceso.Cuenta cuenta : club.getAcceso().getCuentas()) {
            if (club.buscarUsuario(cuenta.usuarioId()) == null) {
                throw new IllegalArgumentException("Cuenta sin usuario.");
            }
            JSONArray perfiles = new JSONArray();
            for (gestores.GestorAcceso.Perfil perfil : cuenta.perfiles()) {
                perfiles.put(perfil.name());
            }
            cuentas.put(new JSONObject().put("usuarioId", cuenta.usuarioId()).put("perfiles", perfiles)
                    .put("sal", cuenta.sal()).put("hash", cuenta.hash()).put("login", cuenta.login())
                    .put("activa", cuenta.activa()));
        }
        for (String id : club.getSeguridad().getPosesiones()) {
            if (club.buscarUsuario(id) == null) {
                throw new IllegalArgumentException("Posesión sin usuario.");
            }
            posesiones.put(id);
        }
        for (Map.Entry<String, String> uso : club.getSeguridad().getUsos().entrySet()) {
            usos.put(new JSONObject().put("usuarioId", uso.getKey()).put("reservaId", uso.getValue()));
        }
        club.getVentas().validarSaldos();
        raiz.put("cuentas", cuentas).put("posesionesCalientes", posesiones).put("usosInstalaciones", usos);
        PersistenciaModulosClub.salvar(raiz, club);
        PersistenciaDisciplina.salvar(raiz, club);
        PersistenciaAcademiasAgenda.salvar(raiz, club);
        PersistenciaTorneosInternos.salvar(raiz, club);
        Path destino = archivo.toAbsolutePath();
        Files.createDirectories(destino.getParent());
        Path temporal = Files.createTempFile(destino.getParent(), "club-", ".tmp");
        try {
            Files.writeString(temporal, raiz.toString(2), StandardCharsets.UTF_8);
            try {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private JSONObject escribirProducto(Producto p) {
        JSONObject j = new JSONObject().put("id", p.getId()).put("nombre", p.getNombre())
                .put("categoria", p.getCategoria()).put("precio", p.getPrecio());
        if (p instanceof Articulo) {
            j.put("tipo", "ARTICULO");
        } else if (p instanceof Bebida) {
            j.put("tipo", "BEBIDA").put("esCaliente", ((Bebida) p).esCaliente());
        } else if (p instanceof Snack) {
            Object alergenosJson;
            if (((Snack) p).tieneDeclaracionAlergenos()) {
                alergenosJson = new JSONArray(((Snack) p).getAlergenos());
            } else {
                alergenosJson = JSONObject.NULL;
            }
            j.put("tipo", "SNACK").put("alergenos", alergenosJson);
        } else {
            throw new IllegalArgumentException("Tipo de producto desconocido.");
        }
        return j;
    }

    private Producto leerProducto(JSONObject j) {
        String id = j.getString("id"), nombre = j.getString("nombre"), categoria = j.getString("categoria");
        double precio = j.getDouble("precio");
        switch (j.getString("tipo")) {
        case "ARTICULO":
            return new Articulo(id, nombre, categoria, precio);
        case "BEBIDA":
            return new Bebida(id, nombre, categoria, precio, j.getBoolean("esCaliente"));
        case "SNACK":
            List<String> alergenos = null;
            if (!j.isNull("alergenos")) {
                alergenos = new ArrayList<>();
                JSONArray lista = j.getJSONArray("alergenos");
                for (int k = 0; k < lista.length(); k++) {
                    alergenos.add(lista.getString(k));
                }
            }
            return new Snack(id, nombre, categoria, precio, alergenos);
        default:
            throw new IllegalArgumentException("Tipo de producto desconocido.");
        }
    }

    private JSONArray salvarProductos(EstadoClub club) {
        JSONArray arreglo = new JSONArray();
        for (Producto p : club.getInventario().getCatalogo()) {
            JSONObject j = escribirProducto(p);
            JSONArray existencias = new JSONArray();
            for (String u : club.getInventario().getUbicaciones()) {
                if (club.getInventario().buscarProducto(u, p.getId()) != null) {
                    existencias.put(new JSONObject().put("ubicacion", u).put("stock",
                            club.getInventario().consultarStock(u, p)));
                }
            }
            j.put("existencias", existencias);
            arreglo.put(j);
        }
        return arreglo;
    }

    private void cargarProductos(JSONArray arreglo, EstadoClub club) {
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject j = arreglo.getJSONObject(i);
            Producto p = leerProducto(j);
            if (club.getInventario().buscarProductoCatalogo(p.getId()) != null) {
                throw new IllegalArgumentException("Producto duplicado.");
            }

            if (j.has("existencias")) {
                JSONArray existencias = j.getJSONArray("existencias");
                if (existencias.length() == 0) {
                    throw new IllegalArgumentException("Producto sin ubicación.");
                }
                for (int k = 0; k < existencias.length(); k++) {
                    JSONObject e = existencias.getJSONObject(k);
                    club.getInventario().registrarProducto(e.getString("ubicacion"), p, e.getInt("stock"));
                }
            } else {
                club.getInventario().registrarProducto(p, j.getInt("stock"));
            }
        }
    }

    private JSONArray salvarVentas(EstadoClub club) {
        JSONArray arreglo = new JSONArray();
        for (Venta v : club.getVentas().getVentasRegistradas()) {
            JSONArray detalles = new JSONArray();
            for (DetalleVenta d : v.getDetalles()) {
                detalles.put(new JSONObject().put("productoId", d.getProducto().getId())
                        .put("cantidad", d.getCantidad()).put("precioUnitario", d.getPrecioUnitario()));
            }
            Object codigoDescuentoJson;
            if (v.getCodigoDescuento() == null) {
                codigoDescuentoJson = JSONObject.NULL;
            } else {
                codigoDescuentoJson = v.getCodigoDescuento();
            }
            arreglo.put(new JSONObject().put("id", v.getIdVenta()).put("compradorId", v.getComprador().getId())
                    .put("rubro", v.getRubro()).put("fecha", v.getFecha().toString())
                    .put("total", v.getValorFinalPagado()).put("propina", v.getPropina())
                    .put("ubicacion", v.getUbicacion()).put("codigoDescuento", codigoDescuentoJson)
                    .put("subtotal", v.getSubtotal()).put("descuento", v.getDescuento())
                    .put("impuesto", v.getImpuesto()).put("puntosRedimidos", v.getPuntosRedimidos())
                    .put("puntosGanados", v.getPuntosGanados()).put("detalles", detalles));
        }
        return arreglo;
    }

    private void cargarVentas(JSONArray arreglo, EstadoClub club, int version) {
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject j = arreglo.getJSONObject(i);
            if (version >= 2 && (!j.has("compradorId") || j.isNull("compradorId"))) {
                throw new IllegalArgumentException("Falta comprador en venta actual.");
            }
            String compradorId;
            if (j.has("compradorId") && !j.isNull("compradorId")) {
                compradorId = j.getString("compradorId");
            } else {
                compradorId = null;
            }
            Usuario comprador;
            if (compradorId == null) {
                comprador = club.compradorHistorico(j.getString("id"));
            } else {
                comprador = club.buscarUsuario(compradorId);
            }

            if (comprador == null) {
                throw new IllegalArgumentException("Comprador de venta inexistente: " + compradorId);
            }
            List<DetalleVenta> detalles = new ArrayList<>();
            JSONArray lista = j.getJSONArray("detalles");
            for (int k = 0; k < lista.length(); k++) {
                JSONObject d = lista.getJSONObject(k);
                Producto p = club.getInventario().buscarProductoCatalogo(d.getString("productoId"));
                if (p == null) {
                    throw new IllegalArgumentException("Producto de venta inexistente.");
                }
                detalles.add(new DetalleVenta(p, d.getInt("cantidad"), d.getDouble("precioUnitario")));
            }
            if (version >= 2 && !j.has("subtotal")) {
                throw new IllegalArgumentException("Falta desglose de venta actual.");
            }

            if (j.has("subtotal")) {
                String datoRegistrado;
                if (j.isNull("codigoDescuento")) {
                    datoRegistrado = null;
                } else {
                    datoRegistrado = j.getString("codigoDescuento");
                }
                Venta venta = new Venta(j.getString("id"), comprador, j.getString("rubro"),
                        LocalDate.parse(j.getString("fecha")), detalles, j.getString("ubicacion"), datoRegistrado,
                        j.getDouble("subtotal"), j.getDouble("descuento"), j.getDouble("impuesto"),
                        j.getDouble("propina"), j.getLong("puntosRedimidos"), j.getLong("puntosGanados"));
                if (Math.abs(venta.getValorFinalPagado() - j.getDouble("total")) > 0.001) {
                    throw new IllegalArgumentException("Total no coincide con desglose.");
                }
                club.getVentas().cargarVenta(venta);
            } else {
                club.getVentas().cargarVenta(new Venta(j.getString("id"), comprador, j.getString("rubro"),
                        LocalDate.parse(j.getString("fecha")), detalles, j.getDouble("total"), j.getDouble("propina")));
            }
        }
    }
}
