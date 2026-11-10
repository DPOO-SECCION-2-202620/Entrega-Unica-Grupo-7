package modelo.competencias;

import modelo.deportes.Deporte;
import modelo.partidos.PartidoCompetencia;

public interface FormatoCompetencia {
    boolean procesarResultado(PartidoCompetencia partido);
    void configurarDeporte(Deporte deporte);
}
