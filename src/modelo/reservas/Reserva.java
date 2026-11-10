package modelo.reservas;

import java.time.LocalDateTime;
import modelo.usuarios.Jugador;

public class Reserva implements ActividadAgenda {

    private boolean cancelada;
    private final java.util.List<CambioAgenda> cambios =
        new java.util.ArrayList<>();
    private final String id;
    private Jugador jugador;
    private Instalacion instalacion;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private int cantidadParticipantes;
    private String modalidad;

    public Reserva(
        String id,
        Jugador jugador,
        Instalacion instalacion,
        LocalDateTime inicio,
        LocalDateTime fin,
        int cantidadParticipantes,
        String modalidad
    ) {
        super();
        this.id = id;
        this.jugador = jugador;
        this.instalacion = instalacion;
        this.inicio = inicio;
        this.fin = fin;
        this.cantidadParticipantes = cantidadParticipantes;
        this.modalidad = modalidad;
    }

    public boolean estaCancelada() {
        return cancelada;
    }

    public void restaurarCancelada(boolean valor) {
        cancelada = valor;
    }

    public java.util.Set<String> getUsuariosAgenda() {
        return java.util.Set.of(jugador.getId());
    }

    public java.util.List<CambioAgenda> getCambiosAgenda() {
        return java.util.List.copyOf(cambios);
    }

    public void cargarCambio(CambioAgenda cambio) {
        cambios.add(cambio);
    }

    public void restaurarHorario(
        Instalacion instalacion,
        LocalDateTime inicio,
        LocalDateTime fin
    ) {
        this.instalacion = instalacion;
        this.inicio = inicio;
        this.fin = fin;
    }

    public String getId() {
        return id;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Instalacion getInstalacion() {
        return instalacion;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public int getCantidadParticipantes() {
        return cantidadParticipantes;
    }

    public String getModalidad() {
        return modalidad;
    }
}
