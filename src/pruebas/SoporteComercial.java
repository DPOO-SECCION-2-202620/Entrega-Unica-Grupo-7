package pruebas;

import aplicacion.EstadoClub;
import gestores.GestorAcceso;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import java.util.Set;

final class SoporteComercial {

    static Sesion administrador(EstadoClub club) {
        if (!club.getAcceso().tieneAdministrador()
                && !club.crearAdministradorInicial("ADMIN-TEST", "Administrador de pruebas", "ClavePrueba2026")) {
            throw new AssertionError("No se pudo crear administrador de prueba");
        }
        Sesion sesion = club.getAcceso().autenticar("ADMIN-TEST", "ClavePrueba2026");
        if (sesion == null) {
            throw new AssertionError("No se pudo autenticar administrador de prueba");
        }
        return sesion;
    }

    static void academiaHistorica(EstadoClub c, String id, String deporte) {
        var j = c.buscarJugador(id);
        var d = c.buscarDeporte(deporte);
        if (!j.getRankingsPorDisciplina().containsKey(d.getId())) {
            j.actualizarRanking(d.getId(), 1000, java.time.LocalDate.of(2026, 10, 1), "INSCRIPCION:" + d.getId());
        }
        c.getAcademias().cargarInscripcion(
                new modelo.deportes.InscripcionDisciplina(j, d, modelo.deportes.NivelAcademia.PRINCIPIANTE));
    }

    static Sesion habilitar(EstadoClub club, Sesion admin, String id, Perfil perfil) {
        if (!club.getAcceso().tienePerfil(id, perfil)
                && !club.habilitarCuenta(admin, id, "ClavePrueba2026", Set.of(perfil))) {
            throw new AssertionError("No se pudo habilitar perfil " + perfil);
        }
        Sesion sesion = club.getAcceso().autenticar(id, "ClavePrueba2026");
        if (sesion == null) {
            throw new AssertionError("No se pudo autenticar " + id);
        }
        return sesion;
    }
}
