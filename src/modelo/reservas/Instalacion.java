package modelo.reservas;

import modelo.deportes.Deporte;

public class Instalacion {

    private final String id;
    private final boolean techada;
    private String nombre;
    private Deporte deporte;
    private int capacidadMaxima;
    private AgendaInstalacion agenda;

    public Instalacion(String id, String nombre, Deporte deporte, int capacidadMaxima) {
        this(id, nombre, deporte, capacidadMaxima, true);
    }

    public Instalacion(String id, String nombre, Deporte deporte, int capacidadMaxima, boolean techada) {
        this.techada = techada;
        this.id = id;
        this.nombre = nombre;
        this.deporte = deporte;
        this.capacidadMaxima = capacidadMaxima;
        this.agenda = new AgendaInstalacion();
    }

    public boolean esTechada() {
        return techada;
    }

    public boolean restringeBebidasCalientes() {
        return (deporte != null
                && ("PADEL".equalsIgnoreCase(deporte.getId()) || "Pádel".equalsIgnoreCase(deporte.getNombre())
                        || (techada && ("MESA".equalsIgnoreCase(deporte.getId())
                                || "Tenis de mesa".equalsIgnoreCase(deporte.getNombre())))));
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public AgendaInstalacion getAgenda() {
        return agenda;
    }

    public boolean admiteParticipantes(int cantidad) {
        if (deporte == null || deporte.getNombre() == null) {
            return false;
        }

        if (cantidad <= 0 || cantidad > capacidadMaxima) {
            return false;
        }

        if (deporte.getNombre().equals("Tenis")) {
            if (cantidad != 2 && cantidad != 4) {
                return false;
            }
        } else if (deporte.getNombre().equals("Pádel")) {
            if (cantidad != 4) {
                return false;
            }
        } else if (deporte.getNombre().equals("Tenis de mesa")) {
            if (cantidad != 2 && cantidad != 4) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
