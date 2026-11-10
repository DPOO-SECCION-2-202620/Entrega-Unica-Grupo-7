package modelo.reservas;

import java.time.LocalDateTime;
import java.util.*;
import modelo.entrenamientos.Entrenamiento;
import modelo.partidos.Partido;

public class AgendaInstalacion {

    private final List<ActividadAgenda> actividades = new ArrayList<>();

    public List<ActividadAgenda> getActividades() {
        return List.copyOf(actividades);
    }

    public boolean estaDisponible(LocalDateTime inicio, LocalDateTime fin) {
        return estaDisponible(inicio, fin, null);
    }

    public boolean estaDisponible(LocalDateTime inicio, LocalDateTime fin, ActividadAgenda ignorar) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            return false;
        }
        return actividades.stream().filter(a -> a != ignorar && !a.estaCancelada())
                .noneMatch(a -> inicio.isBefore(a.getFin()) && fin.isAfter(a.getInicio()));
    }

    private boolean registrar(ActividadAgenda a) {
        if (a == null || a.getId() == null || a.getId().isBlank() || a.getInicio() == null || a.getFin() == null
                || !a.getFin().isAfter(a.getInicio())
                || actividades.stream().anyMatch(e -> e.getClass() == a.getClass() && e.getId().equals(a.getId()))
                || (!a.estaCancelada() && !estaDisponible(a.getInicio(), a.getFin()))) {
            return false;
        }
        actividades.add(a);
        return true;
    }

    public boolean registrarActividad(ActividadAgenda a) {
        return registrar(a);
    }

    public boolean retirarActividad(ActividadAgenda a) {
        return actividades.remove(a);
    }

    public boolean registrarReserva(Reserva r) {
        return registrar(r);
    }

    public boolean registrarEntrenamiento(Entrenamiento e) {
        return registrar(e);
    }

    public boolean registrarPartido(Partido p) {
        return p.getInstalacion() != null && registrar(p);
    }

    public boolean existeReserva(String id) {
        return actividades.stream().anyMatch(a -> a instanceof Reserva && a.getId().equals(id));
    }

    public boolean existeEntrenamiento(String id) {
        return actividades.stream().anyMatch(a -> a instanceof Entrenamiento && a.getId().equals(id));
    }
}
