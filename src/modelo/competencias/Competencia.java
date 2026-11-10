package modelo.competencias;

import modelo.deportes.Deporte;

public class Competencia {

    private int setsParaGanar = 3;
    private String id;
    private String nombre;
    private String organizador;
    private int temporada;
    private Deporte deporte;
    private String categoria;
    private FormatoCompetencia formato;

    public Competencia(String id, String nombre, String organizador, int temporada, Deporte deporte, String categoria,
            FormatoCompetencia formato) {
        super();
        this.id = id;
        this.nombre = nombre;
        this.organizador = organizador;
        this.temporada = temporada;
        this.deporte = deporte;
        this.categoria = categoria;
        this.formato = formato;
        if (formato != null && deporte != null) {
            formato.configurarDeporte(deporte);
        }
    }

    public void restaurarSetsParaGanar(int sets) {
        if (sets != 2 && sets != 3) {
            throw new IllegalArgumentException("Sets inválidos.");
        }
        setsParaGanar = sets;
    }

    public int getSetsParaGanar() {
        return setsParaGanar;
    }

    public boolean configurarSetsParaGanar(int sets) {
        if (sets != 2 && sets != 3) {
            return false;
        }

        if (formato instanceof FormatoLiga liga && !liga.configurarSets(sets)) {
            return false;
        }
        setsParaGanar = sets;
        return true;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getOrganizador() {
        return organizador;
    }

    public int getTemporada() {
        return temporada;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public String getCategoria() {
        return categoria;
    }

    public FormatoCompetencia getFormato() {
        return formato;
    }
}
