package modelo.deportes;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import modelo.usuarios.Jugador;

public class Equipo {

    private boolean activo = true;
    private final String id;
    private String nombre;
    private Deporte deporte;
    private String categoria;
    private int cupoMaximo;
    private List<InscripcionEquipo> inscripciones;

    public Equipo(String id, String nombre, Deporte deporte, String categoria, int cupoMaximo) {
        super();
        this.id = id;
        this.nombre = nombre;
        this.deporte = deporte;
        this.categoria = categoria;
        this.cupoMaximo = cupoMaximo;
        this.inscripciones = new ArrayList<>();
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

    public String getCategoria() {
        return categoria;
    }

    public int getCupoMaximo() {
        return cupoMaximo;
    }

    public boolean estaActivo() {
        return activo;
    }

    public void restaurarActivo(boolean activo) {
        this.activo = activo;
    }

    public List<InscripcionEquipo> getHistorialInscripciones() {
        return List.copyOf(inscripciones);
    }

    public List<InscripcionEquipo> getInscripciones() {
        return inscripciones.stream().filter(InscripcionEquipo::estaActiva).toList();
    }

    public int getCantidadInscritos() {
        return getInscripciones().size();
    }

    public boolean tieneCupo() {
        if (getCantidadInscritos() >= getCupoMaximo()) {
            return false;
        }
        return true;
    }

    public boolean estaDisponibleNumero(int numero) {
        for (int i = 0; i < inscripciones.size(); i++) {
            if (inscripciones.get(i).estaActiva() && inscripciones.get(i).getNumeroCamiseta() == numero) {
                return false;
            }
        }
        return true;
    }

    public boolean agregarInscripcion(InscripcionEquipo i) {
        return vincularInscripcion(i, LocalDate.now(java.time.ZoneId.of("America/Bogota")));
    }

    /** La referencia nula se reserva para recuperar inscripciones históricas. */
    public boolean vincularInscripcion(InscripcionEquipo i, LocalDate referencia) {
        if (i == null || i.getEquipo() != this || i.getJugador() == null || i.getPosicion() == null
                || i.getPosicion().isBlank() || i.getNumeroCamiseta() <= 0 || inscripciones.contains(i)) {
            return false;
        }

        if (i.estaActiva()) {
            if ((referencia != null
                    && (!activo || !i.getJugador().estaActivo() || !esCategoriaCompatible(i.getJugador(), referencia)))
                    || !tieneCupo() || !estaDisponibleNumero(i.getNumeroCamiseta()) || contieneJugador(i.getJugador())
                    || i.getJugador().getInscripcionesEquipo().stream()
                            .anyMatch(in -> in.getEquipo().getDeporte().es(deporte.getId()))) {
                return false;
            }
        }
        inscripciones.add(i);
        if (i.getJugador().agregarInscripcionEquipo(i)) {
            return true;
        }
        inscripciones.remove(i);
        return false;
    }

    public boolean contieneJugador(Jugador jugador) {
        for (InscripcionEquipo inscripcion : inscripciones) {
            if (jugador != null && inscripcion.estaActiva()
                    && inscripcion.getJugador().getId().equals(jugador.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean esCategoriaCompatible(Jugador jugador) {
        return esCategoriaCompatible(jugador, LocalDate.now(java.time.ZoneId.of("America/Bogota")));
    }

    public boolean esCategoriaCompatible(Jugador jugador, LocalDate referencia) {
        if (jugador == null || jugador.getFechaNacimiento() == null || referencia == null
                || jugador.getFechaNacimiento().isAfter(referencia)) {
            return false;
        }

        LocalDate fechaNacimiento = jugador.getFechaNacimiento();
        int edad = jugador.getEdad(referencia);

        if (categoria.equals("Sub-13")) {
            return edad < 13;
        } else if (categoria.equals("Sub-15")) {
            return edad >= 13 && edad <= 14;
        } else if (categoria.equals("Sub-17")) {
            return edad >= 15 && edad <= 16;
        } else if (categoria.equals("Mayores")) {
            return edad >= 17;
        } else {
            return false;
        }
    }

    public boolean eliminarInscripcion(InscripcionEquipo inscripcion) {
        return (inscripciones.contains(inscripcion) && inscripcion.retirar("Retiro de equipo"));
    }
}
