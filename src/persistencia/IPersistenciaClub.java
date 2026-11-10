package persistencia;

import aplicacion.EstadoClub;
import java.io.IOException;
import java.nio.file.Path;

public interface IPersistenciaClub {
    EstadoClub cargar(Path archivo) throws IOException;
    void salvar(Path archivo, EstadoClub club) throws IOException;
}
