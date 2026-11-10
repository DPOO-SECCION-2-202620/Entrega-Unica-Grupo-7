package gestores;

import java.time.*;
import java.util.*;
import modelo.socios.PagoMensualidad;
import modelo.usuarios.Jugador;
import modelo.ventas.Dinero;

public class GestorSocios {

    private final Map<String, Jugador> jugadores;
    private final Map<String, PagoMensualidad> pagos = new LinkedHashMap<>();

    public GestorSocios() {
        this(new LinkedHashMap<>());
    }

    public GestorSocios(Map<String, Jugador> jugadores) {
        this.jugadores = jugadores;
    }

    public boolean registrarJugador(Jugador j) {
        if (j == null || j.getId() == null || j.getId().isBlank() || jugadores.containsKey(j.getId())) {
            return false;
        }
        jugadores.put(j.getId(), j);
        return true;
    }

    public Jugador buscarJugadorPorId(String id) {
        return jugadores.get(id);
    }

    public PagoMensualidad buscarPago(String id) {
        return pagos.get(id);
    }

    public boolean registrarPagoMensualidad(PagoMensualidad pago) {
        return agregar(pago, true);
    }

    public void cargarPago(PagoMensualidad pago) {
        if (!agregar(pago, false)) {
            throw new IllegalArgumentException("Pago inválido, duplicado o ajeno al registro.");
        }
    }

    private boolean agregar(PagoMensualidad pago, boolean exigirActivo) {
        if (pago == null || pago.getIdPago() == null || pago.getIdPago().isBlank()
                || pagos.containsKey(pago.getIdPago()) || pago.getJugador() == null
                || jugadores.get(pago.getJugador().getId()) != pago.getJugador()
                || (exigirActivo && !pago.getJugador().estaActivo()) || !Double.isFinite(pago.getImporteRecibido())
                || pago.getImporteRecibido() <= 0 || pago.getFechaPago() == null || pago.getPeriodo() == null) {
            return false;
        }
        try {
            if (!YearMonth.parse(pago.getPeriodo()).toString().equals(pago.getPeriodo())) {
                return false;
            }
        } catch (DateTimeException e) {
            return false;
        }
        pagos.put(pago.getIdPago(), pago);
        return true;
    }

    public double consultarTotalMensualidadesPorPeriodo(LocalDate inicio, LocalDate fin) {
        validarPeriodo(inicio, fin);
        return Dinero.redondear(pagos.values().stream()
                .filter(p -> !p.getFechaPago().isBefore(inicio) && !p.getFechaPago().isAfter(fin))
                .mapToDouble(PagoMensualidad::getImporteRecibido).sum());
    }

    public static void validarPeriodo(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new IllegalArgumentException("Periodo inválido.");
        }
    }

    public Map<String, Double> consultarRankingsDisciplina(String disciplina) {
        Map<String, Double> resultado = new LinkedHashMap<>();
        for (Jugador j : jugadores.values()) {
            if (j.getRankingsPorDisciplina().containsKey(modelo.deportes.Deporte.clave(disciplina))) {
                resultado.put(j.getId(), j.obtenerRankingPorDisciplina(disciplina));
            }
        }
        return resultado;
    }

    public List<Jugador> getJugadoresRegistrados() {
        return List.copyOf(jugadores.values());
    }

    public List<PagoMensualidad> getPagosMensualidades() {
        return List.copyOf(pagos.values());
    }
}
