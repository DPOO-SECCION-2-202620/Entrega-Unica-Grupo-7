package modelo.deportes;

import modelo.usuarios.Jugador;

public class InscripcionDisciplina {

    private final Jugador jugador;
    private final Deporte disciplina;
    private NivelAcademia nivel;
    private boolean activa = true;

    public InscripcionDisciplina(Jugador jugador, Deporte disciplina, NivelAcademia nivel) {
        if (jugador == null || disciplina == null || disciplina.isColectivo() || nivel == null) {
            throw new IllegalArgumentException("Inscripción de academia inválida.");
        }
        this.jugador = jugador;
        this.disciplina = disciplina;
        this.nivel = nivel;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Deporte getDisciplina() {
        return disciplina;
    }

    public NivelAcademia getNivel() {
        return nivel;
    }

    public boolean estaActiva() {
        return activa;
    }

    public void restaurarNivel(NivelAcademia nivel) {
        this.nivel = nivel;
    }

    public void restaurarActiva(boolean activa) {
        this.activa = activa;
    }

    public double getRanking() {
        return jugador.obtenerRankingPorDisciplina(disciplina.getId());
    }
}
