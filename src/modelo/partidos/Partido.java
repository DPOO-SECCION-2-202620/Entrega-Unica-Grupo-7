package modelo.partidos;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BooleanSupplier;
import modelo.deportes.*;
import modelo.reservas.*;
import modelo.usuarios.Jugador;

public abstract class Partido implements ActividadAgenda {

    private Instalacion instalacion;
    private int duracionMinutos;
    private final List<CambioAgenda> cambios = new ArrayList<>();
    private final String id;
    private LocalDateTime fechaHora;
    private final Deporte deporte;
    private final List<EstadisticaPartido> estadisticas = new ArrayList<>();
    private final List<Sancion> sanciones = new ArrayList<>();
    private final Map<String, List<Jugador>> selecciones = new LinkedHashMap<>(), plantillas = new LinkedHashMap<>();
    private boolean cancelado, cerrado, legado;
    private int setsParaGanar = 3;
    private BooleanSupplier validacion;

    public Partido(String id, LocalDateTime fechaHora, Deporte deporte) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.deporte = deporte;
        if (deporte != null && deporte.isColectivo()) {
            this.duracionMinutos = 120;
        } else {
            this.duracionMinutos = 90;
        }
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getInicio() {
        return fechaHora;
    }

    public LocalDateTime getFin() {
        if (fechaHora == null) {
            return null;
        } else {
            return fechaHora.plusMinutes(duracionMinutos);
        }
    }

    public Instalacion getInstalacion() {
        return instalacion;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public boolean estaCancelada() {
        return cancelado;
    }

    public Set<String> getUsuariosAgenda() {
        Set<String> ids = new HashSet<>();
        getEquiposClub().forEach(e -> {
            if (selecciones.containsKey(e.getId())) {
                selecciones.get(e.getId()).forEach(j -> ids.add(j.getId()));
            } else {
                e.getInscripciones().forEach(i -> ids.add(i.getJugador().getId()));
            }
        });
        return Set.copyOf(ids);
    }

    public List<CambioAgenda> getCambiosAgenda() {
        return List.copyOf(cambios);
    }

    public void cargarCambio(CambioAgenda cambio) {
        cambios.add(cambio);
    }

    public boolean configurarOcupacion(Instalacion instalacion, int duracion) {
        if (tieneResultado() || cerrado || cancelado || duracion < 0
                || (instalacion != null && (duracion == 0 || instalacion.getDeporte() != deporte))) {
            return false;
        }
        this.instalacion = instalacion;
        this.duracionMinutos = duracion;
        return true;
    }

    public void restaurarHorario(Instalacion instalacion, LocalDateTime inicio, int duracion) {
        this.instalacion = instalacion;
        this.fechaHora = inicio;
        this.duracionMinutos = duracion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public abstract boolean tieneResultado();

    public abstract List<Equipo> getEquiposClub();

    protected abstract boolean participanteLegado(Jugador j);

    public boolean estaCancelado() {
        return cancelado;
    }

    public boolean estaCerrado() {
        return cerrado;
    }

    public boolean esLegado() {
        return legado;
    }

    public int getSetsParaGanar() {
        return setsParaGanar;
    }

    public boolean configurarSetsParaGanar(int sets) {
        if (tieneResultado() || cerrado || cancelado || (sets != 2 && sets != 3)) {
            return false;
        }
        setsParaGanar = sets;
        return true;
    }

    public void vincularValidacion(BooleanSupplier validacion) {
        this.validacion = validacion;
    }

    protected boolean cumpleValidacion() {
        return (!cancelado && !cerrado && (validacion == null || validacion.getAsBoolean()));
    }

    protected boolean marcadorValido(int local, int visitante) {
        if (deporte == null || local < 0 || visitante < 0 || tieneResultado() || cancelado || cerrado) {
            return false;
        }

        if (deporte.es("VOLEIBOL")) {
            return (Math.max(local, visitante) == setsParaGanar && Math.min(local, visitante) < setsParaGanar);
        }
        return !deporte.es("BALONCESTO") || local != visitante;
    }

    protected boolean marcadorLegadoValido(int local, int visitante) {
        return (deporte != null && deporte.marcadorValido(local, visitante) && !tieneResultado() && !cancelado
                && !cerrado);
    }

    public List<Jugador> getParticipantes(String equipoId) {
        return selecciones.getOrDefault(equipoId, List.of());
    }

    public Map<String, List<Jugador>> getSelecciones() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(selecciones));
    }

    public Map<String, List<Jugador>> getPlantillas() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(plantillas));
    }

    public boolean definirSeleccion(String equipoId, List<Jugador> jugadores) {
        if (tieneResultado() || cancelado || cerrado) {
            return false;
        }
        restaurarSeleccion(equipoId, jugadores);
        return true;
    }

    public void restaurarSeleccion(String equipoId, List<Jugador> jugadores) {
        validarLista(equipoId, jugadores);
        selecciones.put(equipoId, List.copyOf(jugadores));
    }

    public void restaurarPlantilla(String equipoId, List<Jugador> jugadores) {
        validarLista(equipoId, jugadores);
        plantillas.put(equipoId, List.copyOf(jugadores));
    }

    private void validarLista(String equipoId, List<Jugador> jugadores) {
        if (jugadores == null || jugadores.stream().anyMatch(Objects::isNull)
                || getEquiposClub().stream().noneMatch(e -> e.getId().equals(equipoId))
                || jugadores.stream().map(Jugador::getId).distinct().count() != jugadores.size()) {
            throw new IllegalArgumentException("Selección o plantilla inválida.");
        }
    }

    public boolean esParticipante(Jugador jugador) {
        if (jugador == null) {
            return false;
        }

        if (selecciones.isEmpty()) {
            return participanteLegado(jugador);
        } else {
            return selecciones.values().stream().anyMatch(lista -> lista.contains(jugador));
        }
    }

    public boolean registrarEstadistica(EstadisticaPartido e) {
        if (e == null || e.getPartido() != this || cerrado || cancelado || !esParticipante(e.getJugador())
                || estadisticas.stream().anyMatch(a -> a.getJugador().getId().equals(e.getJugador().getId()))) {
            return false;
        }
        estadisticas.add(e);
        return true;
    }

    public boolean registrarSancion(Sancion s) {
        if (s == null || s.getPartido() != this || cerrado || cancelado || s.getTipo() == null || deporte == null
                || !deporte.es("FUTBOL") || !esParticipante(s.getJugador())) {
            return false;
        }
        long repetidas = sanciones.stream().filter(a -> a.getJugador() == s.getJugador() && a.getTipo() == s.getTipo())
                .count();
        int maximoRepeticiones;
        if (s.getTipo() == TipoTarjeta.ROJA) {
            maximoRepeticiones = 1;
        } else {
            maximoRepeticiones = 2;
        }

        if (repetidas >= maximoRepeticiones) {
            return false;
        }
        sanciones.add(s);
        return true;
    }

    public boolean cancelar() {
        if (tieneResultado() || cerrado || cancelado) {
            return false;
        }
        cancelado = true;
        return true;
    }

    public boolean cerrarActa() {
        if (!tieneResultado() || cerrado || cancelado) {
            return false;
        }
        cerrado = true;
        return true;
    }

    public void restaurarEstado(boolean cancelado, boolean cerrado, boolean legado) {
        if ((legado && !tieneResultado()) || (cancelado && (cerrado || tieneResultado()))
                || (cerrado && !tieneResultado())) {
            throw new IllegalArgumentException("Estado de partido incompatible.");
        }
        this.cancelado = cancelado;
        this.cerrado = cerrado;
        this.legado = legado;
    }

    public List<EstadisticaPartido> getEstadisticas() {
        return List.copyOf(estadisticas);
    }

    public List<Sancion> getSanciones() {
        return List.copyOf(sanciones);
    }
}
