package modelo.entrenamientos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import modelo.deportes.Deporte;
import modelo.deportes.Equipo;
import modelo.deportes.NivelAcademia;
import modelo.reservas.*;
import modelo.usuarios.Empleado;
import modelo.usuarios.Jugador;

public class Entrenamiento implements ActividadAgenda {

    private boolean cancelado;
    private Deporte disciplina;
    private NivelAcademia nivelGrupo;
    private final List<CambioAgenda> cambios = new ArrayList<>();
    private final String id;
    private final Equipo equipo;
    private LocalDateTime inicio;
    private int duracionMinutos;
    private Empleado entrenador;
    private Instalacion instalacion;
    private List<Jugador> convocados;
    private List<AsistenciaEvaluacion> registroAsistencia;

    public Entrenamiento(String id, LocalDateTime inicio, int duracionMinutos, Empleado entrenador,
            Instalacion instalacion) {
        this(id, inicio, duracionMinutos, entrenador, instalacion, null);
    }

    public Entrenamiento(String id, LocalDateTime inicio, int duracionMinutos, Empleado entrenador,
            Instalacion instalacion, Equipo equipo) {
        this.equipo = equipo;
        this.id = id;
        this.inicio = inicio;
        this.duracionMinutos = duracionMinutos;
        this.entrenador = entrenador;
        this.instalacion = instalacion;
        this.convocados = new ArrayList<>();
        this.registroAsistencia = new ArrayList<>();
    }

    public Deporte getDisciplina() {
        if (equipo == null) {
            return disciplina;
        } else {
            return equipo.getDeporte();
        }
    }

    public NivelAcademia getNivelGrupo() {
        return nivelGrupo;
    }

    public void definirGrupoAcademia(Deporte disciplina, NivelAcademia nivel) {
        if (equipo != null || disciplina == null || disciplina.isColectivo() || nivel == null) {
            throw new IllegalArgumentException("Grupo de academia inválido.");
        }
        this.disciplina = disciplina;
        this.nivelGrupo = nivel;
    }

    public boolean estaCancelada() {
        return cancelado;
    }

    public void restaurarCancelado(boolean valor) {
        cancelado = valor;
    }

    public java.util.Set<String> getUsuariosAgenda() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        ids.add(entrenador.getId());
        convocados.forEach(j -> ids.add(j.getId()));
        return java.util.Set.copyOf(ids);
    }

    public List<CambioAgenda> getCambiosAgenda() {
        return List.copyOf(cambios);
    }

    public void cargarCambio(CambioAgenda cambio) {
        cambios.add(cambio);
    }

    public void restaurarHorario(Instalacion instalacion, LocalDateTime inicio, int duracion) {
        this.instalacion = instalacion;
        this.inicio = inicio;
        this.duracionMinutos = duracion;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public List<Jugador> getConvocados() {
        return List.copyOf(convocados);
    }

    public List<AsistenciaEvaluacion> getRegistroAsistencia() {
        return List.copyOf(registroAsistencia);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public Empleado getEntrenador() {
        return entrenador;
    }

    public Instalacion getInstalacion() {
        return instalacion;
    }

    public LocalDateTime getFin() {
        if (inicio == null) {
            return null;
        } else {
            return inicio.plusMinutes(duracionMinutos);
        }
    }

    public boolean estaConvocado(Jugador jugador) {
        if (jugador == null) {
            return false;
        }
        for (Jugador jugadorActual : convocados) {
            if (jugadorActual.getId().equals(jugador.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean convocarJugador(Jugador jugador) {
        if (cancelado || jugador == null || estaConvocado(jugador)) {
            return false;
        }
        convocados.add(jugador);
        return true;
    }

    public boolean tieneRegistroAsistencia(Jugador jugador) {
        if (jugador == null) {
            return false;
        }
        for (AsistenciaEvaluacion asistencia : registroAsistencia) {
            if (asistencia.getJugador().getId().equals(jugador.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean registrarAsistencia(AsistenciaEvaluacion registro) {
        if (cancelado || registro == null || registro.getJugador() == null || registro.getEstadoAsistencia() == null) {
            return false;
        }

        if (registro.getEntrenamiento() != this) {
            return false;
        }

        if (!estaConvocado(registro.getJugador())) {
            return false;
        }

        if (tieneRegistroAsistencia(registro.getJugador())) {
            return false;
        }
        Double nota = registro.getCalificacion();

        if (nota != null && (!Double.isFinite(nota) || nota < 1 || nota > 5)) {
            return false;
        }
        registroAsistencia.add(registro);
        return true;
    }
}
