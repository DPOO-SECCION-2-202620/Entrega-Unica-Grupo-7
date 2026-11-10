package modelo.partidos;

import java.time.LocalDateTime;
import java.util.List;
import modelo.deportes.*;
import modelo.usuarios.Jugador;

public class PartidoInterno extends Partido {

    private final Equipo local, visitante;
    private Integer marcadorLocal, marcadorVisitante;

    public PartidoInterno(String id, LocalDateTime fecha, Deporte deporte, Equipo local, Equipo visitante) {
        super(id, fecha, deporte);
        this.local = local;
        this.visitante = visitante;
    }

    public Equipo getLocal() {
        return local;
    }

    public Equipo getVisitante() {
        return visitante;
    }

    public Integer getMarcadorLocal() {
        return marcadorLocal;
    }

    public Integer getMarcadorVisitante() {
        return marcadorVisitante;
    }

    public boolean tieneResultado() {
        return marcadorLocal != null;
    }

    public List<Equipo> getEquiposClub() {
        if (local == null || visitante == null) {
            return List.of();
        } else {
            return List.of(local, visitante);
        }
    }

    protected boolean participanteLegado(Jugador j) {
        if (local == null || visitante == null) {
            return false;
        }

        if (tieneResultado()) {
            return java.util.stream.Stream
                    .concat(local.getHistorialInscripciones().stream(), visitante.getHistorialInscripciones().stream())
                    .anyMatch(i -> i.getJugador() == j);
        }
        return local.contieneJugador(j) || visitante.contieneJugador(j);
    }

    public boolean sonEquiposCompatibles() {
        return (local != null && visitante != null && getDeporte() != null && local.getDeporte() != null
                && visitante.getDeporte() != null && !local.getId().equals(visitante.getId())
                && local.getCategoria().equals(visitante.getCategoria())
                && local.getDeporte().getId().equals(visitante.getDeporte().getId())
                && local.getDeporte().getId().equals(getDeporte().getId()));
    }

    public int getMinimoJugadores() {
        if (getDeporte() == null) {
            return 0;
        } else {
            return getDeporte().getMinimoJugadores();
        }
    }

    public boolean puedeDisputarse() {
        int minimo = getMinimoJugadores();
        return (sonEquiposCompatibles() && local.estaActivo() && visitante.estaActivo() && minimo > 0
                && cumpleValidacion()
                && local.getInscripciones().stream().filter(i -> i.getJugador().estaActivo()).count() >= minimo
                && visitante.getInscripciones().stream().filter(i -> i.getJugador().estaActivo()).count() >= minimo);
    }

    public boolean registrarResultado(int l, int v) {
        return puedeDisputarse() && restaurarResultado(l, v);
    }

    public boolean restaurarResultado(int l, int v) {
        return restaurarResultado(l, v, false);
    }

    public boolean restaurarResultado(int l, int v, boolean legado) {
        if (!sonEquiposCompatibles()) {
            return false;
        }
        boolean marcadorAceptado;
        if (legado) {
            marcadorAceptado = marcadorLegadoValido(l, v);
        } else {
            marcadorAceptado = marcadorValido(l, v);
        }

        if (!marcadorAceptado) {
            return false;
        }
        marcadorLocal = l;
        marcadorVisitante = v;
        return true;
    }
}
