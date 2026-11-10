package modelo.partidos;

import java.time.LocalDateTime;
import java.util.List;
import modelo.competencias.Competencia;
import modelo.competencias.NominaCompetencia;
import modelo.deportes.Deporte;
import modelo.deportes.Equipo;
import modelo.usuarios.Jugador;

public class PartidoCompetencia extends Partido {

    private NominaCompetencia nominaResultado;
    private Equipo equipoRivalClub;
    private String rivalId, encuentroTorneoId, ganadorDesempateId;
    private Competencia competencia;
    private Equipo equipoClub;
    private String institucionRival;
    private String escenario;
    private Integer marcadorClub;
    private Integer marcadorRival;

    public PartidoCompetencia(String id, LocalDateTime fechaHora, Deporte deporte, Competencia competencia,
            Equipo equipoClub, String institucionRival, String escenario) {
        super(id, fechaHora, deporte);
        this.competencia = competencia;
        if (competencia != null) {
            configurarSetsParaGanar(competencia.getSetsParaGanar());
        }
        this.equipoClub = equipoClub;
        this.institucionRival = institucionRival;
        this.rivalId = "EXT:" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(institucionRival).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.encuentroTorneoId = id;
        this.escenario = escenario;
        this.marcadorClub = null;
        this.marcadorRival = null;
    }

    public boolean tieneResultado() {
        return marcadorClub != null;
    }

    public List<Equipo> getEquiposClub() {
        if (equipoClub == null) {
            return List.of();
        } else if (equipoRivalClub == null) {
            return List.of(equipoClub);
        } else {
            return List.of(equipoClub, equipoRivalClub);
        }
    }

    protected boolean participanteLegado(Jugador j) {
        return nominaResultado != null && nominaResultado.contieneJugador(j);
    }

    public String getRivalId() {
        return rivalId;
    }

    public String getEncuentroTorneoId() {
        return encuentroTorneoId;
    }

    public String getGanadorDesempateId() {
        return ganadorDesempateId;
    }

    public Equipo getEquipoRivalClub() {
        return equipoRivalClub;
    }

    public boolean vincularEncuentro(String encuentro, String rivalId, String nombre, Equipo rivalClub) {
        if (tieneResultado() || estaCancelado() || estaCerrado() || encuentro == null || encuentro.isBlank()
                || rivalId == null || rivalId.isBlank() || nombre == null || nombre.isBlank()
                || (rivalClub != null && (rivalClub == equipoClub || rivalClub.getDeporte() != getDeporte()
                        || !rivalClub.getCategoria().equals(equipoClub.getCategoria())))) {
            return false;
        }
        this.encuentroTorneoId = encuentro;
        this.rivalId = rivalId;
        this.institucionRival = nombre;
        this.equipoRivalClub = rivalClub;
        return true;
    }

    public boolean definirDesempate(String ganadorId) {
        if (estaCerrado() || estaCancelado() || ganadorId == null
                || (!ganadorId.equals(equipoClub.getId()) && !ganadorId.equals(rivalId))) {
            return false;
        }
        ganadorDesempateId = ganadorId;
        return true;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public Equipo getEquipoClub() {
        return equipoClub;
    }

    public String getInstitucionRival() {
        return institucionRival;
    }

    public String getEscenario() {
        return escenario;
    }

    public Integer getMarcadorClub() {
        return marcadorClub;
    }

    public Integer getMarcadorRival() {
        return marcadorRival;
    }

    public boolean cumpleRequisitosCompetencia(NominaCompetencia nomina) {
        if (nomina == null || competencia == null || equipoClub == null || nomina.getCompetencia() == null
                || nomina.getEquipo() == null || getDeporte() == null || competencia.getDeporte() == null
                || equipoClub.getDeporte() == null) {
            return false;
        }

        if (!competencia.getId().equals(nomina.getCompetencia().getId())) {
            return false;
        }

        if (!equipoClub.getId().equals(nomina.getEquipo().getId())) {
            return false;
        }

        if (!equipoClub.getDeporte().getId().equals(competencia.getDeporte().getId())) {
            return false;
        }

        if (!getDeporte().getId().equals(competencia.getDeporte().getId())) {
            return false;
        }

        if (!equipoClub.getCategoria().equals(competencia.getCategoria())) {
            return false;
        }

        return nomina.cumpleMinimo() && cumpleValidacion();
    }

    public boolean registrarResultado(int club, int rival, NominaCompetencia nomina) {
        return (equipoClub != null && equipoClub.estaActivo() && cumpleRequisitosCompetencia(nomina)
                && restaurarResultado(club, rival, nomina));
    }

    public boolean restaurarResultado(int club, int rival, NominaCompetencia nomina) {
        return restaurarResultado(club, rival, nomina, false);
    }

    public boolean restaurarResultado(int club, int rival, NominaCompetencia nomina, boolean legado) {
        if (nomina == null || nomina.getCompetencia() != competencia || nomina.getEquipo() != equipoClub) {
            return false;
        }
        boolean marcadorAceptado;
        if (legado) {
            marcadorAceptado = marcadorLegadoValido(club, rival);
        } else {
            marcadorAceptado = marcadorValido(club, rival);
        }

        if (!marcadorAceptado) {
            return false;
        }
        marcadorClub = club;
        marcadorRival = rival;
        nominaResultado = nomina;
        return true;
    }
}
