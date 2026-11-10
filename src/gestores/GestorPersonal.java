package gestores;

import aplicacion.ResultadoOperacion;
import gestores.GestorAcceso.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import modelo.administracion.*;
import modelo.usuarios.Empleado;

public class GestorPersonal {

    public record Deficit(DayOfWeek dia, LocalTime inicio, LocalTime fin, int entrenadoresFaltantes,
            int fisioterapeutasFaltantes) {
    }

    private final GestorAcceso acceso;
    private final Function<String, Empleado> empleados;
    private final Map<DayOfWeek, HorarioAtencion> horarios = new EnumMap<>(DayOfWeek.class);
    private final Map<String, Turno> turnos = new LinkedHashMap<>();
    private final Map<String, SolicitudCambioTurno> solicitudes = new LinkedHashMap<>();

    public GestorPersonal(GestorAcceso acceso, Function<String, Empleado> empleados) {
        this.acceso = acceso;
        this.empleados = empleados;
    }

    public List<HorarioAtencion> getHorarios() {
        return List.copyOf(horarios.values());
    }

    public List<Turno> getTurnos() {
        return List.copyOf(turnos.values());
    }

    public List<SolicitudCambioTurno> getSolicitudes() {
        return List.copyOf(solicitudes.values());
    }

    public List<SolicitudCambioTurno> consultarSolicitudes(Sesion sesion) {
        if (!acceso.estaAutenticada(sesion)) {
            return List.of();
        }
        return solicitudes.values().stream().filter(
                s -> acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR) || s.empleadoId().equals(sesion.getUsuarioId()))
                .toList();
    }

    public ResultadoOperacion configurarHorario(Sesion admin, HorarioAtencion horario) {
        if (!acceso.tienePerfil(admin, Perfil.ADMINISTRADOR) || horario == null) {
            return ResultadoOperacion.rechazo("Solo el administrador configura horarios.");
        }
        horarios.put(horario.dia(), horario);
        return ResultadoOperacion.correcto("Horario configurado.");
    }

    public ResultadoOperacion registrarTurno(Sesion admin, Turno turno) {
        if (!acceso.tienePerfil(admin, Perfil.ADMINISTRADOR) || turno == null
                || !acceso.tienePerfil(turno.empleadoId(), Perfil.EMPLEADO)) {
            return ResultadoOperacion.rechazo("Se requiere administrador y empleado activo con perfil laboral.");
        }
        try {
            cargarTurno(turno);
            return ResultadoOperacion.correcto("Turno registrado.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion solicitarCambio(Sesion sesion, String id, Turno propuesto) {
        Turno anterior;
        if (propuesto == null) {
            anterior = null;
        } else {
            anterior = turnos.get(propuesto.id());
        }

        if (!acceso.tienePerfil(sesion, Perfil.EMPLEADO) || anterior == null
                || !anterior.empleadoId().equals(sesion.getUsuarioId())
                || !propuesto.empleadoId().equals(sesion.getUsuarioId()) || id == null || id.isBlank()
                || solicitudes.containsKey(id)) {
            return ResultadoOperacion.rechazo("Solo el empleado titular puede solicitar un cambio con ID único.");
        }
        solicitudes.put(id, new SolicitudCambioTurno(id, sesion.getUsuarioId(), anterior, propuesto,
                SolicitudCambioTurno.Estado.PENDIENTE, ""));
        return ResultadoOperacion.correcto("Cambio solicitado.");
    }

    public ResultadoOperacion resolverCambio(Sesion admin, String id, boolean aprobar) {
        SolicitudCambioTurno s = solicitudes.get(id);
        if (!acceso.tienePerfil(admin, Perfil.ADMINISTRADOR) || s == null
                || s.estado() != SolicitudCambioTurno.Estado.PENDIENTE) {
            return ResultadoOperacion.rechazo("Solicitud inexistente, resuelta o sin permiso administrativo.");
        }
        String motivo = "Rechazada por el administrador.";
        if (aprobar) {
            if (!acceso.tienePerfil(s.empleadoId(), Perfil.EMPLEADO)) {
                motivo = "Empleado inactivo o sin perfil laboral vigente.";
            } else if (!s.anterior().equals(turnos.get(s.anterior().id()))) {
                motivo = "El turno cambió después de crear la solicitud.";
            } else if (horarios.isEmpty()) {
                motivo = "Configure el horario de atención antes de aprobar.";
            } else {
                Map<String, Turno> simulados = new LinkedHashMap<>(turnos);
                simulados.put(s.propuesto().id(), s.propuesto());
                List<Deficit> faltantes = cobertura(simulados.values());
                if (faltantes.isEmpty()) {
                    turnos.put(s.propuesto().id(), s.propuesto());
                    solicitudes.put(id, new SolicitudCambioTurno(id, s.empleadoId(), s.anterior(), s.propuesto(),
                            SolicitudCambioTurno.Estado.APROBADA, "Cobertura conservada."));
                    return ResultadoOperacion.correcto("Cambio aprobado.");
                }
                Deficit d = faltantes.get(0);
                motivo = d.dia() + " " + d.inicio() + "–" + d.fin() + ": faltan " + d.entrenadoresFaltantes()
                        + " entrenadores y " + d.fisioterapeutasFaltantes() + " fisioterapeutas.";
            }
        }
        solicitudes.put(id, new SolicitudCambioTurno(id, s.empleadoId(), s.anterior(), s.propuesto(),
                SolicitudCambioTurno.Estado.RECHAZADA, motivo));
        if (aprobar) {
            return ResultadoOperacion.rechazo(motivo);
        } else {
            return ResultadoOperacion.correcto(motivo);
        }
    }

    public List<Deficit> consultarCobertura(Sesion sesion) {
        if (!acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR)) {
            throw new IllegalArgumentException("Solo el administrador consulta cobertura completa.");
        }
        return cobertura(turnos.values());
    }

    private List<Deficit> cobertura(Collection<Turno> calendario) {
        List<Deficit> resultado = new ArrayList<>();
        for (HorarioAtencion h : horarios.values()) {
            SortedSet<LocalTime> limites = new TreeSet<>(List.of(h.inicio(), h.fin()));
            for (Turno t : calendario) {
                if (t.dia() == h.dia()) {
                    if (t.inicio().isAfter(h.inicio()) && t.inicio().isBefore(h.fin())) {
                        limites.add(t.inicio());
                    }

                    if (t.fin().isAfter(h.inicio()) && t.fin().isBefore(h.fin())) {
                        limites.add(t.fin());
                    }
                }
            }
            List<LocalTime> puntos = new ArrayList<>(limites);
            for (int i = 0; i < puntos.size() - 1; i++) {
                LocalTime desde = puntos.get(i), hasta = puntos.get(i + 1);
                Set<String> entrenadores = new HashSet<>(), fisios = new HashSet<>();
                for (Turno t : calendario) {
                    if (t.dia() == h.dia() && !t.inicio().isAfter(desde) && !t.fin().isBefore(hasta)) {
                        Empleado empleado = empleados.apply(t.empleadoId());
                        if (empleado == null || !empleado.getUsuario().estaActivo()) {
                            continue;
                        }

                        if (acceso.tienePerfil(t.empleadoId(), Perfil.ENTRENADOR)) {
                            entrenadores.add(t.empleadoId());
                        }

                        if (acceso.tienePerfil(t.empleadoId(), Perfil.FISIOTERAPEUTA)) {
                            fisios.add(t.empleadoId());
                        }
                    }
                }
                if (entrenadores.size() < 2 || fisios.size() < 1) {
                    resultado.add(new Deficit(h.dia(), desde, hasta, Math.max(0, 2 - entrenadores.size()),
                            Math.max(0, 1 - fisios.size())));
                }
            }
        }
        return resultado;
    }

    public void cargarHorario(HorarioAtencion h) {
        if (h == null || horarios.containsKey(h.dia())) {
            throw new IllegalArgumentException("Horario duplicado.");
        }
        horarios.put(h.dia(), h);
    }

    public void cargarTurno(Turno t) {
        if (t == null || empleados.apply(t.empleadoId()) == null || turnos.containsKey(t.id())) {
            throw new IllegalArgumentException("Turno duplicado o empleado inexistente.");
        }
        turnos.put(t.id(), t);
    }

    public void cargarSolicitud(SolicitudCambioTurno s) {
        if (s == null || empleados.apply(s.empleadoId()) == null || !turnos.containsKey(s.anterior().id())
                || !turnos.get(s.anterior().id()).empleadoId().equals(s.empleadoId())
                || solicitudes.containsKey(s.id())) {
            throw new IllegalArgumentException("Solicitud duplicada o referencia inexistente.");
        }
        solicitudes.put(s.id(), s);
    }
}
