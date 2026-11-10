package gestores;

import aplicacion.ResultadoOperacion;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import modelo.reservas.*;
import modelo.usuarios.Jugador;

public class GestorReservas {

    private final List<Instalacion> instalaciones = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private Function<Reserva, String> validador;

    public void configurarValidador(Function<Reserva, String> validador) {
        this.validador = validador;
    }

    public List<Instalacion> getInstalaciones() {
        return List.copyOf(instalaciones);
    }

    public List<Reserva> getReservas() {
        return List.copyOf(reservas);
    }

    public boolean registrarInstalacion(Instalacion i) {
        if (i == null || i.getId() == null || i.getId().isBlank() || i.getNombre() == null || i.getNombre().isBlank()
                || i.getDeporte() == null || i.getCapacidadMaxima() < 1 || buscarInstalacion(i.getId()) != null) {
            return false;
        }
        instalaciones.add(i);
        return true;
    }

    public Instalacion buscarInstalacion(String id) {
        return instalaciones.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    public Reserva buscarReserva(String id) {
        return reservas.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    public String datosInvalidos(Reserva r) {
        if (r == null || r.getId() == null || r.getId().isBlank() || r.getJugador() == null
                || r.getInstalacion() == null || r.getInicio() == null || r.getFin() == null
                || !r.getFin().isAfter(r.getInicio())) {
            return "Datos de reserva o intervalo inválidos.";
        }

        if (buscarReserva(r.getId()) != null) {
            return "ID de reserva repetido.";
        }

        if (buscarInstalacion(r.getInstalacion().getId()) != r.getInstalacion()) {
            return "Instalación inexistente.";
        }

        if (r.getModalidad() == null
                || (!r.getModalidad().equalsIgnoreCase("Sencillos") && !r.getModalidad().equalsIgnoreCase("Dobles"))) {
            return "Modalidad desconocida.";
        }

        if (r.getInstalacion().getDeporte().es("PADEL")
                && (r.getCantidadParticipantes() != 4 || !r.getModalidad().equalsIgnoreCase("Dobles"))) {
            return "Pádel exige dobles y cuatro participantes.";
        }
        int exigidos;
        if (r.getModalidad().equalsIgnoreCase("Dobles")) {
            exigidos = 4;
        } else {
            exigidos = 2;
        }

        if (r.getCantidadParticipantes() != exigidos || !r.getInstalacion().admiteParticipantes(exigidos)) {
            return "Cantidad o capacidad incompatible con la modalidad.";
        }
        return null;
    }

    public ResultadoOperacion registrarConMotivo(Reserva r) {
        String causa = datosInvalidos(r);
        if (causa == null && !r.getJugador().estaActivo()) {
            causa = "Jugador dado de baja.";
        }

        if (causa == null && validador != null) {
            causa = validador.apply(r);
        }

        if (causa != null) {
            return ResultadoOperacion.rechazo(causa);
        }

        if (cargarInterna(r)) {
            return ResultadoOperacion.correcto("Reserva creada.");
        } else {
            return ResultadoOperacion.rechazo("No hay disponibilidad en esa instalación.");
        }
    }

    private boolean cargarInterna(Reserva r) {
        if (!r.getInstalacion().getAgenda().registrarReserva(r)) {
            return false;
        }
        reservas.add(r);
        return true;
    }

    public void cargarReserva(Reserva r) {
        String causa = datosInvalidos(r);
        if (causa != null || !cargarInterna(r)) {
            String motivoRechazo;
            if (causa == null) {
                motivoRechazo = "Cruce en reserva guardada.";
            } else {
                motivoRechazo = causa;
            }
            throw new IllegalArgumentException(motivoRechazo);
        }
    }

    public boolean realizarReserva(String id, Jugador j, Instalacion i, LocalDateTime inicio, LocalDateTime fin,
            int cantidad, String modalidad) {
        return registrarConMotivo(new Reserva(id, j, i, inicio, fin, cantidad, modalidad)).exito();
    }

    public List<Object> consultarReservasRango(LocalDate inicio, LocalDate fin) {
        GestorSocios.validarPeriodo(inicio, fin);
        return new ArrayList<>(reservas.stream()
                .filter(r -> !r.getInicio().toLocalDate().isBefore(inicio) && !r.getInicio().toLocalDate().isAfter(fin))
                .toList());
    }
}
