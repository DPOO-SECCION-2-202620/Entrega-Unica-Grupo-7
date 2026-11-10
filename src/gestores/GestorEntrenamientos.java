package gestores;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.entrenamientos.Entrenamiento;
import modelo.reservas.AgendaInstalacion;

public class GestorEntrenamientos {

    private java.util.function.Function<Entrenamiento, String> validador;

    public void configurarValidador(java.util.function.Function<Entrenamiento, String> validador) {
        this.validador = validador;
    }

    private List<Entrenamiento> entrenamientos;

    public GestorEntrenamientos() {
        super();
        this.entrenamientos = new ArrayList<>();
    }

    public List<Entrenamiento> getEntrenamientos() {
        return List.copyOf(entrenamientos);
    }

    public Entrenamiento buscarEntrenamiento(String id) {
        for (Entrenamiento entrenamiento : entrenamientos) {
            if (entrenamiento.getId().equals(id)) {
                return entrenamiento;
            }
        }
        return null;
    }

    public boolean registrarEntrenamiento(Entrenamiento entrenamiento) {
        return registrar(entrenamiento, true);
    }

    public boolean cargarEntrenamiento(Entrenamiento entrenamiento) {
        return registrar(entrenamiento, false);
    }

    private boolean registrar(Entrenamiento entrenamiento, boolean validar) {
        if (entrenamiento == null || entrenamiento.getId() == null) {
            return false;
        }

        if (buscarEntrenamiento(entrenamiento.getId()) != null) {
            return false;
        }

        if (entrenamiento.getInstalacion() == null) {
            return false;
        }

        if (entrenamiento.getId().isBlank() || entrenamiento.getInicio() == null
                || entrenamiento.getDuracionMinutos() <= 0 || entrenamiento.getEntrenador() == null) {
            return false;
        }

        if (!entrenamiento.estaCancelada() && validar && validador != null && validador.apply(entrenamiento) != null) {
            return false;
        }
        for (Entrenamiento existente : entrenamientos) {
            if (!entrenamiento.estaCancelada() && !existente.estaCancelada()
                    && existente.getEntrenador().getId().equals(entrenamiento.getEntrenador().getId())
                    && entrenamiento.getInicio().isBefore(existente.getFin())
                    && entrenamiento.getFin().isAfter(existente.getInicio())) {
                return false;
            }
        }
        AgendaInstalacion agenda = entrenamiento.getInstalacion().getAgenda();

        if (agenda == null) {
            return false;
        }

        if (!agenda.registrarEntrenamiento(entrenamiento)) {
            return false;
        }
        entrenamientos.add(entrenamiento);
        return true;
    }

    public List<Object> consultarAsistenciasPorEquipoYPeriodo(String idEquipo, LocalDate inicio, LocalDate fin) {
        List<Object> resultado = new ArrayList<>();
        for (Entrenamiento e : entrenamientos) {
            LocalDate fechaEntrenamiento = e.getInicio().toLocalDate();
            if (e.getEquipo() != null && e.getEquipo().getId().equals(idEquipo) && !fechaEntrenamiento.isBefore(inicio)
                    && !fechaEntrenamiento.isAfter(fin)) {
                resultado.add(e);
            }
        }
        return resultado;
    }

    public List<Object> consultarEntrenamientosRango(LocalDate inicio, LocalDate fin) {
        List<Object> resultado = new ArrayList<>();
        for (Entrenamiento e : entrenamientos) {
            LocalDate fechaEntrenamiento = e.getInicio().toLocalDate();
            if (!fechaEntrenamiento.isBefore(inicio) && !fechaEntrenamiento.isAfter(fin)) {
                resultado.add(e);
            }
        }
        return resultado;
    }
}
