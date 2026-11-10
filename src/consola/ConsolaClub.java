package consola;

import aplicacion.EstadoClub;
import aplicacion.ResultadoOperacion;
import gestores.GestorAcceso;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import modelo.administracion.*;
import modelo.competencias.*;
import modelo.deportes.Deporte;
import modelo.deportes.Equipo;
import modelo.deportes.NivelAcademia;
import modelo.entrenamientos.*;
import modelo.partidos.*;
import modelo.reservas.*;
import modelo.socios.PagoMensualidad;
import modelo.usuarios.*;
import modelo.usuarios.Jugador;
import modelo.ventas.*;
import persistencia.PersistenciaClubJson;

public class ConsolaClub {

    private final Scanner entrada = new Scanner(System.in);
    private final PersistenciaClubJson persistencia = new PersistenciaClubJson();
    private final Path archivo;
    private EstadoClub club;
    private boolean pendiente;
    private Sesion sesion;

    private ConsolaClub(Path archivo) {
        this.archivo = archivo;
    }

    public static void main(String[] args) {
        Path archivo;
        if (args.length > 0) {
            archivo = Path.of(args[0]);
        } else {
            archivo = Path.of("datos", "club.json");
        }
        new ConsolaClub(archivo).ejecutar();
    }

    private void ejecutar() {
        try {
            club = persistencia.cargar(archivo);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("No se iniciará para evitar sobrescribir el archivo. Revísalo o usa otra ruta.");
            return;
        }
        System.out.println("CLUB DEPORTIVO SÉNECA");
        System.out.println("Archivo: " + archivo.toAbsolutePath());
        System.out.println("Fechas y horas de las reservas: hora local de Bogotá.");
        while (true) {
            Map<String, MenuPorRol.Opcion> opciones = MenuPorRol.opciones(club, sesion);
            if (club.getAcceso().estaAutenticada(sesion)) {
                System.out.println(
                        "\nUsuario: " + sesion.getUsuarioId() + " | Perfiles: " + club.getAcceso().getPerfiles(sesion));
            }

            if (club.getAcceso().getPerfiles(sesion).equals(Set.of(Perfil.BASE))) {
                System.out.println("Cuenta base: pendiente de asignación de perfiles por el administrador.");
            }
            String grupo = "";
            for (var opcion : opciones.entrySet()) {
                if (!grupo.equals(opcion.getValue().grupo())) {
                    grupo = opcion.getValue().grupo();
                    System.out.println("\n" + grupo);
                }
                System.out.println(opcion.getKey() + ". " + opcion.getValue().etiqueta());
            }
            String opcion = leer("Opción");
            if (opcion == null) {
                if (pendiente) {
                    guardar();
                }
                return;
            }
            try {
                exigir(opciones.containsKey(opcion), "Opción no disponible para tu sesión y perfiles.");
                switch (opcion) {
                case "1":
                    registrarJugador();
                    break;
                case "2":
                    listarJugadores();
                    break;
                case "3":
                    registrarInstalacion();
                    break;
                case "4":
                    listarInstalaciones();
                    break;
                case "5":
                    reservar();
                    break;
                case "6":
                    listarReservas();
                    break;
                case "7":
                    guardar();
                    break;
                case "8":
                    registrarProducto();
                    break;
                case "9":
                    listarInventario();
                    break;
                case "10":
                    reabastecer();
                    break;
                case "11":
                    vender();
                    break;
                case "12":
                    listarVentas();
                    break;
                case "13":
                    iniciarSesion();
                    break;
                case "14":
                    club.getAcceso().cerrarSesion(sesion);
                    sesion = null;
                    System.out.println("Sesión cerrada.");
                    break;
                case "15":
                    crearAdministrador();
                    break;
                case "16":
                    registrarEmpleado();
                    break;
                case "17":
                    registrarUsuarioComercial();
                    break;
                case "18":
                    crearPerfiles();
                    break;
                case "19":
                    registrarUbicacion();
                    break;
                case "20":
                    trasladar();
                    break;
                case "21":
                    sugerir();
                    break;
                case "22":
                    resolverSugerencia();
                    break;
                case "23":
                    consultarSugerencias();
                    break;
                case "24":
                    completarAlergenos();
                    break;
                case "25":
                    ingresar();
                    break;
                case "26":
                    terminarUso();
                    break;
                case "27":
                    terminarBebida();
                    break;
                case "28":
                    listarCodigos();
                    break;
                case "29":
                    registrarMensualidad();
                    break;
                case "30":
                    consultarMensualidades();
                    break;
                case "31":
                    registrarEquipo();
                    break;
                case "32":
                    listarEquipos();
                    break;
                case "33":
                    inscribirJugador();
                    break;
                case "34":
                    programarEntrenamiento();
                    break;
                case "35":
                    listarEntrenamientos();
                    break;
                case "36":
                    convocar();
                    break;
                case "37":
                    registrarAsistencia();
                    break;
                case "38":
                    actualizarRanking();
                    break;
                case "39":
                    reportes();
                    break;
                case "40":
                    configurarHorario();
                    break;
                case "41":
                    registrarTurno();
                    break;
                case "42":
                    consultarTurnos();
                    break;
                case "43":
                    solicitarCambioTurno();
                    break;
                case "44":
                    consultarSolicitudesTurno();
                    break;
                case "45":
                    resolverCambioTurno();
                    break;
                case "46":
                    consultarCobertura();
                    break;
                case "47":
                    cambiarEstadoUsuario();
                    break;
                case "48":
                    restablecerClave();
                    break;
                case "49":
                    registrarCompetencia();
                    break;
                case "50":
                    registrarNomina();
                    break;
                case "51":
                    programarPartido();
                    break;
                case "52":
                    registrarResultado();
                    break;
                case "53":
                    aplicar(club.getOperaciones().procesarTablaLiga(sesion, pedir("ID de partido oficial")));
                    break;
                case "54":
                    registrarMetrica();
                    break;
                case "55":
                    registrarTarjeta();
                    break;
                case "56":
                    actualizarCuenta();
                    break;
                case "57":
                    habilitarCuenta(sesion);
                    break;
                case "58":
                    consultarHabilitados();
                    break;
                case "59":
                    confirmarAlineacion();
                    break;
                case "60":
                    aplicar(club.getOperaciones().cerrarActa(sesion, pedir("ID del partido con acta completa")));
                    break;
                case "61":
                    aplicar(club.getOperaciones().cancelarPartido(sesion, pedir("ID del partido sin resultado")));
                    break;
                case "62":
                    retirarJugador();
                    break;
                case "63":
                    aplicar(club.getOperaciones().cambiarEstadoEquipo(sesion, pedir("ID de equipo"),
                            preguntarSi("¿Activar? (s reactiva/n da de baja)")));
                    break;
                case "64":
                    consultarDisciplina();
                    break;
                case "65":
                    academia();
                    break;
                case "66":
                    torneos();
                    break;
                case "67":
                    agenda();
                    break;
                case "68":
                    acumulados();
                    break;
                case "69":
                    if (new ConsolaTorneosInternos(club, sesion, entrada).ejecutar()) {
                        pendiente = true;
                    }
                    break;
                case "0":
                    if (!pendiente || guardar()) {
                        return;
                    }
                    break;
                default:
                    System.out.println("Selecciona una opción del menú.");
                }
            } catch (NoSuchElementException e) {
                if (pendiente) {
                    guardar();
                }
                return;
            } catch (IllegalArgumentException | DateTimeException e) {
                System.out.println("No se realizó la operación: " + e.getMessage());
            }
        }
    }

    private String leer(String mensaje) {
        System.out.print(mensaje + ": ");
        if (entrada.hasNextLine()) {
            return entrada.nextLine().trim();
        } else {
            return null;
        }
    }

    private String pedir(String mensaje) {
        String texto = leer(mensaje);
        if (texto == null) {
            throw new NoSuchElementException();
        }

        if (texto.isBlank()) {
            throw new IllegalArgumentException("El campo es obligatorio.");
        }
        return texto;
    }

    private void registrarJugador() {
        String id = pedir("ID del jugador");
        String nombre = pedir("Nombre");
        LocalDate nacimiento = LocalDate.parse(pedir("Nacimiento (AAAA-MM-DD)"));
        aplicar(club.getOperaciones().registrarJugador(sesion, id, nombre, nacimiento));
    }

    private void listarJugadores() {
        if (club.getJugadores().isEmpty()) {
            System.out.println("No hay jugadores registrados.");
        }
        for (Jugador j : club.getJugadores()) {
            System.out.println(
                    j.getId() + " | " + j.getUsuario().getNombre() + " | nacimiento: " + j.getFechaNacimiento());
        }
    }

    private void registrarInstalacion() {
        String id = pedir("ID de la instalación");
        String nombre = pedir("Nombre");
        System.out.println("Deportes: FUTBOL, BALONCESTO, VOLEIBOL, TENIS, PADEL, MESA");
        Deporte deporte = club.buscarDeporte(pedir("Código del deporte").toUpperCase(Locale.ROOT));
        if (deporte == null) {
            throw new IllegalArgumentException("Deporte desconocido.");
        }
        int capacidad = Integer.parseInt(pedir("Capacidad máxima (2 o 4; pádel: 4)"));
        if (capacidad <= 0 || (!deporte.isColectivo()
                && ((capacidad != 2 && capacidad != 4) || (deporte.getId().equals("PADEL") && capacidad != 4)))) {
            throw new IllegalArgumentException("Capacidad incompatible con el deporte.");
        }
        boolean techada = preguntarSi("¿La instalación es techada? (s/n)");
        if (!club.getOperaciones()
                .registrarInstalacion(sesion, new Instalacion(id, nombre, deporte, capacidad, techada)).exito()) {
            throw new IllegalArgumentException("Ya existe esa instalación.");
        }
        cambio("Instalación registrada.");
    }

    private void listarInstalaciones() {
        if (club.getReservas().getInstalaciones().isEmpty()) {
            System.out.println("No hay instalaciones.");
        }
        for (Instalacion i : club.getReservas().getInstalaciones()) {
            System.out.println(i.getId() + " | " + i.getNombre() + " | " + i.getDeporte().getNombre() + " | capacidad: "
                    + i.getCapacidadMaxima());
        }
    }

    private void reservar() {
        String id = pedir("ID de la reserva");
        String jugadorIdReserva;
        if (club.getOperaciones().esCaja(sesion)) {
            jugadorIdReserva = pedir("ID del jugador");
        } else {
            jugadorIdReserva = sesion.getUsuarioId();
        }
        Jugador jugador = club.buscarJugador(jugadorIdReserva);
        Instalacion instalacion = club.getReservas().buscarInstalacion(pedir("ID de la instalación"));
        if (jugador == null || instalacion == null) {
            throw new IllegalArgumentException("Primero registra el jugador y la instalación.");
        }
        LocalDateTime inicio = LocalDateTime.parse(pedir("Inicio (AAAA-MM-DDTHH:MM)"));
        LocalDateTime fin = LocalDateTime.parse(pedir("Fin (AAAA-MM-DDTHH:MM)"));
        String modalidad = pedir("Modalidad (Sencillos o Dobles)");
        int cantidad;
        if (modalidad.equalsIgnoreCase("Sencillos")) {
            cantidad = 2;
        } else if (modalidad.equalsIgnoreCase("Dobles")) {
            cantidad = 4;
        } else {
            throw new IllegalArgumentException("Modalidad desconocida.");
        }

        if (!club.getOperaciones()
                .reservar(sesion, id, jugador.getId(), instalacion.getId(), inicio, fin, cantidad, modalidad).exito()) {
            throw new IllegalArgumentException("Reserva rechazada: ID repetido, horario inválido/ocupado,"
                    + " capacidad insuficiente o modalidad incompatible (pádel solo dobles).");
        }
        cambio("Reserva creada.");
    }

    private void listarReservas() {
        List<Reserva> reservas = new ArrayList<>(club.getOperaciones().consultarReservas(sesion));
        reservas.sort(Comparator.comparing(Reserva::getInicio));
        if (reservas.isEmpty()) {
            System.out.println("No hay reservas.");
        }
        for (Reserva r : reservas) {
            System.out.println(
                    r.getId() + " | " + r.getJugador().getUsuario().getNombre() + " | " + r.getInstalacion().getNombre()
                            + " | " + r.getInicio() + " → " + r.getFin() + " | " + r.getModalidad());
        }
    }

    private void cambio(String mensaje) {
        pendiente = true;
        System.out.println(mensaje);
        guardar();
    }

    private void registrarProducto() {
        exigirAdministrador();
        String ubicacion = pedirUbicacion();
        int stock = Integer.parseInt(pedir("Existencias iniciales"));
        Producto producto = leerProducto();
        exigir(club.getVentas().registrarProducto(sesion, ubicacion, producto, stock),
                "Producto inválido, duplicado o sin declaración de alérgenos.");
        cambio("Producto registrado.");
    }

    private Producto leerProducto() {
        String id = pedir("ID del producto");
        if (id.equals("0")) {
            throw new IllegalArgumentException("El ID 0 está reservado para terminar la venta.");
        }

        if (club.getInventario().buscarProductoCatalogo(id) != null) {
            throw new IllegalArgumentException("Ya existe ese producto.");
        }
        String nombre = pedir("Nombre");
        String categoria = pedir("Categoría");
        double precio = Double.parseDouble(pedir("Precio en pesos (usar punto para decimales)"));
        String tipo = pedir("Tipo: 1 artículo, 2 bebida, 3 snack");
        Producto producto;
        switch (tipo) {
        case "1":
            producto = new Articulo(id, nombre, categoria, precio);
            break;
        case "2":
            producto = new Bebida(id, nombre, categoria, precio, preguntarSi("¿Es caliente? (s/n)"));
            break;
        case "3":
            producto = new Snack(id, nombre, categoria, precio, leerAlergenos());
            break;
        default:
            throw new IllegalArgumentException("Tipo de producto desconocido.");
        }
        return producto;
    }

    private boolean preguntarSi(String mensaje) {
        String respuesta = pedir(mensaje);
        if (respuesta.equalsIgnoreCase("s")) {
            return true;
        }

        if (respuesta.equalsIgnoreCase("n")) {
            return false;
        }
        throw new IllegalArgumentException("Responde s o n.");
    }

    private void listarInventario() {
        listarInventario(pedirUbicacion());
    }

    private void listarInventario(String ubicacion) {
        List<Producto> productos = club.getInventario().getProductos(ubicacion);
        productos.sort(Comparator.comparing(Producto::getId));
        if (productos.isEmpty()) {
            System.out.println("No hay productos registrados.");
        }
        for (Producto p : productos) {
            System.out.printf(Locale.ROOT, "%s | %s | %s | $%.2f | existencias: %d%n", p.getId(), p.getNombre(),
                    p.getRubro(), p.getPrecio(), club.getInventario().consultarStock(ubicacion, p));
        }
    }

    private void reabastecer() {
        exigirOperador();
        String ubicacion = pedirUbicacion();
        Producto p = club.getInventario().buscarProducto(ubicacion, pedir("ID del producto"));
        if (p == null) {
            throw new IllegalArgumentException("El producto no existe.");
        }
        int cantidad = Integer.parseInt(pedir("Cantidad que ingresa"));
        exigir(club.getVentas().reabastecer(sesion, ubicacion, p, cantidad), "Reposición inválida o sin permiso.");
        cambio("Existencias actualizadas.");
    }

    private void vender() {
        exigirOperador();
        String ubicacion = pedirUbicacion();
        if (club.getInventario().getProductos(ubicacion).isEmpty()) {
            throw new IllegalArgumentException("Primero registra productos.");
        }
        String id = pedir("ID de la venta");
        if (club.getVentas().buscarVenta(id) != null) {
            throw new IllegalArgumentException("Ya existe esa venta.");
        }
        String rubro = pedir("Rubro (TIENDA o CAFETERIA)").toUpperCase(Locale.ROOT);
        if (!rubro.equals("TIENDA") && !rubro.equals("CAFETERIA")) {
            throw new IllegalArgumentException("Rubro desconocido.");
        }
        listarInventario(ubicacion);
        List<DetalleVenta> detalles = new ArrayList<>();
        while (true) {
            String productoId = pedir("ID del producto (0 para terminar)");
            if (productoId.equals("0")) {
                break;
            }
            Producto p = club.getInventario().buscarProducto(ubicacion, productoId);
            if (p == null || !p.getRubro().equals(rubro)) {
                throw new IllegalArgumentException("Producto inexistente o de otro rubro. Venta cancelada.");
            }
            int cantidad = Integer.parseInt(pedir("Cantidad"));
            detalles.add(new DetalleVenta(p, cantidad));
        }
        for (DetalleVenta d : detalles) {
            if (d.getProducto() instanceof Snack) {
                Snack snack = (Snack) d.getProducto();
                Object alergenosMostrados;
                if (snack.tieneDeclaracionAlergenos()) {
                    if (snack.getAlergenos().isEmpty()) {
                        alergenosMostrados = "ninguno declarado";
                    } else {
                        alergenosMostrados = snack.getAlergenos();
                    }
                } else {
                    alergenosMostrados = "información no suministrada";
                }
                System.out.println(snack.getNombre() + " | Alérgenos: " + (alergenosMostrados));
            }
        }
        modelo.usuarios.Usuario comprador = club.buscarUsuario(pedir("ID del comprador registrado"));
        if (comprador == null) {
            throw new IllegalArgumentException("El comprador no está registrado.");
        }
        String codigo = pedir("Código de descuento (0 para ninguno)");
        if (codigo.equals("0")) {
            codigo = null;
        }
        double subtotal = Dinero.subtotal(detalles);
        double tasa = 0;
        if (codigo != null) {
            modelo.usuarios.Empleado titular = club.getVentas().buscarEmpleadoPorCodigo(codigo);
            if (titular == null || !rubro.equals("TIENDA")) {
                throw new IllegalArgumentException("Código inválido para esta venta.");
            }

            if (titular.getId().equals(comprador.getId())) {
                tasa = 0.15;
            } else {
                tasa = 0.08;
            }
        }
        System.out.printf(Locale.ROOT, "Propina sugerida (10 %% de base sin impuestos): $%.2f%n", Dinero
                .porcentaje(Dinero.redondear(subtotal - Dinero.porcentaje(subtotal, Double.toString(tasa))), "0.10"));
        double propina = Double.parseDouble(pedir("Propina en pesos (0 si no aplica)"));
        System.out.println("Saldo de puntos: " + club.getVentas().consultarPuntos(comprador.getId()));
        long puntos = Long.parseLong(pedir("Puntos a redimir (0 para ninguno)"));
        if (!pedir("Confirmar compra (SI/NO)").equalsIgnoreCase("SI")) {
            return;
        }
        boolean registrada = club.getVentas().procesarVenta(sesion, id, comprador, rubro,
                LocalDate.now(ZoneId.of("America/Bogota")), detalles, codigo, propina, puntos, ubicacion);
        if (!registrada) {
            throw new IllegalArgumentException("Venta rechazada: sin productos, existencias insuficientes"
                    + " , código compartido sin socio, bebida caliente durante uso restringido o valores inválidos. No se descontó inventario.");
        }
        System.out.printf(Locale.ROOT, "Total pagado: $%.2f%n", club.getVentas().buscarVenta(id).getValorFinalPagado());
        Venta recibo = club.getVentas().buscarVenta(id);
        System.out.printf(Locale.ROOT,
                "Subtotal: %.2f | Descuento: %.2f | Base: %.2f | Impuesto: %.2f | Propina: %.2f | Puntos: -%d +%d%n",
                recibo.getSubtotal(), recibo.getDescuento(), recibo.getBaseImponible(), recibo.getImpuesto(),
                recibo.getPropina(), recibo.getPuntosRedimidos(), recibo.getPuntosGanados());
        cambio("Venta registrada.");
    }

    private void listarVentas() {
        List<Venta> ventas = club.getVentas().getVentasRegistradas();
        if (ventas.isEmpty()) {
            System.out.println("No hay ventas registradas.");
        }
        double total = 0;
        for (Venta v : ventas) {
            System.out.printf(Locale.ROOT, "%s | %s | %s | total: $%.2f | propina: $%.2f%n", v.getIdVenta(),
                    v.getFecha(), v.getRubro(), v.getValorFinalPagado(), v.getPropina());
            for (DetalleVenta d : v.getDetalles()) {
                System.out.printf(Locale.ROOT, "  %s x %d | unitario: $%.2f | subtotal: $%.2f%n",
                        d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
            }
            total += v.getValorFinalPagado();
        }
        System.out.printf(Locale.ROOT, "Total registrado (incluye impuestos y propinas): $%.2f%n", total);
    }

    private void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private void exigirAdministrador() {
        exigir(club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR), "Debes iniciar sesión como administrador.");
    }

    private void exigirOperador() {
        exigir(club.getOperaciones().esCaja(sesion), "Debes iniciar sesión como empleado de caja o administrador.");
    }

    private void iniciarSesion() {
        String id = pedir("Login de cuenta");
        String clave = leerClave();
        Sesion nueva = club.getAcceso().autenticar(id, clave);
        exigir(nueva != null, "Credenciales inválidas.");
        club.getAcceso().cerrarSesion(sesion);
        sesion = nueva;
        System.out.println("Sesión iniciada: " + id);
    }

    private String leerClave() {
        if (System.console() != null) {
            char[] clave = System.console().readPassword("Clave (mínimo 8 caracteres): ");
            if (clave == null) {
                throw new NoSuchElementException();
            }
            String resultado = new String(clave);
            Arrays.fill(clave, '\0');
            return resultado;
        }
        return pedir("Clave (mínimo 8 caracteres; visible en esta terminal)");
    }

    private void crearAdministrador() {
        exigir(!club.getAcceso().tieneAdministrador(), "Ya existe administrador. Inicia sesión.");
        String id = pedir("ID del nuevo administrador");
        String nombre = pedir("Nombre");
        exigir(club.crearAdministradorInicial(id, nombre, leerClave()), "Datos inválidos o ID ya utilizado.");
        cambio("Administrador creado. Inicia sesión en la opción 13.");
    }

    private void registrarEmpleado() {
        exigirAdministrador();
        String id = pedir("ID del empleado"), nombre = pedir("Nombre");
        exigir(club.altaEmpleado(sesion, id, nombre), "Empleado duplicado o inválido.");
        cambio("Empleado registrado. Código: EMP-" + id + ". Asigna sus perfiles con la opción 57.");
    }

    private void registrarUsuarioComercial() {
        exigirAdministrador();
        exigir(club.altaUsuario(sesion, pedir("ID del usuario"), pedir("Nombre")), "Usuario inválido o duplicado.");
        cambio("Usuario registrado. Para ser socio, asigna el perfil SOCIO en la opción 57.");
    }

    private void crearPerfiles() {
        String id = pedir("ID del nuevo usuario"), nombre = pedir("Nombre"), login = pedir("Login de acceso");
        exigir(club.registrarCuentaBase(id, nombre, login, leerClave()),
                "ID/login ya registrado o datos inválidos. Usa un ID nuevo y clave de al menos 8 caracteres.");
        cambio("Cuenta base creada. Puedes iniciar sesión; la asignación de perfiles corresponde al administrador.");
    }

    private void habilitarCuenta(Sesion administrador) {
        exigir(club.getAcceso().tienePerfil(administrador, Perfil.ADMINISTRADOR), "Se requiere administrador.");
        club.getAcceso().getCuentas().stream().filter(c -> c.perfiles().equals(Set.of(Perfil.BASE)))
                .forEach(c -> System.out.println("Pendiente de asignación: " + c.usuarioId() + " | "
                        + club.buscarUsuario(c.usuarioId()).getNombre()));
        String id = pedir("ID del usuario existente");
        GestorAcceso.Cuenta existente = club.getAcceso().getCuentas().stream().filter(c -> c.usuarioId().equals(id))
                .findFirst().orElse(null);
        String login;
        if (existente == null) {
            login = pedir("Login de acceso (0 para usar ID)");
        } else {
            login = existente.login();
        }

        if (login.equals("0")) {
            login = id;
        }
        Set<Perfil> perfiles = EnumSet.noneOf(Perfil.class);
        for (String texto : pedir(
                "Perfiles separados por coma: SOCIO, EMPLEADO, ENTRENADOR, FISIOTERAPEUTA, ADMINISTRADOR").split(",")) {
            perfiles.add(Perfil.valueOf(texto.trim().toUpperCase(Locale.ROOT)));
        }
        boolean asignada;
        if (existente == null) {
            asignada = club.habilitarCuenta(administrador, id, login, leerClave(), perfiles);
        } else {
            asignada = club.actualizarCuenta(administrador, id, login, perfiles);
        }
        exigir(asignada, "Usuario inválido, perfiles incompatibles o empleado sin registrar.");
        cambio("Perfiles asignados. La cuenta conserva sus credenciales si ya existía; inicia sesión de nuevo.");
    }

    private String pedirUbicacion() {
        System.out.println("Ubicaciones: " + club.getInventario().getUbicaciones());
        String ubicacion = pedir("Ubicación");
        exigir(club.getInventario().getUbicaciones().contains(ubicacion), "Ubicación inexistente.");
        return ubicacion;
    }

    private void registrarUbicacion() {
        exigirAdministrador();
        exigir(club.getVentas().registrarUbicacion(sesion, pedir("ID de nueva ubicación")),
                "Ubicación inválida o repetida.");
        cambio("Ubicación registrada.");
    }

    private void trasladar() {
        exigirAdministrador();
        String id = pedir("ID del producto");
        String origen = pedirUbicacion(), destino = pedirUbicacion();
        exigir(club.getVentas().trasladarProducto(sesion, id, origen, destino),
                "No existe en origen o ya existe en destino. Sin cambios.");
        cambio("Catálogo y existencias trasladados completamente.");
    }

    private List<String> leerAlergenos() {
        String texto = pedir("Alérgenos separados por coma, ninguno, o pendiente si no se conocen");
        if (texto.equalsIgnoreCase("pendiente")) {
            return null;
        }

        if (texto.equalsIgnoreCase("ninguno")) {
            return List.of();
        }
        List<String> resultado = new ArrayList<>();
        for (String alergeno : texto.split(",", -1)) {
            exigir(!alergeno.isBlank(), "Alérgeno vacío. Declara ninguno o pendiente explícitamente.");
            resultado.add(alergeno.trim());
        }
        return resultado;
    }

    private void sugerir() {
        exigir(club.getAcceso().tienePerfil(sesion, Perfil.EMPLEADO), "Solo un empleado puede sugerir productos.");
        String id = pedir("ID de la sugerencia"), ubicacion = pedirUbicacion();
        int cantidad = Integer.parseInt(pedir("Existencias iniciales para aprobación"));
        Producto producto = leerProducto();
        exigir(club.getVentas().sugerirProducto(sesion, id,
                club.getVentas().buscarEmpleadoPorCodigo("EMP-" + sesion.getUsuarioId()), producto, ubicacion,
                cantidad), "Sugerencia inválida o duplicada.");
        cambio("Sugerencia pendiente registrada.");
    }

    private void resolverSugerencia() {
        exigirAdministrador();
        String id = pedir("ID de la sugerencia");
        exigir(club.getVentas().resolverSugerencia(sesion, id, preguntarSi("¿Aprobar? (s aprueba/n rechaza)")),
                "No existe, ya fue resuelta, falta declaración de alérgenos o producto/stock/ubicación inválidos.");
        cambio("Decisión registrada.");
    }

    private void consultarSugerencias() {
        exigir(club.getAcceso().estaAutenticada(sesion), "Inicia sesión.");
        String autor;
        if (club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR)) {
            autor = pedir("ID del empleado autor");
        } else {
            autor = sesion.getUsuarioId();
        }
        List<SugerenciaProducto> lista = club.getVentas().consultarSugerencias(sesion, autor);
        if (lista.isEmpty()) {
            System.out.println("No hay sugerencias visibles para esa cuenta.");
        }
        for (SugerenciaProducto sugerencia : lista) {
            Producto producto = sugerencia.getProducto();
            System.out.println(sugerencia.getId() + " | " + producto.getNombre() + " | " + sugerencia.getUbicacion()
                    + " | " + sugerencia.getEstado() + " | autor: " + sugerencia.getAutor().getId() + " | precio: "
                    + producto.getPrecio() + " | cantidad: " + sugerencia.getCantidadInicial());
            if (producto instanceof Snack) {
                Snack snack = (Snack) producto;
                Object alergenosMostrados2;
                if (snack.tieneDeclaracionAlergenos()) {
                    alergenosMostrados2 = snack.getAlergenos();
                } else {
                    alergenosMostrados2 = "información no suministrada";
                }
                System.out.println("Alérgenos: " + (alergenosMostrados2));
            }
        }
    }

    private void completarAlergenos() {
        exigir(club.getAcceso().tienePerfil(sesion, Perfil.EMPLEADO), "Inicia sesión como empleado autor.");
        exigir(club.getVentas().completarAlergenosSugerencia(sesion, pedir("ID de sugerencia pendiente"),
                club.getVentas().buscarEmpleadoPorCodigo("EMP-" + sesion.getUsuarioId()), leerAlergenos()),
                "No es un snack pendiente del autor o falta declaración.");
        cambio("Declaración completada.");
    }

    private void ingresar() {
        String reservaId = pedir("ID de reserva para ingresar ahora");
        String usuarioId = pedir("ID de quien ingresa (0 para el titular; acompañantes los registra el personal)");
        boolean ingreso;
        if (usuarioId.equals("0")) {
            ingreso = club.getSeguridad().iniciarUso(sesion, reservaId);
        } else {
            ingreso = club.getSeguridad().iniciarUso(sesion, reservaId, usuarioId);
        }
        exigir(ingreso,
                "Ingreso rechazado: sin permiso, fuera de horario, ya está usando una instalación o lleva bebida caliente a instalación restringida.");
        cambio("Ingreso registrado.");
    }

    private String usuarioDeSeguridad(String mensaje) {
        if (club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR)
                || club.getAcceso().tienePerfil(sesion, Perfil.EMPLEADO)) {
            return pedir(mensaje);
        } else {
            return sesion.getUsuarioId();
        }
    }

    private void terminarUso() {
        exigir(club.getSeguridad().finalizarUso(sesion, usuarioDeSeguridad("ID del usuario que sale")),
                "Sin permiso o sin ingreso registrado.");
        cambio("Uso finalizado.");
    }

    private void terminarBebida() {
        exigir(club.getSeguridad().finalizarBebida(sesion,
                usuarioDeSeguridad("ID del usuario que consumió o entregó su bebida")),
                "Sin permiso o sin bebida caliente registrada.");
        cambio("Posesión de bebida caliente finalizada.");
    }

    private void listarCodigos() {
        exigir(club.getAcceso().tienePerfil(sesion, Perfil.ADMINISTRADOR)
                || club.getAcceso().tienePerfil(sesion, Perfil.EMPLEADO), "Solo personal autenticado.");
        for (modelo.usuarios.Empleado empleado : club.getVentas().getEmpleados()) {
            System.out.println(empleado.getId() + " | " + empleado.getUsuario().getNombre() + " | "
                    + empleado.getCodigoDescuento());
        }
    }

    private void aplicar(ResultadoOperacion resultado) {
        exigir(resultado.exito(), resultado.mensaje());
        cambio(resultado.mensaje());
    }

    private void registrarMensualidad() {
        String id = pedir("ID del pago");
        Jugador jugador = club.buscarJugador(pedir("ID del socio"));
        double importe = Double.parseDouble(pedir("Importe recibido"));
        LocalDate fecha = LocalDate.parse(pedir("Fecha de pago (AAAA-MM-DD)"));
        aplicar(club.getOperaciones().registrarPago(sesion,
                new PagoMensualidad(id, jugador, importe, fecha, pedir("Mensualidad correspondiente (AAAA-MM)"))));
    }

    private void consultarMensualidades() {
        for (PagoMensualidad p : club.getOperaciones().consultarPagos(sesion)) {
            System.out.println(p.getIdPago() + " | " + p.getJugador().getId() + " | " + p.getPeriodo() + " | "
                    + p.getFechaPago() + " | $" + p.getImporteRecibido());
        }
    }

    private void registrarEquipo() {
        String id = pedir("ID de equipo"), nombre = pedir("Nombre"),
                deporte = pedir("Deporte: FUTBOL, BALONCESTO o VOLEIBOL");
        String categoria = pedir("Categoría: Sub-13, Sub-15, Sub-17, Mayores");
        int cupo = Integer.parseInt(pedir("Cupo máximo"));
        aplicar(club.getOperaciones().registrarEquipo(sesion,
                new Equipo(id, nombre, club.buscarDeporte(deporte), categoria, cupo)));
    }

    private void listarEquipos() {
        for (Equipo equipo : club.getEquipos().getEquipos()) {
            System.out.println(equipo.getId() + " | " + equipo.getNombre() + " | " + equipo.getDeporte().getNombre()
                    + " | " + equipo.getCategoria() + " | inscritos: " + equipo.getCantidadInscritos() + "/"
                    + equipo.getCupoMaximo());
        }
    }

    private void inscribirJugador() {
        String jugador;
        if (club.getOperaciones().esAdministrador(sesion) || club.getOperaciones().esEntrenador(sesion)) {
            jugador = pedir("ID del jugador");
        } else {
            jugador = sesion.getUsuarioId();
        }
        String equipo = pedir("ID del equipo"), posicion = pedir("Posición");
        int dorsal = Integer.parseInt(pedir("Dorsal"));
        ResultadoOperacion resultado = club.getOperaciones().inscribir(sesion, jugador, equipo, posicion, dorsal);
        while (!resultado.exito() && resultado.mensaje().startsWith("Dorsal ocupado")) {
            System.out.println(resultado.mensaje());
            dorsal = Integer.parseInt(pedir("Otro dorsal (0 cancela)"));
            if (dorsal == 0) {
                return;
            }
            resultado = club.getOperaciones().inscribir(sesion, jugador, equipo, posicion, dorsal);
        }
        aplicar(resultado);
    }

    private void programarEntrenamiento() {
        String id = pedir("ID del entrenamiento"), equipo = pedir("ID del equipo"),
                instalacion = pedir("ID de instalación");
        String entrenador;
        if (club.getOperaciones().esAdministrador(sesion)) {
            entrenador = pedir("ID del entrenador");
        } else {
            entrenador = sesion.getUsuarioId();
        }
        LocalDateTime inicio = LocalDateTime.parse(pedir("Inicio (AAAA-MM-DDTHH:MM)"));
        int duracion = Integer.parseInt(pedir("Duración en minutos"));
        aplicar(club.getOperaciones()
                .registrarEntrenamiento(sesion, new Entrenamiento(id, inicio, duracion,
                        club.getVentas().buscarEmpleadoPorCodigo("EMP-" + entrenador),
                        club.getReservas().buscarInstalacion(instalacion), club.getEquipos().buscarEquipo(equipo))));
    }

    private void listarEntrenamientos() {
        for (Entrenamiento e : club.getEntrenamientos().getEntrenamientos()) {
            boolean visible = club.getOperaciones().esAdministrador(sesion)
                    || (club.getOperaciones().esEntrenador(sesion)
                            && e.getEntrenador().getId().equals(sesion.getUsuarioId()))
                    || e.getConvocados().stream().anyMatch(j -> j.getId().equals(sesion.getUsuarioId()));
            if (visible) {
                String equipoMostrado;
                if (e.getEquipo() == null) {
                    equipoMostrado = "sin equipo";
                } else {
                    equipoMostrado = e.getEquipo().getId();
                }
                System.out.println(e.getId() + " | " + e.getInicio() + " | " + e.getInstalacion().getNombre()
                        + " | equipo: " + equipoMostrado);
            }
        }
    }

    private void convocar() {
        aplicar(club.getOperaciones().convocar(sesion, pedir("ID de entrenamiento"), pedir("ID de jugador")));
    }

    private void registrarAsistencia() {
        String id = pedir("ID de entrenamiento"), jugador = pedir("ID de jugador");
        System.out.println("Estados: " + Arrays.toString(EstadoAsistencia.values()));
        EstadoAsistencia estado = EstadoAsistencia.valueOf(pedir("Estado").toUpperCase(Locale.ROOT));
        String texto = pedir("Calificación de 1 a 5, o ninguna");
        Double nota;
        if (texto.equalsIgnoreCase("ninguna")) {
            nota = null;
        } else {
            nota = Double.valueOf(texto);
        }
        String comentario = pedir("Comentario (0 para ninguno)");
        String comentarioGuardado;
        if (comentario.equals("0")) {
            comentarioGuardado = "";
        } else {
            comentarioGuardado = comentario;
        }
        aplicar(club.getOperaciones().registrarAsistencia(sesion, id, jugador, estado, nota, comentarioGuardado));
    }

    private void actualizarRanking() {
        String jugador = pedir("ID de jugador"), deporte = pedir("Deporte");
        double valor = Double.parseDouble(pedir("Nuevo ranking"));
        LocalDate fecha = LocalDate.parse(pedir("Fecha (AAAA-MM-DD)"));
        String referencia = pedir("Referencia única del movimiento");
        aplicar(club.getOperaciones().actualizarRanking(sesion, jugador, deporte, valor, fecha, referencia));
    }

    private LocalDate[] pedirPeriodo() {
        String modo = pedir("Periodo: D diario, S semanal, M mensual, R rango").toUpperCase(Locale.ROOT);
        if (modo.equals("R")) {
            return new LocalDate[] { LocalDate.parse(pedir("Desde (AAAA-MM-DD)")),
                    LocalDate.parse(pedir("Hasta (AAAA-MM-DD)")), };
        }
        LocalDate fecha = LocalDate.parse(pedir("Fecha de referencia (AAAA-MM-DD)"));
        if (modo.equals("D")) {
            return new LocalDate[] { fecha, fecha };
        }

        if (modo.equals("S")) {
            LocalDate inicio = fecha.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new LocalDate[] { inicio, inicio.plusDays(6) };
        }
        exigir(modo.equals("M"), "Periodo desconocido.");
        return new LocalDate[] { fecha.withDayOfMonth(1), fecha.withDayOfMonth(fecha.lengthOfMonth()), };
    }

    private void reportes() {
        if (club.getOperaciones().esAdministrador(sesion)) {
            LocalDate[] periodo = pedirPeriodo();
            var resumen = club.getOperaciones().consultarFinanzas(sesion, null, periodo[0], periodo[1]);
            System.out.println(resumen);
            System.out.println("Ingresos comerciales y mensualidades sin propinas: " + resumen.ingresoSinPropinas());
            for (Deporte deporte : club.getDeportes()) {
                club.getReportes().consultarResultados(deporte.getId(), periodo[0], periodo[1])
                        .forEach(System.out::println);
            }
        } else if (club.getOperaciones().esEntrenador(sesion)) {
            String equipo = pedir("ID del equipo");
            LocalDate[] periodo = pedirPeriodo();
            club.getReportes().consultarDetalleAsistencia(equipo, periodo[0], periodo[1]).forEach(System.out::println);
            club.getReportes().consultarEvolucion(null, periodo[0], periodo[1]).forEach(System.out::println);
            Equipo e = club.getEquipos().buscarEquipo(equipo);
            if (e != null) {
                club.getReportes().consultarResultados(e.getDeporte().getId(), periodo[0], periodo[1])
                        .forEach(System.out::println);
                for (var i : e.getInscripciones()) {
                    club.getReportes().consultarEstadisticasJugador(i.getJugador().getId(), periodo[0], periodo[1])
                            .forEach(System.out::println);
                }
            }
        } else {
            Jugador jugador = club.buscarJugador(sesion.getUsuarioId());
            if (jugador != null) {
                System.out.println("Rankings: " + jugador.getRankingsPorDisciplina());
                jugador.getMovimientosRanking().forEach(System.out::println);
                club.getReportes().consultarEstadisticasJugador(jugador.getId(), LocalDate.MIN, LocalDate.MAX)
                        .forEach(System.out::println);
            }
        }
    }

    private DayOfWeek pedirDia() {
        return DayOfWeek.of(Integer.parseInt(pedir("Día de semana (1 lunes a 7 domingo)")));
    }

    private void configurarHorario() {
        aplicar(club.getPersonal().configurarHorario(sesion, new HorarioAtencion(pedirDia(),
                LocalTime.parse(pedir("Apertura HH:MM")), LocalTime.parse(pedir("Cierre HH:MM")))));
    }

    private Turno leerTurno(String id, String empleado) {
        return new Turno(id, empleado, pedirDia(), LocalTime.parse(pedir("Inicio HH:MM")),
                LocalTime.parse(pedir("Fin HH:MM")));
    }

    private void registrarTurno() {
        aplicar(club.getPersonal().registrarTurno(sesion, leerTurno(pedir("ID de turno"), pedir("ID de empleado"))));
    }

    private void consultarTurnos() {
        for (Turno t : club.getPersonal().getTurnos()) {
            if (club.getOperaciones().esAdministrador(sesion) || t.empleadoId().equals(sesion.getUsuarioId())) {
                System.out.println(t);
            }
        }
    }

    private void solicitarCambioTurno() {
        String id = pedir("ID de solicitud"), turno = pedir("ID de turno actual");
        aplicar(club.getPersonal().solicitarCambio(sesion, id, leerTurno(turno, sesion.getUsuarioId())));
    }

    private void consultarSolicitudesTurno() {
        club.getPersonal().consultarSolicitudes(sesion).forEach(System.out::println);
    }

    private void resolverCambioTurno() {
        ResultadoOperacion r = club.getPersonal().resolverCambio(sesion, pedir("ID de solicitud"),
                preguntarSi("¿Aprobar? (s/n)"));
        cambio(r.mensaje()); // El rechazo de cobertura registra la decisión y conserva el calendario.
    }

    private void consultarCobertura() {
        var deficits = club.getPersonal().consultarCobertura(sesion);
        if (deficits.isEmpty()) {
            String mensajeCobertura;
            if (club.getPersonal().getHorarios().isEmpty()) {
                mensajeCobertura = "Horario de atención aún no configurado.";
            } else {
                mensajeCobertura = "Cobertura completa: 2 entrenadores y 1 fisioterapeuta en todo intervalo.";
            }
            System.out.println(mensajeCobertura);
        } else {
            deficits.forEach(System.out::println);
        }
    }

    private void cambiarEstadoUsuario() {
        aplicar(club.getOperaciones().cambiarEstadoUsuario(sesion, pedir("ID del usuario"),
                preguntarSi("¿Activar usuario? (s reactiva/n da de baja)")));
    }

    private void restablecerClave() {
        exigir(club.getAcceso().restablecerClave(sesion, pedir("ID de cuenta"), leerClave()),
                "Sin permiso, cuenta inexistente o clave inválida.");
        cambio("Clave restablecida y sesiones de esa cuenta cerradas.");
    }

    private void registrarCompetencia() {
        String id = pedir("ID de competencia"), nombre = pedir("Nombre"), organizador = pedir("Organizador");
        int temporada = Integer.parseInt(pedir("Temporada (año)"));
        Deporte deporte = club.buscarDeporte(pedir("Deporte"));
        String categoria = pedir("Categoría");
        modelo.competencias.FormatoCompetencia formatoSeleccionado;
        if (pedir("Formato LIGA/FASES").equalsIgnoreCase("FASES")) {
            formatoSeleccionado = new FormatoFases();
        } else {
            formatoSeleccionado = new FormatoLiga();
        }
        Competencia competencia = new Competencia(id, nombre, organizador, temporada, deporte, categoria,
                formatoSeleccionado);
        if (deporte != null && deporte.es("VOLEIBOL")) {
            exigir(competencia.configurarSetsParaGanar(Integer.parseInt(pedir("Sets para ganar (2 o 3)"))),
                    "Formato de sets inválido.");
        }
        aplicar(club.getOperaciones().registrarCompetencia(sesion, competencia));
    }

    private void registrarNomina() {
        String competencia = pedir("ID de competencia"), equipo = pedir("ID de equipo");
        List<String> jugadores = Arrays.stream(pedir("IDs de jugadores separados por coma").split(","))
                .map(String::trim).toList();
        aplicar(club.getOperaciones().registrarNomina(sesion, competencia, equipo, jugadores));
    }

    private void programarPartido() {
        String tipo = pedir("Tipo: 1 interno, 2 competencia"), id = pedir("ID de partido");
        LocalDateTime fecha = LocalDateTime.parse(pedir("Fecha y hora AAAA-MM-DDTHH:MM"));
        Deporte deporte = club.buscarDeporte(pedir("Deporte"));
        Partido p;
        if (tipo.equals("1")) {
            p = new PartidoInterno(id, fecha, deporte, club.getEquipos().buscarEquipo(pedir("ID de equipo local")),
                    club.getEquipos().buscarEquipo(pedir("ID de visitante")));
        } else {
            exigir(tipo.equals("2"), "Tipo de partido desconocido.");
            Competencia competencia = club.getCompetencias().buscarCompetencia(pedir("ID de competencia"));
            Equipo equipo = club.getEquipos().buscarEquipo(pedir("ID de equipo del club"));
            String rivalId = pedir("ID del participante rival registrado"),
                    encuentro = pedir("ID del encuentro del torneo");
            exigir(competencia != null && competencia.getFormato() instanceof FormatoLiga, "Competencia inexistente.");
            ParticipanteCompetencia rival = ((FormatoLiga) competencia.getFormato()).buscarParticipante(rivalId);
            exigir(rival != null, "Registre primero el participante rival.");
            PartidoCompetencia pc = new PartidoCompetencia(id, fecha, deporte, competencia, equipo, rival.nombre(),
                    pedir("Escenario"));
            Equipo equipoRival;
            if (rival.equipoClubId() == null) {
                equipoRival = null;
            } else {
                equipoRival = club.getEquipos().buscarEquipo(rival.equipoClubId());
            }
            exigir(pc.vincularEncuentro(encuentro, rivalId, rival.nombre(), equipoRival), "Rival incompatible.");
            p = pc;
        }

        if (tipo.equals("1") && deporte != null && deporte.es("VOLEIBOL")) {
            exigir(p.configurarSetsParaGanar(Integer.parseInt(pedir("Sets para ganar (2 o 3)"))),
                    "Formato de sets inválido.");
        }
        String cancha = pedir("ID de instalación del club o 0 si es externo");
        Instalacion instalacion;
        if (cancha.equals("0")) {
            instalacion = null;
        } else {
            instalacion = club.getReservas().buscarInstalacion(cancha);
        }
        exigir(cancha.equals("0") || instalacion != null, "Instalación inexistente.");
        exigir(p.configurarOcupacion(instalacion, Integer.parseInt(pedir("Duración en minutos"))),
                "Ocupación inválida.");
        aplicar(club.getOperaciones().registrarPartido(sesion, p));
    }

    private void registrarResultado() {
        aplicar(club.getOperaciones().registrarResultado(sesion, pedir("ID de partido"),
                Integer.parseInt(pedir("Puntos local/club")), Integer.parseInt(pedir("Puntos visitante/rival"))));
    }

    private void registrarMetrica() {
        String partido = pedir("ID de partido"), jugador = pedir("ID de jugador");
        System.out.println(Arrays.toString(TipoEstadistica.values()));
        aplicar(club.getOperaciones().registrarMetrica(sesion, partido, jugador,
                TipoEstadistica.valueOf(pedir("Tipo de métrica").toUpperCase(Locale.ROOT)),
                Integer.parseInt(pedir("Valor"))));
    }

    private void registrarTarjeta() {
        String partido = pedir("ID de partido"), jugador = pedir("ID de jugador");
        System.out.println(Arrays.toString(TipoTarjeta.values()));
        aplicar(club.getOperaciones().registrarTarjeta(sesion, partido, jugador,
                TipoTarjeta.valueOf(pedir("Tipo de tarjeta").toUpperCase(Locale.ROOT))));
    }

    private void actualizarCuenta() {
        exigirAdministrador();
        String id = pedir("ID de cuenta"), login = pedir("Nuevo login");
        Set<Perfil> perfiles = EnumSet.noneOf(Perfil.class);
        System.out.println(Arrays.toString(Perfil.values()));
        for (String perfil : pedir("Perfiles separados por coma").split(",")) {
            perfiles.add(Perfil.valueOf(perfil.trim().toUpperCase(Locale.ROOT)));
        }
        exigir(club.actualizarCuenta(sesion, id, login, perfiles),
                "Login ocupado, perfiles incompatibles, sin empleado, cuenta inexistente o último administrador.");
        cambio("Login y perfiles actualizados; las sesiones de esa cuenta se cerraron.");
    }

    private void consultarHabilitados() {
        var d = club.getHabilitacion().consultar(sesion, pedir("ID de partido"), pedir("ID de equipo del club"));
        String disponibilidadPartido;
        if (d.puedeJugar()) {
            disponibilidadPartido = "sí";
        } else {
            disponibilidadPartido = "no";
        }
        System.out.println("Habilitados: " + d.cantidad() + " | Mínimo: " + d.minimo() + " | Puede jugar: "
                + (disponibilidadPartido));
        d.habilitados().forEach(j -> System.out.println("  " + j.getId() + " | " + j.getNombre()));
        d.excluidos().forEach(
                e -> System.out.println("  No disponible: " + e.jugadorId() + " | " + e.nombre() + " | " + e.motivo()));
    }

    private void confirmarAlineacion() {
        String partido = pedir("ID de partido"), equipo = pedir("ID de equipo del club");
        List<String> jugadores = Arrays.stream(pedir("IDs de participantes separados por coma").split(","))
                .map(String::trim).toList();
        aplicar(club.getOperaciones().alinear(sesion, partido, equipo, jugadores));
    }

    private void retirarJugador() {
        String jugador;
        if (club.getOperaciones().esAdministrador(sesion) || club.getOperaciones().esEntrenador(sesion)) {
            jugador = pedir("ID de jugador");
        } else {
            jugador = sesion.getUsuarioId();
        }
        aplicar(club.getOperaciones().retirarJugador(sesion, jugador, pedir("ID de equipo"),
                pedir("Motivo del retiro")));
    }

    private void consultarDisciplina() {
        for (var s : club.getHabilitacion().getSuspensiones()) {
            String estadoSuspension;
            if (s.partidoCumplimientoId() == null) {
                estadoSuspension = "Suspensión pendiente";
            } else {
                estadoSuspension = "Cumplida en " + s.partidoCumplimientoId();
            }
            System.out.println(s.jugadorId() + " | " + club.buscarJugador(s.jugadorId()).getNombre() + " | "
                    + s.ambito() + " | Origen: " + s.partidoOrigenId() + " | " + estadoSuspension);
        }
        club.getHabilitacion().getAmarillas().forEach((clave, cantidad) -> System.out
                .println(clave.jugadorId() + " | " + clave.ambito() + " | Amarillas acumuladas: " + cantidad));
    }

    private List<String> ids(String mensaje) {
        return Arrays.stream(pedir(mensaje).split(",")).map(String::trim).toList();
    }

    private String opcional(String mensaje) {
        String r = pedir(mensaje + " (0 = todos/ninguno)");
        if (r.equals("0")) {
            return null;
        } else {
            return r;
        }
    }

    private LocalDateTime momento() {
        return LocalDateTime.parse(pedir("Inicio AAAA-MM-DDTHH:MM"));
    }

    private boolean docente() {
        return (club.getOperaciones().esAdministrador(sesion) || club.getOperaciones().esEntrenador(sesion));
    }

    private NivelAcademia nivel() {
        return NivelAcademia.valueOf(pedir("Nivel PRINCIPIANTE/INTERMEDIO/AVANZADO").toUpperCase(Locale.ROOT));
    }

    private void academia() {
        System.out.println("1 Inscribirse en disciplina | 3 Consultar niveles y ranking");
        if (docente()) {
            System.out.println("2 Actualizar nivel/estado | 4 Programar partido | 5 Programar entrenamiento de grupo");
        }

        if (club.getOperaciones().esAdministrador(sesion)) {
            System.out.println("6 Configurar academia");
        }
        String op = pedir("Acción de academia");
        switch (op) {
        case "1" -> {
            String jugadorId;
            if (docente()) {
                jugadorId = pedir("ID jugador");
            } else {
                jugadorId = sesion.getUsuarioId();
            }
            String disciplina = pedir("Disciplina");
            NivelAcademia nivelInscripcion;
            if (docente()) {
                nivelInscripcion = nivel();
            } else {
                nivelInscripcion = NivelAcademia.PRINCIPIANTE;
            }
            aplicar(club.getAcademias().inscribir(sesion, jugadorId, disciplina, nivelInscripcion));
        }
        case "2" -> {
            exigir(docente(), "Sin permiso docente.");
            aplicar(club.getAcademias().actualizar(sesion, pedir("ID jugador"), pedir("Disciplina"), nivel(),
                    preguntarSi("¿Inscripción activa? s/n")));
        }
        case "3" -> {
            String deporte = pedir("Disciplina");
            club.getAcademias().getInscripciones().stream()
                    .filter(i -> i.getDisciplina().es(deporte)
                            && (docente() || i.getJugador().getId().equals(sesion.getUsuarioId())))
                    .forEach(i -> System.out.println(i.getJugador().getId() + " | " + i.getJugador().getNombre() + " | "
                            + i.getNivel() + " | activa: " + i.estaActiva() + " | ELO: " + i.getRanking()));
        }
        case "4" -> {
            exigir(docente(), "Sin permiso docente.");
            aplicar(club.getDeportivo().programarAcademia(sesion, pedir("ID partido"), pedir("Disciplina"),
                    pedir("Modalidad Sencillos/Dobles"), ids("IDs lado A separados por coma"),
                    ids("IDs lado B separados por coma"), pedir("ID instalación"), momento(),
                    Integer.parseInt(pedir("Minutos")), Integer.parseInt(pedir("Sets para ganar 2/3"))));
        }
        case "5" -> {
            exigir(docente(), "Sin permiso docente.");
            aplicar(club.getDeportivo().programarGrupo(sesion, pedir("ID entrenamiento"), pedir("Disciplina"), nivel(),
                    pedir("ID entrenador"), pedir("ID instalación"), momento(), Integer.parseInt(pedir("Minutos"))));
        }
        case "6" -> {
            exigirAdministrador();
            aplicar(club.getAcademias().configurar(sesion, new gestores.GestorAcademias.Configuracion(
                    Integer.parseInt(pedir("Diferencia máxima de niveles 0/1")),
                    preguntarSi("¿Excepción de nivel para entrenador-jugador? s/n"),
                    Integer.parseInt(pedir("Factor K ELO 1..100")), Double.parseDouble(pedir("Ranking inicial")))));
        }
        default -> throw new IllegalArgumentException("Acción de academia desconocida.");
        }
        System.out.println("Para resultado y ELO use 52; para convocar/asistencia use 36/37.");
    }

    private void torneos() {
        System.out.println("1 Tabla, cruces y campeón");
        if (docente()) {
            System.out.println(
                    "3 Programar encuentro de liga | 4 Resultado externo | 6 Avanzar fase | 9 Actualizar nómina | 10 Desempate eliminatorio");
        }

        if (club.getOperaciones().esAdministrador(sesion)) {
            System.out.println(
                    "2 Participante externo | 5 Configurar grupos | 7 Activar/retirar participante | 8 Victoria administrativa | 11 Configurar puntuación");
        }
        String op = pedir("Acción de torneo"), co = pedir("ID competencia");
        var d = club.getDeportivo();
        if (!op.equals("1")) {
            exigir(docente(), "Sin permiso docente.");
        }
        switch (op) {
        case "1" -> {
            String grupo = opcional("Grupo");
            d.tabla(sesion, co, grupo).forEach(System.out::println);
            d.encuentros(sesion, co).forEach(System.out::println);
            if (club.getCompetencias().buscarCompetencia(co).getFormato() instanceof FormatoFases f) {
                System.out.println("Ronda: " + f.getRonda() + " | Campeón: " + f.getCampeon());
            }
        }
        case "2" -> aplicar(d.registrarExterno(sesion, co, pedir("ID participante"), pedir("Nombre/institución")));
        case "3" -> aplicar(d.registrarEncuentro(sesion, co, pedir("ID encuentro"), pedir("ID participante local"),
                pedir("ID participante visitante")));
        case "4" ->
            aplicar(d.resultadoExterno(sesion, co, pedir("ID encuentro"), Integer.parseInt(pedir("Marcador local")),
                    Integer.parseInt(pedir("Marcador visitante")), opcional("ID ganador del desempate")));
        case "5" -> {
            exigirAdministrador();
            int n = Integer.parseInt(pedir("Número de grupos"));
            exigir(n > 0 && n <= 100, "Cantidad inválida.");
            Map<String, List<String>> grupos = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                exigir(grupos.putIfAbsent(pedir("Nombre de grupo"),
                        ids("IDs participantes separados por coma")) == null, "Grupo repetido.");
            }
            aplicar(d.configurarGrupos(sesion, co, grupos, Integer.parseInt(pedir("Clasificados por grupo"))));
        }
        case "6" -> aplicar(d.avanzar(sesion, co));
        case "7" -> aplicar(d.estadoParticipante(sesion, co, pedir("ID participante"), preguntarSi("¿Activar? s/n")));
        case "8" -> aplicar(d.victoriaAdministrativa(sesion, co, pedir("ID encuentro"), pedir("ID ganador")));
        case "9" -> aplicar(
                d.actualizarNomina(sesion, co, pedir("ID equipo"), ids("IDs jugadores actuales separados por coma")));
        case "10" -> aplicar(d.definirDesempate(sesion, pedir("ID partido oficial"), pedir("ID participante ganador")));
        case "11" -> aplicar(d.configurarPuntuacion(sesion, co,
                new ReglasPuntuacion(Integer.parseInt(pedir("Puntos victoria")),
                        Integer.parseInt(pedir("Puntos empate")), Integer.parseInt(pedir("Puntos derrota")),
                        preguntarSi("¿Ajustar voleibol: 3/0 directo y 2/1 set decisivo? s/n")),
                Integer.parseInt(pedir("Sets para ganar 2/3"))));
        default -> throw new IllegalArgumentException("Acción de torneo desconocida.");
        }
    }

    private ActividadAgenda actividad() {
        String tipo = pedir("Tipo RESERVA/ENTRENAMIENTO/PARTIDO"), id = pedir("ID actividad");
        return switch (tipo.toUpperCase(Locale.ROOT)) {
        case "RESERVA" -> club.getReservas().buscarReserva(id);
        case "ENTRENAMIENTO" -> club.getEntrenamientos().buscarEntrenamiento(id);
        case "PARTIDO" -> club.getPartidos().buscarPartido(id);
        default -> throw new IllegalArgumentException("Tipo desconocido.");
        };
    }

    private void agenda() {
        System.out.println("1 Consultar agenda | 3 Reprogramar | 4 Cancelar");
        if (club.getOperaciones().esCaja(sesion) || club.getAcceso().tienePerfil(sesion, Perfil.SOCIO)) {
            System.out.println("2 Disponibilidad");
        }

        if (club.getOperaciones().esAdministrador(sesion)) {
            System.out.println("5 Configurar máximo simultáneo");
        }
        switch (pedir("Acción de agenda")) {
        case "1" -> club.getDeportivo()
                .agenda(sesion, LocalDate.parse(pedir("Desde AAAA-MM-DD")), LocalDate.parse(pedir("Hasta AAAA-MM-DD")))
                .forEach(a -> {
                    String instalacion;
                    if (a.getInstalacion() == null) {
                        instalacion = "externa";
                    } else {
                        instalacion = a.getInstalacion().getId();
                    }
                    System.out.println(a.getClass().getSimpleName() + " | " + a.getId() + " | " + a.getInicio() + " -> "
                            + a.getFin() + " | instalación: " + instalacion + " | cancelada: " + a.estaCancelada()
                            + " | cambios: " + a.getCambiosAgenda().size());
                });
        case "2" -> {
            String jugadorConsulta;
            if (club.getOperaciones().esCaja(sesion)) {
                jugadorConsulta = pedir("ID jugador");
            } else {
                jugadorConsulta = sesion.getUsuarioId();
            }
            String j = jugadorConsulta, dep = pedir("Disciplina"), mod = pedir("Modalidad Sencillos/Dobles");
            LocalDateTime inicio = momento();
            int modalidadSeleccionada;
            if (mod.equalsIgnoreCase("Dobles")) {
                modalidadSeleccionada = 4;
            } else {
                modalidadSeleccionada = 2;
            }
            club.getDeportivo()
                    .disponibles(sesion, j, dep, inicio, inicio.plusMinutes(Integer.parseInt(pedir("Minutos"))),
                            modalidadSeleccionada, mod)
                    .forEach(i -> System.out.println(i.getId() + " | " + i.getNombre()));
        }
        case "3" -> {
            ActividadAgenda a = actividad();
            aplicar(club.getDeportivo().reprogramar(sesion, a, opcional("ID instalación (0 externa solo partidos)"),
                    momento(), Integer.parseInt(pedir("Minutos")), pedir("Motivo")));
        }
        case "4" -> aplicar(club.getDeportivo().cancelar(sesion, actividad(), pedir("Motivo")));
        case "5" -> {
            exigirAdministrador();
            aplicar(club.getAgenda().configurarMaximo(sesion, pedir("Disciplina"),
                    Integer.parseInt(pedir("Máximo simultáneo"))));
        }
        default -> throw new IllegalArgumentException("Acción de agenda desconocida.");
        }
    }

    private void acumulados() {
        String deporte = opcional("Disciplina"), competencia = opcional("ID competencia");
        club.getDeportivo().clasificacion(sesion, deporte, competencia,
                TipoEstadistica.valueOf(pedir("Métrica de clasificación: " + Arrays.toString(TipoEstadistica.values()))
                        .toUpperCase(Locale.ROOT)),
                LocalDate.parse(pedir("Desde AAAA-MM-DD")), LocalDate.parse(pedir("Hasta AAAA-MM-DD")))
                .forEach(System.out::println);
    }

    private boolean guardar() {
        try {
            persistencia.salvar(archivo, club);
            pendiente = false;
            System.out.println("Datos guardados en JSON.");
            return true;
        } catch (IOException e) {
            System.err.println("No se pudo guardar: " + e.getMessage());
            System.err.println("Los cambios siguen en memoria. Reintenta con la opción 7 antes de salir.");
            return false;
        }
    }
}
