package modelo.usuarios;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import modelo.deportes.Deporte;
import modelo.deportes.Equipo;
import modelo.deportes.InscripcionDisciplina;
import modelo.deportes.InscripcionEquipo;

public class Jugador {

    public record MovimientoRanking(String disciplina, LocalDate fecha, double anterior, double nuevo,
            String referencia) {
    }

    private String correo = "";
    private final Map<String, Double> rankings = new LinkedHashMap<>();
    private final List<MovimientoRanking> movimientos = new ArrayList<>();
    private LocalDate fechaNacimiento;
    private Usuario usuario;
    private List<InscripcionEquipo> inscripcionesEquipo;
    private List<InscripcionDisciplina> inscripcionesDisciplina;

    public Jugador(LocalDate fechaNacimiento, Usuario usuario) {
        if (fechaNacimiento == null || usuario == null || usuario.getId() == null || usuario.getId().isBlank()) {
            throw new IllegalArgumentException("Identidad o nacimiento del jugador inválidos.");
        }
        this.fechaNacimiento = fechaNacimiento;
        this.usuario = usuario;
        this.inscripcionesEquipo = new ArrayList<>();
        this.inscripcionesDisciplina = new ArrayList<>();
    }

    public String getNombre() {
        return usuario.getNombre();
    }

    public boolean estaActivo() {
        return usuario.estaActivo();
    }

    public String getCorreo() {
        return correo;
    }

    public void actualizarCorreo(String correo) {
        if (correo == null) {
            this.correo = "";
        } else {
            this.correo = correo.trim();
        }
    }

    public int getEdad(LocalDate referencia) {
        if (referencia == null || referencia.isBefore(fechaNacimiento)) {
            throw new IllegalArgumentException("Fecha de referencia inválida.");
        }
        return Period.between(fechaNacimiento, referencia).getYears();
    }

    public void actualizarRanking(String disciplina, double puntaje) {
        actualizarRanking(disciplina, puntaje, LocalDate.now(), "MANUAL-" + (movimientos.size() + 1));
    }

    public boolean puedeRegistrarRanking(String disciplina, double puntaje, LocalDate fecha, String referencia) {
        String clave = Deporte.clave(disciplina);
        if (clave == null || clave.isBlank() || !Double.isFinite(puntaje) || puntaje < 0 || fecha == null
                || referencia == null || referencia.isBlank()
                || movimientos.stream().anyMatch(m -> m.referencia().equals(referencia))
                || movimientos.stream().anyMatch(m -> m.disciplina().equals(clave) && fecha.isBefore(m.fecha()))) {
            return false;
        }
        return true;
    }

    public void actualizarRanking(String disciplina, double puntaje, LocalDate fecha, String referencia) {
        if (!puedeRegistrarRanking(disciplina, puntaje, fecha, referencia)) {
            throw new IllegalArgumentException("Movimiento de ranking inválido o duplicado.");
        }
        String clave = Deporte.clave(disciplina);
        movimientos.add(new MovimientoRanking(clave, fecha, rankings.getOrDefault(clave, 1000.0), puntaje, referencia));
        rankings.put(clave, puntaje);
    }

    public double obtenerRankingPorDisciplina(String disciplina) {
        Double ranking = rankings.get(Deporte.clave(disciplina));
        if (ranking == null) {
            throw new IllegalArgumentException("No existe ranking registrado para esa disciplina.");
        }
        return ranking;
    }

    public Map<String, Double> getRankingsPorDisciplina() {
        return new LinkedHashMap<>(rankings);
    }

    public List<MovimientoRanking> getMovimientosRanking() {
        return List.copyOf(movimientos);
    }

    public List<InscripcionEquipo> getHistorialInscripcionesEquipo() {
        return List.copyOf(inscripcionesEquipo);
    }

    public List<InscripcionEquipo> getInscripcionesEquipo() {
        return inscripcionesEquipo.stream().filter(InscripcionEquipo::estaActiva).toList();
    }

    public List<InscripcionDisciplina> getInscripcionesDisciplina() {
        return List.copyOf(inscripcionesDisciplina);
    }

    public void vincularDisciplina(InscripcionDisciplina i) {
        if (i == null || i.getJugador() != this
                || inscripcionesDisciplina.stream().anyMatch(e -> e.getDisciplina() == i.getDisciplina())) {
            throw new IllegalArgumentException("Inscripción de disciplina duplicada.");
        }
        inscripcionesDisciplina.add(i);
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getId() {
        return usuario.getId();
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public boolean contieneInscripcionEquipo(Equipo equipo) {
        for (InscripcionEquipo inscripcion : inscripcionesEquipo) {
            if (inscripcion.estaActiva() && inscripcion.getEquipo().getId().equals(equipo.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean agregarInscripcionEquipo(InscripcionEquipo inscripcion) {
        if (inscripcion != null && inscripcion.getJugador() == this && !inscripcionesEquipo.contains(inscripcion)
                && inscripcion.getEquipo().getHistorialInscripciones().contains(inscripcion)
                && (!inscripcion.estaActiva() || !contieneInscripcionEquipo(inscripcion.getEquipo()))) {
            inscripcionesEquipo.add(inscripcion);
            return true;
        }
        return false;
    }
}
