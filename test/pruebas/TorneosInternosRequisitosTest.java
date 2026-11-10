package pruebas;

import static org.junit.Assert.*;

import aplicacion.EstadoClub;
import aplicacion.ResultadoOperacion;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import gestores.GestorTorneos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import modelo.deportes.NivelAcademia;
import modelo.torneos.ModalidadTorneo;
import modelo.torneos.TipoCupo;
import modelo.torneos.TipoTorneo;
import org.junit.Before;
import org.junit.Test;

public class TorneosInternosRequisitosTest {

    private EstadoClub club;
    private GestorTorneos gestor;
    private Sesion admin;
    private LocalDateTime inicio;

    @Before
    public void preparar() {
        club = new EstadoClub();
        admin = SoporteComercial.administrador(club);
        gestor = club.getTorneos();
        inicio = LocalDateTime.now(club.getReloj()).plusDays(10);
    }

    private void exitoso(ResultadoOperacion resultado) {
        assertTrue(resultado.mensaje(), resultado.exito());
    }

    private void crearTorneo(String id, TipoTorneo tipo) {
        java.math.BigDecimal tarifaSeleccionada;
        if (tipo == TipoTorneo.COMPETITIVO) {
            tarifaSeleccionada = new BigDecimal("25.00");
        } else {
            tarifaSeleccionada = null;
        }
        exitoso(gestor.crear(admin, id, id, "TENIS", null, NivelAcademia.PRINCIPIANTE, ModalidadTorneo.SENCILLOS, 10,
                inicio, null, tipo, tarifaSeleccionada));
    }

    private void registrarEmpleadoDeportivo(String id) {
        club.registrarEmpleado(id, "Empleado " + id);
        if (!club.habilitarCuenta(admin, id, id, "clave-segura-123", java.util.Set.of(Perfil.EMPLEADO, Perfil.SOCIO))) {
            throw new AssertionError("No se pudo habilitar el empleado");
        }
        club.registrarJugador(id, "Empleado " + id, LocalDate.of(1995, 1, 1));
        exitoso(club.getAcademias().inscribir(admin, id, "TENIS", NivelAcademia.PRINCIPIANTE));
    }

    @Test
    public void participanteExternoPuedeClasificarseYOcuparCupoPreferencial() {
        crearTorneo("EXT", TipoTorneo.RECREATIVO);
        exitoso(gestor.clasificarParticipantesExternos(admin, "EXT", Map.of("DOC-1", 80.0, "DOC-2", 90.0)));
        exitoso(gestor.inscribir(admin, "EXT", List.of("DOC-2"), null, TipoCupo.PREFERENCIAL));
        assertEquals("EXTERNO:DOC-2", gestor.buscar("EXT").getInscripciones().get(0).participante().id());
    }

    @Test
    public void participanteExternoNoPuedeInscribirseEnCupoPreferencialSinClasificacion() {
        crearTorneo("EXT", TipoTorneo.RECREATIVO);
        ResultadoOperacion resultado = gestor.inscribir(admin, "EXT", List.of("DOC-1"), null, TipoCupo.PREFERENCIAL);
        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().toLowerCase().contains("clasificación"));
    }

    @Test
    public void empleadoSeInscribeGratisEnRecreativoIndividual() {
        crearTorneo("REC", TipoTorneo.RECREATIVO);
        registrarEmpleadoDeportivo("EMP1");
        exitoso(gestor.inscribirEmpleado(admin, "REC", List.of("EMP1"), null, TipoCupo.REGULAR));
        assertEquals("EMP1", gestor.buscar("REC").getInscripciones().get(0).participante().id());
    }

    @Test
    public void competitivoNoAdmiteInscripcionDeEmpleadoNiPremioEnEfectivo() {
        crearTorneo("COMP", TipoTorneo.COMPETITIVO);
        registrarEmpleadoDeportivo("EMP2");
        ResultadoOperacion resultado = gestor.inscribirEmpleado(admin, "COMP", List.of("EMP2"), null, TipoCupo.REGULAR);
        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().toLowerCase().contains("competitivos"));
        assertTrue(gestor.buscar("COMP").getInscripciones().isEmpty());
    }
}
