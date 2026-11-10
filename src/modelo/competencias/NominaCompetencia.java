package modelo.competencias;

import java.util.ArrayList;
import java.util.List;
import modelo.deportes.Equipo;
import modelo.usuarios.Jugador;

public class NominaCompetencia {

    private Competencia competencia;
    private Equipo equipo;
    private final java.util.Set<String> actuales = new java.util.LinkedHashSet<>();
    private List<Jugador> jugadores;

    public NominaCompetencia(Competencia competencia, Equipo equipo) {
        super();
        this.competencia = competencia;
        this.equipo = equipo;
        this.jugadores = new ArrayList<>();
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public List<Jugador> getJugadores() {
        return new ArrayList<>(jugadores);
    }

    public boolean contieneJugador(Jugador jugador) {
        if (jugador == null) {
            return false;
        }
        for (Jugador jugadorActual : jugadores) {
            if (jugador.getId().equals(jugadorActual.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean agregarJugador(Jugador jugador) {
        if (jugador == null || equipo == null || competencia == null) {
            return false;
        }

        if (!equipo.contieneJugador(jugador)) {
            return false;
        }

        if (contieneJugador(jugador)) {
            return false;
        }

        if (!equipo.getDeporte().getId().equals(competencia.getDeporte().getId())) {
            return false;
        }

        if (!equipo.getCategoria().equals(competencia.getCategoria())) {
            return false;
        }
        jugadores.add(jugador);
        actuales.add(jugador.getId());
        return true;
    }

    public List<Jugador> getJugadoresActuales() {
        return jugadores.stream().filter(j -> actuales.contains(j.getId())).toList();
    }

    public void actualizarActuales(List<Jugador> nuevos) {
        if (nuevos == null || nuevos.stream().anyMatch(java.util.Objects::isNull)
                || nuevos.stream().map(Jugador::getId).distinct().count() != nuevos.size()) {
            throw new IllegalArgumentException("Nómina actual inválida.");
        }
        for (Jugador j : nuevos) {
            if (!contieneJugador(j)) {
                jugadores.add(j);
            }
        }
        actuales.clear();
        nuevos.forEach(j -> actuales.add(j.getId()));
    }

    public boolean cumpleMinimo() {
        if (competencia == null || competencia.getDeporte() == null || equipo == null || !equipo.estaActivo()) {
            return false;
        }
        int minimo = competencia.getDeporte().getMinimoJugadores();
        return (minimo > 0 && getJugadoresActuales().stream().filter(j -> j.estaActivo() && equipo.contieneJugador(j))
                .count() >= minimo);
    }

    public boolean cargarJugadorHistorico(Jugador jugador) {
        if (jugador == null || equipo == null || competencia == null || contieneJugador(jugador)
                || equipo.getHistorialInscripciones().stream().noneMatch(i -> i.getJugador() == jugador)) {
            return false;
        }
        jugadores.add(jugador);
        actuales.add(jugador.getId());
        return true;
    }
}
