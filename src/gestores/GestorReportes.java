package gestores;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import modelo.entrenamientos.*;
import modelo.usuarios.Jugador;
import modelo.ventas.*;

public class GestorReportes {

    private GestorVentas gestorVentas;
    private GestorSocios gestorSocios;
    private GestorPartidos gestorPartidos;
    private GestorEntrenamientos gestorEntrenamientos;
    private GestorReservas gestorReservas;

    public GestorReportes(GestorVentas gestorVentas, GestorSocios gestorSocios, GestorPartidos gestorPartidos,
            GestorEntrenamientos gestorEntrenamientos, GestorReservas gestorReservas) {
        super();
        this.gestorVentas = gestorVentas;
        this.gestorSocios = gestorSocios;
        this.gestorPartidos = gestorPartidos;
        this.gestorEntrenamientos = gestorEntrenamientos;
        this.gestorReservas = gestorReservas;
    }

    public record ResumenFinanciero(double subtotal, double descuento, double base, double impuesto,
            long puntosRedimidos, double comercialPagado, double propina, double ventasPagadas, double mensualidades) {
        public double ingresoSinPropinas() {
            return Dinero.redondear(comercialPagado + mensualidades);
        }
    }

    public record EvolucionRanking(String jugadorId, String nombre, String disciplina, LocalDate fecha, double anterior,
            double nuevo, String referencia) {
    }

    public record DetalleAsistencia(String equipoId, String entrenamientoId, String jugadorId, String nombre,
            java.time.LocalDateTime fecha, EstadoAsistencia estado, Double calificacion, String comentario) {
    }

    public ResumenFinanciero consultarDesgloseFinanciero(String rubro, LocalDate inicio, LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        double subtotal = 0, descuento = 0, base = 0, impuesto = 0, comercial = 0, propina = 0, total = 0;
        long puntos = 0;
        for (Venta v : gestorVentas.getVentasRegistradas()) {
            if ((rubro == null || v.getRubro().equalsIgnoreCase(rubro)) && !v.getFecha().isBefore(inicio)
                    && !v.getFecha().isAfter(fin)) {
                subtotal += v.getSubtotal();
                descuento += v.getDescuento();
                base += v.getBaseImponible();
                impuesto += v.getImpuesto();
                puntos += v.getPuntosRedimidos();
                comercial += v.getValorFinalPagado() - v.getPropina();
                propina += v.getPropina();
                total += v.getValorFinalPagado();
            }
        }
        return new ResumenFinanciero(Dinero.redondear(subtotal), Dinero.redondear(descuento), Dinero.redondear(base),
                Dinero.redondear(impuesto), puntos, Dinero.redondear(comercial), Dinero.redondear(propina),
                Dinero.redondear(total), gestorSocios.consultarTotalMensualidadesPorPeriodo(inicio, fin));
    }

    public List<EvolucionRanking> consultarEvolucion(String disciplina, LocalDate inicio, LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        List<EvolucionRanking> resultado = new ArrayList<>();
        for (Jugador j : gestorSocios.getJugadoresRegistrados()) {
            for (Jugador.MovimientoRanking m : j.getMovimientosRanking()) {
                if ((disciplina == null || m.disciplina().equals(modelo.deportes.Deporte.clave(disciplina)))
                        && !m.fecha().isBefore(inicio) && !m.fecha().isAfter(fin)) {
                    resultado.add(new EvolucionRanking(j.getId(), j.getNombre(), m.disciplina(), m.fecha(),
                            m.anterior(), m.nuevo(), m.referencia()));
                }
            }
        }
        resultado.sort(
                java.util.Comparator.comparing(EvolucionRanking::fecha).thenComparing(EvolucionRanking::jugadorId));
        return List.copyOf(resultado);
    }

    public List<DetalleAsistencia> consultarDetalleAsistencia(String equipoId, LocalDate inicio, LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        List<DetalleAsistencia> resultado = new ArrayList<>();
        for (Object objeto : gestorEntrenamientos.consultarAsistenciasPorEquipoYPeriodo(equipoId, inicio, fin)) {
            Entrenamiento e = (Entrenamiento) objeto;
            for (AsistenciaEvaluacion a : e.getRegistroAsistencia()) {
                resultado.add(
                        new DetalleAsistencia(equipoId, e.getId(), a.getJugador().getId(), a.getJugador().getNombre(),
                                e.getInicio(), a.getEstadoAsistencia(), a.getCalificacion(), a.getComentario()));
            }
        }
        return List.copyOf(resultado);
    }

    public record ResultadoPartido(String partidoId, String deporte, java.time.LocalDateTime fecha, String local,
            String visitante, Integer puntosLocal, Integer puntosVisitante) {
    }

    public record EstadisticaJugador(String partidoId, String jugadorId, String nombre, String deporte,
            java.time.LocalDateTime fecha, Map<modelo.partidos.TipoEstadistica, Integer> metricas,
            List<modelo.partidos.TipoTarjeta> tarjetas) {
    }

    public List<ResultadoPartido> consultarResultados(String deporte, LocalDate inicio, LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        List<ResultadoPartido> resultado = new ArrayList<>();
        for (Object objeto : gestorPartidos.consultarPartidosPorDeporteYPeriodo(deporte, inicio, fin)) {
            modelo.partidos.Partido p = (modelo.partidos.Partido) objeto;
            if (p instanceof modelo.partidos.PartidoInterno i) {
                resultado.add(
                        new ResultadoPartido(p.getId(), p.getDeporte().getId(), p.getFechaHora(), i.getLocal().getId(),
                                i.getVisitante().getId(), i.getMarcadorLocal(), i.getMarcadorVisitante()));
            } else if (p instanceof modelo.partidos.PartidoCompetencia c) {
                resultado.add(new ResultadoPartido(p.getId(), p.getDeporte().getId(), p.getFechaHora(),
                        c.getEquipoClub().getId(), c.getInstitucionRival(), c.getMarcadorClub(), c.getMarcadorRival()));
            }
        }
        return List.copyOf(resultado);
    }

    public List<EstadisticaJugador> consultarEstadisticasJugador(String jugadorId, LocalDate inicio, LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        List<EstadisticaJugador> resultado = new ArrayList<>();
        for (modelo.partidos.Partido p : gestorPartidos.getPartidos()) {
            if (!p.getFechaHora().toLocalDate().isBefore(inicio) && !p.getFechaHora().toLocalDate().isAfter(fin)) {
                for (var e : p.getEstadisticas()) {
                    if (e.getJugador().getId().equals(jugadorId)) {
                        resultado.add(new EstadisticaJugador(p.getId(), jugadorId, e.getJugador().getNombre(),
                                p.getDeporte().getId(), p.getFechaHora(), e.getMetricas(),
                                p.getSanciones().stream().filter(sa -> sa.getJugador().getId().equals(jugadorId))
                                        .map(modelo.partidos.Sancion::getTipo).toList()));
                    }
                }
            }
        }
        return List.copyOf(resultado);
    }

    public record AcumuladoJugador(String jugadorId, String nombre, String deporte, int partidos,
            Map<modelo.partidos.TipoEstadistica, Integer> metricas, int amarillas, int rojas) {
    }

    /** Una fila por identidad y disciplina; únicamente actas definitivas, sin sumar ELO como métrica. */
    public List<AcumuladoJugador> acumularEstadisticas(String deporte, String competencia, LocalDate inicio,
            LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        record Clave(String id, String deporte) {
        }
        Map<Clave, java.util.Set<String>> partidos = new java.util.LinkedHashMap<>();
        Map<Clave, Map<modelo.partidos.TipoEstadistica, Integer>> sumas = new java.util.LinkedHashMap<>();
        Map<Clave, int[]> tarjetas = new java.util.LinkedHashMap<>();
        Map<Clave, String> nombres = new java.util.LinkedHashMap<>();
        for (var p : gestorPartidos.getPartidos()) {
            if (p.estaCancelado() || !p.tieneResultado() || (!p.estaCerrado() && !p.esLegado())
                    || (deporte != null && !p.getDeporte().es(deporte))
                    || p.getFechaHora().toLocalDate().isBefore(inicio) || p.getFechaHora().toLocalDate().isAfter(fin)
                    || (competencia != null && (!(p instanceof modelo.partidos.PartidoCompetencia pc)
                            || !pc.getCompetencia().getId().equals(competencia)))) {
                continue;
            }
            java.util.Set<Jugador> participantes = new java.util.LinkedHashSet<>();
            p.getSelecciones().values().forEach(participantes::addAll);
            if (p instanceof modelo.partidos.PartidoAcademia a) {
                participantes.addAll(a.getJugadores());
            }
            p.getEstadisticas().forEach(e -> participantes.add(e.getJugador()));
            p.getSanciones().forEach(e -> participantes.add(e.getJugador()));
            for (Jugador j : participantes) {
                if (p.esParticipante(j)) {
                    Clave k = new Clave(j.getId(), p.getDeporte().getId());
                    nombres.put(k, j.getNombre());
                    partidos.computeIfAbsent(k, x -> new java.util.HashSet<>()).add(p.getId());
                    Map<modelo.partidos.TipoEstadistica, Integer> m = sumas.computeIfAbsent(k,
                            x -> new java.util.EnumMap<>(modelo.partidos.TipoEstadistica.class));
                    p.getEstadisticas().stream().filter(e -> e.getJugador() == j)
                            .forEach(e -> e.getMetricas().forEach((t, v) -> m.merge(t, v, Integer::sum)));
                    int[] t = tarjetas.computeIfAbsent(k, x -> new int[2]);
                    p.getSanciones().stream().filter(e -> e.getJugador() == j).forEach(e -> {
                        if (e.getTipo() == modelo.partidos.TipoTarjeta.AMARILLA) {
                            t[0]++;
                        } else {
                            t[1]++;
                        }
                    });
                }
            }
        }
        List<AcumuladoJugador> filas = new ArrayList<>();
        partidos.forEach((k, v) -> filas.add(new AcumuladoJugador(k.id(), nombres.get(k), k.deporte(), v.size(),
                Map.copyOf(sumas.get(k)), tarjetas.get(k)[0], tarjetas.get(k)[1])));
        filas.sort(
                java.util.Comparator.comparing(AcumuladoJugador::deporte).thenComparing(AcumuladoJugador::jugadorId));
        return List.copyOf(filas);
    }

    public double consultarReporteFinancieroVentas(String rubro, LocalDate fechaInicio, LocalDate fechaFin) {
        return gestorVentas.consultarVentasPorRubroYPeriodo(rubro, fechaInicio, fechaFin);
    }

    public double consultarReporteFinancieroMensualidades(LocalDate fechaInicio, LocalDate fechaFin) {
        return gestorSocios.consultarTotalMensualidadesPorPeriodo(fechaInicio, fechaFin);
    }

    public List<Object> generarReporteAsistenciaEntrenamientos(String idEquipo, LocalDate fechaInicio,
            LocalDate fechaFin) {
        return gestorEntrenamientos.consultarAsistenciasPorEquipoYPeriodo(idEquipo, fechaInicio, fechaFin);
    }

    public List<Object> generarReporteResultadosPartidos(String deporte, LocalDate fechaInicio, LocalDate fechaFin) {
        return gestorPartidos.consultarPartidosPorDeporteYPeriodo(deporte, fechaInicio, fechaFin);
    }

    public Map<String, Double> generarReporteEvolucionRankings(String disciplina) {
        return gestorSocios.consultarRankingsDisciplina(disciplina);
    }

    public List<Object> consultarHistorialReservas(LocalDate fechaInicio, LocalDate fechaFin) {
        return gestorReservas.consultarReservasRango(fechaInicio, fechaFin);
    }

    public List<Object> consultarHistorialPartidos(LocalDate fechaInicio, LocalDate fechaFin) {
        return gestorPartidos.consultarPartidosRango(fechaInicio, fechaFin);
    }

    public List<Object> consultarHistorialEntrenamientos(LocalDate fechaInicio, LocalDate fechaFin) {
        return gestorEntrenamientos.consultarEntrenamientosRango(fechaInicio, fechaFin);
    }
}
