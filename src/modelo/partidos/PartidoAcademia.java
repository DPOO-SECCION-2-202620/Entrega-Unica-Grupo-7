package modelo.partidos;

import java.time.LocalDateTime;
import java.util.*;
import modelo.deportes.*;
import modelo.usuarios.Jugador;

public class PartidoAcademia extends Partido {

    public record CambioElo(double anterior, double nuevo) {
    }

    private final List<Jugador> ladoA, ladoB;
    private final String modalidad;
    private final int diferenciaNiveles, k;
    private final boolean excepcionEntrenador;
    private final Map<String, CambioElo> cambiosElo = new LinkedHashMap<>();
    private Integer marcadorA, marcadorB;

    public PartidoAcademia(String id, LocalDateTime fecha, Deporte deporte, String modalidad, List<Jugador> a,
            List<Jugador> b, int diferenciaNiveles, boolean excepcionEntrenador, int k) {
        super(id, fecha, deporte);
        if (deporte == null || deporte.isColectivo() || modalidad == null || a == null || b == null
                || a.stream().anyMatch(Objects::isNull) || b.stream().anyMatch(Objects::isNull)
                || (diferenciaNiveles != 0 && diferenciaNiveles != 1) || k < 1 || k > 100) {
            throw new IllegalArgumentException("Partido de academia inválido.");
        }
        this.modalidad = modalidad;
        this.ladoA = List.copyOf(a);
        this.ladoB = List.copyOf(b);
        this.diferenciaNiveles = diferenciaNiveles;
        this.excepcionEntrenador = excepcionEntrenador;
        this.k = k;
        int setsParaGanar;
        if (deporte.es("MESA")) {
            setsParaGanar = 3;
        } else {
            setsParaGanar = 2;
        }
        configurarSetsParaGanar(setsParaGanar);
    }

    public String getModalidad() {
        return modalidad;
    }

    public List<Jugador> getLadoA() {
        return ladoA;
    }

    public List<Jugador> getLadoB() {
        return ladoB;
    }

    public List<Jugador> getJugadores() {
        List<Jugador> todos = new ArrayList<>(ladoA);
        todos.addAll(ladoB);
        return List.copyOf(todos);
    }

    public int getDiferenciaNiveles() {
        return diferenciaNiveles;
    }

    public boolean permiteExcepcionEntrenador() {
        return excepcionEntrenador;
    }

    public int getK() {
        return k;
    }

    public Integer getMarcadorA() {
        return marcadorA;
    }

    public Integer getMarcadorB() {
        return marcadorB;
    }

    public boolean tieneResultado() {
        return marcadorA != null;
    }

    public List<Equipo> getEquiposClub() {
        return List.of();
    }

    public Set<String> getUsuariosAgenda() {
        return Set.copyOf(getJugadores().stream().map(Jugador::getId).toList());
    }

    protected boolean participanteLegado(Jugador jugador) {
        return getJugadores().contains(jugador);
    }

    public Map<String, CambioElo> getCambiosElo() {
        return Map.copyOf(cambiosElo);
    }

    public boolean restaurarResultado(int a, int b) {
        if (tieneResultado() || estaCancelado() || estaCerrado() || a < 0 || b < 0
                || Math.max(a, b) != getSetsParaGanar() || Math.min(a, b) >= getSetsParaGanar()) {
            return false;
        }
        marcadorA = a;
        marcadorB = b;
        return true;
    }

    public void cargarCambioElo(String jugador, CambioElo cambio) {
        if (cambio == null || !Double.isFinite(cambio.anterior()) || !Double.isFinite(cambio.nuevo())
                || cambio.anterior() < 0 || cambio.nuevo() < 0
                || getJugadores().stream().noneMatch(j -> j.getId().equals(jugador))
                || cambiosElo.putIfAbsent(jugador, cambio) != null) {
            throw new IllegalArgumentException("Movimiento ELO inválido o duplicado.");
        }
    }
}
