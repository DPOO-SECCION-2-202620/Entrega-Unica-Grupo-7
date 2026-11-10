package gestores;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.partidos.Partido;

public class GestorPartidos {

    private java.util.function.Function<Partido, String> validadorAgenda;

    public void configurarValidadorAgenda(java.util.function.Function<Partido, String> validador) {
        validadorAgenda = validador;
    }

    private java.util.function.Function<Partido, Boolean> verificador;

    public void configurarVerificador(java.util.function.Function<Partido, Boolean> verificador) {
        this.verificador = verificador;
    }

    private List<Partido> partidosRegistrados;

    public GestorPartidos() {
        super();
        this.partidosRegistrados = new ArrayList<>();
    }

    public List<Partido> getPartidos() {
        return List.copyOf(partidosRegistrados);
    }

    public Partido buscarPartido(String id) {
        return partidosRegistrados.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    public boolean registrarPartido(Partido partido) {
        return registrar(partido, true);
    }

    public boolean cargarPartido(Partido partido) {
        return registrar(partido, false);
    }

    private boolean registrar(Partido partido, boolean validar) {
        if (partido == null || partido.getId() == null || partido.getId().isBlank() || partido.getFechaHora() == null
                || partido.getDeporte() == null || buscarPartido(partido.getId()) != null) {
            return false;
        }

        if (validar && !partido.estaCancelado() && validadorAgenda != null && validadorAgenda.apply(partido) != null) {
            return false;
        }

        if (partido.getInstalacion() != null && !partido.getInstalacion().getAgenda().registrarPartido(partido)) {
            return false;
        }

        if (verificador != null) {
            partido.vincularValidacion(() -> verificador.apply(partido));
        }
        partidosRegistrados.add(partido);
        return true;
    }

    public List<Object> consultarPartidosPorDeporteYPeriodo(String nombreDeporte, LocalDate fechaInicio,
            LocalDate fechaFin) {
        List<Object> resultado = new ArrayList<>();

        for (Partido partido : partidosRegistrados) {
            boolean coincideDeporte = partido.getDeporte() != null && partido.getDeporte().getNombre() != null
                    && partido.getDeporte().es(nombreDeporte);

            LocalDate fechaPartido;
            if (partido.getFechaHora() != null) {
                fechaPartido = partido.getFechaHora().toLocalDate();
            } else {
                fechaPartido = null;
            }

            boolean enRango = fechaPartido != null && !fechaPartido.isBefore(fechaInicio)
                    && !fechaPartido.isAfter(fechaFin);

            if (coincideDeporte && enRango) {
                resultado.add(partido);
            }
        }

        return resultado;
    }

    public List<Object> consultarPartidosRango(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Object> resultado = new ArrayList<>();

        for (Partido partido : partidosRegistrados) {
            LocalDate fechaPartido;
            if (partido.getFechaHora() != null) {
                fechaPartido = partido.getFechaHora().toLocalDate();
            } else {
                fechaPartido = null;
            }

            if (fechaPartido != null && !fechaPartido.isBefore(fechaInicio) && !fechaPartido.isAfter(fechaFin)) {
                resultado.add(partido);
            }
        }

        return resultado;
    }
}
