package modelo.partidos;

import java.util.HashMap;
import java.util.Map;
import modelo.usuarios.Jugador;

public class EstadisticaPartido {

    private Jugador jugador;
    private Partido partido;
    private Map<TipoEstadistica, Integer> metricas;
    private Integer eloResultante;

    public EstadisticaPartido(Jugador jugador, Partido partido) {
        super();
        this.jugador = jugador;
        this.partido = partido;
        this.metricas = new HashMap<>();
        this.eloResultante = null;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Partido getPartido() {
        return partido;
    }

    public boolean registrarMetrica(TipoEstadistica tipo, int valor) {
        if (tipo == null || partido == null || partido.estaCerrado() || partido.estaCancelado()) {
            return false;
        }

        if (valor < 0) {
            return false;
        }

        if (metricas.containsKey(tipo)) {
            return false;
        }

        if (!esMetricaCompatible(tipo)) {
            return false;
        }
        metricas.put(tipo, valor);
        return true;
    }

    public boolean esMetricaCompatible(TipoEstadistica tipo) {
        if (tipo == null || partido == null || partido.getDeporte() == null) {
            return false;
        }

        String deporte = partido.getDeporte().getNombre();

        if (deporte.equals("Fútbol")) {
            return (tipo == TipoEstadistica.GOLES || tipo == TipoEstadistica.ASISTENCIAS);
        } else if (deporte.equals("Baloncesto")) {
            return (tipo == TipoEstadistica.PUNTOS || tipo == TipoEstadistica.REBOTES);
        } else if (deporte.equals("Voleibol")) {
            return (tipo == TipoEstadistica.ACES || tipo == TipoEstadistica.BLOQUEOS);
        } else if (deporte.equals("Tenis") || deporte.equals("Pádel")) {
            return (tipo == TipoEstadistica.SETS || tipo == TipoEstadistica.GAMES || tipo == TipoEstadistica.ACES
                    || tipo == TipoEstadistica.ERRORES_NO_FORZADOS);
        } else if (deporte.equals("Tenis de mesa")) {
            return (tipo == TipoEstadistica.SETS || tipo == TipoEstadistica.PUNTOS);
        }

        return false;
    }

    public Integer obtenerMetrica(TipoEstadistica tipo) {
        return metricas.get(tipo);
    }

    public Map<TipoEstadistica, Integer> getMetricas() {
        return Map.copyOf(metricas);
    }

    public void cargarEloResultante(int elo) {
        if (elo < 0 || eloResultante != null) {
            throw new IllegalArgumentException("ELO histórico inválido.");
        }
        eloResultante = elo;
    }

    public Integer getEloResultante() {
        return eloResultante;
    }
}
