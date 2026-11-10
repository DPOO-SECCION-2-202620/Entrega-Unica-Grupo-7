package modelo.reservas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface ActividadAgenda {
    String getId();
    LocalDateTime getInicio();
    LocalDateTime getFin();
    Instalacion getInstalacion();
    boolean estaCancelada();
    Set<String> getUsuariosAgenda();
    List<CambioAgenda> getCambiosAgenda();
}
