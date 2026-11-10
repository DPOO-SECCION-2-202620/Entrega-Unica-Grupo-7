package aplicacion;

import gestores.*;
import gestores.GestorAcceso;
import gestores.GestorReservas;
import gestores.GestorSeguridadInstalaciones;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.Deporte;
import modelo.usuarios.Empleado;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;
import modelo.ventas.Inventario;

public class EstadoClub {

    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Jugador> jugadores = new LinkedHashMap<>();
    private final Map<String, Deporte> deportes = new LinkedHashMap<>();
    private final GestorReservas reservas = new GestorReservas();
    private final Inventario inventario = new Inventario();
    private final GestorSocios socios = new GestorSocios(jugadores);
    private final GestorEquipos equipos = new GestorEquipos();
    private final GestorEntrenamientos entrenamientos = new GestorEntrenamientos();
    private final GestorPartidos partidos = new GestorPartidos();
    private final GestorTorneos torneos = new GestorTorneos(this);
    private final GestorBonos bonos = new GestorBonos(this);
    private final GestorCompetencias competencias = new GestorCompetencias();
    private final GestorAcceso acceso = new GestorAcceso();
    private final GestorSeguridadInstalaciones seguridad;
    private final GestorVentas ventas;
    private final GestorAcademias academias;
    private final GestorAgendaClub agenda;
    private final GestorHabilitacion habilitacion;
    private final GestorPersonal personal;
    private final GestorReportes reportes;
    private final ServicioClub operaciones;
    private final ServicioBC deportivo;
    private final Clock reloj;

    public EstadoClub() {
        this(Clock.system(ZoneId.of("America/Bogota")));
    }

    public EstadoClub(Clock reloj) {
        this.reloj = reloj;
        seguridad = new GestorSeguridadInstalaciones(reservas, acceso, reloj, this::buscarUsuario);
        ventas = new GestorVentas(inventario, acceso, seguridad, this::buscarUsuario);
        academias = new GestorAcademias(this);
        agenda = new GestorAgendaClub(this);
        reservas.configurarValidador(r -> {
            if (!academias.estaInscrito(r.getJugador().getId(), r.getInstalacion().getDeporte().getId())) {
                return "El socio no está inscrito en esa disciplina.";
            } else {
                return agenda.validar(r);
            }
        });
        entrenamientos.configurarValidador(agenda::validar);
        partidos.configurarValidadorAgenda(agenda::validar);
        habilitacion = new GestorHabilitacion(this);
        partidos.configurarVerificador(p -> {
            if (p instanceof modelo.partidos.PartidoAcademia a) {
                return academias.validarJuego(a).exito();
            } else {
                return habilitacion.validarResultado(p).exito();
            }
        });
        personal = new GestorPersonal(acceso, id -> ventas.buscarEmpleadoPorCodigo("EMP-" + id));
        reportes = new GestorReportes(ventas, socios, partidos, entrenamientos, reservas);
        operaciones = new ServicioClub(this);
        deportivo = new ServicioBC(this);
        deportes.put("FUTBOL", new Deporte("FUTBOL", "Fútbol", true));
        deportes.put("BALONCESTO", new Deporte("BALONCESTO", "Baloncesto", true));
        deportes.put("VOLEIBOL", new Deporte("VOLEIBOL", "Voleibol", true));
        deportes.put("TENIS", new Deporte("TENIS", "Tenis", false));
        deportes.put("PADEL", new Deporte("PADEL", "Pádel", false));
        deportes.put("MESA", new Deporte("MESA", "Tenis de mesa", false));
    }

    public void registrarJugador(String id, String nombre, LocalDate nacimiento) {
        if (id == null || id.isBlank() || id.startsWith("HISTORICO-VENTA-") || nombre == null || nombre.isBlank()
                || nacimiento == null || nacimiento.isAfter(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("Datos del jugador inválidos.");
        }
        id = id.trim();
        if (jugadores.containsKey(id)) {
            throw new IllegalArgumentException("Ya existe el jugador " + id);
        }
        Usuario usuario = buscarUsuario(id);
        if (usuario == null) {
            usuario = new Usuario(id, nombre.trim(), id, "");
            usuarios.put(id, usuario);
        }

        if (!usuario.getNombre().equals(nombre.trim())) {
            throw new IllegalArgumentException("Identidad incompatible.");
        }
        jugadores.put(id, new Jugador(nacimiento, usuario));
    }

    public Jugador buscarJugador(String id) {
        return jugadores.get(id);
    }

    public Deporte buscarDeporte(String id) {
        return deportes.get(Deporte.clave(id));
    }

    public List<Jugador> getJugadores() {
        return new ArrayList<>(jugadores.values());
    }

    public List<Deporte> getDeportes() {
        return new ArrayList<>(deportes.values());
    }

    public Clock getReloj() {
        return reloj;
    }

    public GestorSocios getSocios() {
        return socios;
    }

    public GestorEquipos getEquipos() {
        return equipos;
    }

    public GestorEntrenamientos getEntrenamientos() {
        return entrenamientos;
    }

    public GestorPartidos getPartidos() {
        return partidos;
    }

    public GestorTorneos getTorneos() {
        return torneos;
    }

    public GestorBonos getBonos() {
        return bonos;
    }

    public GestorCompetencias getCompetencias() {
        return competencias;
    }

    public GestorAcademias getAcademias() {
        return academias;
    }

    public GestorAgendaClub getAgenda() {
        return agenda;
    }

    public GestorHabilitacion getHabilitacion() {
        return habilitacion;
    }

    public GestorPersonal getPersonal() {
        return personal;
    }

    public GestorReportes getReportes() {
        return reportes;
    }

    public ServicioBC getDeportivo() {
        return deportivo;
    }

    public ServicioClub getOperaciones() {
        return operaciones;
    }

    public GestorReservas getReservas() {
        return reservas;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public GestorAcceso getAcceso() {
        return acceso;
    }

    public GestorSeguridadInstalaciones getSeguridad() {
        return seguridad;
    }

    public boolean crearAdministradorInicial(String id, String nombre, String clave) {
        if (!acceso.puedeCrearAdministradorInicial() || id == null || id.isBlank() || nombre == null || nombre.isBlank()
                || clave == null || clave.length() < 8) {
            return false;
        }
        Usuario existente = buscarUsuario(id);
        if (existente != null) {
            return false;
        }
        Usuario usuario = new Usuario(id, nombre, id, "");
        if (!acceso.crearAdministradorInicial(usuario, clave)) {
            return false;
        }
        usuarios.put(id, usuario);
        return true;
    }

    public boolean registrarCuentaBase(String id, String nombre, String login, String clave) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
            return false;
        }
        id = id.trim();
        if (id.startsWith("HISTORICO-VENTA-") || buscarUsuario(id) != null) {
            return false;
        }
        Usuario usuario = new Usuario(id, nombre.trim(), login, "");
        if (!acceso.registrarCuentaBase(usuario, login, clave)) {
            return false;
        }
        usuarios.put(id, usuario);
        return true;
    }

    public boolean habilitarCuenta(gestores.GestorAcceso.Sesion administrador, String id, String clave,
            Set<gestores.GestorAcceso.Perfil> perfiles) {
        return habilitarCuenta(administrador, id, id, clave, perfiles);
    }

    public boolean habilitarCuenta(gestores.GestorAcceso.Sesion administrador, String id, String login, String clave,
            Set<gestores.GestorAcceso.Perfil> perfiles) {
        Usuario usuario = buscarUsuario(id);
        if (usuario == null || !usuario.esIdentificado() || !usuario.estaActivo() || perfiles == null
                || perfiles.isEmpty()) {
            return false;
        }
        boolean laboral = perfiles.contains(GestorAcceso.Perfil.EMPLEADO)
                || perfiles.contains(GestorAcceso.Perfil.ENTRENADOR)
                || perfiles.contains(GestorAcceso.Perfil.FISIOTERAPEUTA);
        if (laboral && ventas.buscarEmpleadoPorCodigo("EMP-" + id) == null) {
            return false;
        }

        if (perfiles.contains(GestorAcceso.Perfil.ENTRENADOR)
                && perfiles.contains(GestorAcceso.Perfil.FISIOTERAPEUTA)) {
            return false;
        }
        return acceso.registrarCuenta(administrador, usuario, login, clave, perfiles);
    }

    public boolean actualizarCuenta(gestores.GestorAcceso.Sesion administrador, String id, String login,
            Set<gestores.GestorAcceso.Perfil> perfiles) {
        Usuario usuario = buscarUsuario(id);
        if (usuario == null || !usuario.esIdentificado() || perfiles == null || perfiles.isEmpty()) {
            return false;
        }
        boolean laboral = perfiles.contains(GestorAcceso.Perfil.EMPLEADO)
                || perfiles.contains(GestorAcceso.Perfil.ENTRENADOR)
                || perfiles.contains(GestorAcceso.Perfil.FISIOTERAPEUTA);
        if (laboral && ventas.buscarEmpleadoPorCodigo("EMP-" + id) == null) {
            return false;
        }

        if (perfiles.contains(GestorAcceso.Perfil.ENTRENADOR)
                && perfiles.contains(GestorAcceso.Perfil.FISIOTERAPEUTA)) {
            return false;
        }
        return acceso.actualizarCuenta(administrador, id, login, perfiles);
    }

    public GestorVentas getVentas() {
        return ventas;
    }

    public List<Usuario> getUsuarios() {
        Map<String, Usuario> todos = new LinkedHashMap<>(usuarios);
        for (Empleado e : ventas.getEmpleados()) {
            todos.put(e.getId(), e.getUsuario());
        }
        return new ArrayList<>(todos.values());
    }

    public Usuario buscarUsuario(String id) {
        if (usuarios.containsKey(id)) {
            return usuarios.get(id);
        }
        Jugador jugador = buscarJugador(id);
        if (jugador != null) {
            return jugador.getUsuario();
        }
        for (Empleado e : ventas.getEmpleados()) {
            if (e.getId().equals(id)) {
                return e.getUsuario();
            }
        }
        return null;
    }

    public void cargarUsuarioHistorico(String id, String nombre) {
        if (id == null || !id.startsWith("HISTORICO-VENTA-") || nombre == null || nombre.isBlank()
                || buscarUsuario(id) != null) {
            throw new IllegalArgumentException("Marcador histórico inválido o duplicado.");
        }
        usuarios.put(id, new Usuario(id, nombre, "", "", false));
    }

    public Usuario compradorHistorico(String ventaId) {
        if (ventaId == null || ventaId.isBlank()) {
            throw new IllegalArgumentException("Venta histórica sin ID.");
        }
        String id = "HISTORICO-VENTA-" + ventaId;
        Usuario usuario = buscarUsuario(id);
        if (usuario != null) {
            throw new IllegalArgumentException("Marcador histórico duplicado.");
        }
        cargarUsuarioHistorico(id, "Comprador no identificado (venta histórica)");
        return buscarUsuario(id);
    }

    public void registrarUsuario(String id, String nombre) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank() || buscarUsuario(id) != null) {
            throw new IllegalArgumentException("Usuario inválido o duplicado.");
        }
        usuarios.put(id, new Usuario(id, nombre, id, ""));
    }

    public boolean altaEmpleado(gestores.GestorAcceso.Sesion administrador, String id, String nombre) {
        if (!acceso.tienePerfil(administrador, gestores.GestorAcceso.Perfil.ADMINISTRADOR)) {
            return false;
        }
        try {
            registrarEmpleado(id, nombre);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean altaUsuario(gestores.GestorAcceso.Sesion administrador, String id, String nombre) {
        if (!acceso.tienePerfil(administrador, gestores.GestorAcceso.Perfil.ADMINISTRADOR)) {
            return false;
        }
        try {
            registrarUsuario(id, nombre);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void registrarEmpleado(String id, String nombre) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Datos de empleado inválidos.");
        }
        Usuario usuario = buscarUsuario(id);
        if (usuario == null) {
            usuario = new Usuario(id, nombre, id, "");
        }

        if (!usuario.getNombre().equals(nombre) || !ventas.cargarEmpleado(new Empleado(usuario))) {
            throw new IllegalArgumentException("Empleado duplicado o identidad incompatible.");
        }
        usuarios.put(id, usuario);
    }
}
