package pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import modelo.deportes.Deporte;
import modelo.deportes.NivelAcademia;
import modelo.torneos.ModalidadTorneo;
import modelo.torneos.TipoTorneo;
import modelo.torneos.TorneoInterno;

public class TorneoInternoTest {

    private Deporte tenis;
    private LocalDateTime inicio;
    private LocalDateTime liberacion;
    private TorneoInterno torneo;

    @BeforeEach
    void setUp() {
        tenis = new Deporte("TENIS", "Tenis", false);
        inicio = LocalDateTime.of(2026, 11, 10, 10, 0);
        liberacion = LocalDateTime.of(2026, 11, 8, 10, 0);
        torneo = new TorneoInterno("T", "Copa tenis", tenis, null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
    }

    @Test
    void testEstadoInicial() {
        assertEquals("T", torneo.getId(), "El ID del torneo no coincide");
        assertEquals("Copa tenis", torneo.getNombre(), "El nombre no coincide");
        assertSame(tenis, torneo.getDeporte(), "El torneo debe conservar el mismo deporte");
        assertEquals(NivelAcademia.PRINCIPIANTE, torneo.getNivel(), "El nivel no coincide");
        assertEquals(ModalidadTorneo.SENCILLOS, torneo.getModalidad(), "La modalidad no coincide");
        assertEquals(TipoTorneo.RECREATIVO, torneo.getTipo(), "El tipo no coincide");
        assertEquals(inicio, torneo.getInicio(), "La fecha de inicio no coincide");
        assertEquals(liberacion, torneo.getFechaLiberacion(), "La fecha de liberación no coincide");
        assertTrue(torneo.getInscripciones().isEmpty(), "El torneo debe empezar sin inscripciones");
        assertEquals(2, torneo.getDisponibilidad().preferencialesDisponibles(), "Deben reservarse dos cupos");
        assertEquals(5, torneo.getDisponibilidad().regularesDisponibles(), "Deben quedar cinco regulares");
    }

    @Test
    void testTorneoRecreativoSinCosto() {
        TorneoInterno recreativo = new TorneoInterno("R", "Recreativo", tenis, null,
                NivelAcademia.INTERMEDIO, ModalidadTorneo.DOBLES, 7, inicio, liberacion,
                TipoTorneo.RECREATIVO, new BigDecimal("100"));

        assertEquals(new BigDecimal("0.00"), recreativo.getTarifa(), "Un recreativo debe ser gratuito");
    }

    @Test
    void testTarifaCompetitiva() {
        Deporte futbol = new Deporte("FUTBOL", "Fútbol", true);
        TorneoInterno competitivo = new TorneoInterno("C", "Competitivo", futbol, "Sub-15", null,
                ModalidadTorneo.EQUIPOS, 10, inicio, liberacion,
                TipoTorneo.COMPETITIVO, new BigDecimal("2500.50"));

        assertEquals(new BigDecimal("2500.50"), competitivo.getTarifa(), "La tarifa debe conservarse");
        assertEquals("Sub-15", competitivo.getCategoria(), "La categoría debe conservarse");
    }

    @Test
    void testDatosObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno(" ", "Copa", tenis, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "No debe aceptarse un ID vacío");

        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("T", null, tenis, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "El nombre es obligatorio");

        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("T", "Copa", null, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "El deporte es obligatorio");
    }

    @Test
    void testCuposInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("T", "Copa", tenis, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, 0, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "No deben aceptarse cero cupos");

        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("T", "Copa", tenis, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, -1, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "No deben aceptarse cupos negativos");
    }

    @Test
    void testTarifasCompetitivasInvalidas() {
        BigDecimal[] tarifas = {null, BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("2.005")};

        for (BigDecimal tarifa : tarifas) {
            assertThrows(IllegalArgumentException.class, () -> {
                new TorneoInterno("T", "Copa", tenis, null, NivelAcademia.PRINCIPIANTE,
                        ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.COMPETITIVO, tarifa);
            }, "La tarifa debe ser positiva y tener como máximo dos decimales");
        }
    }

    @Test
    void testCondicionesDeportivasIncompatibles() {
        Deporte padel = new Deporte("PADEL", "Pádel", false);
        Deporte futbol = new Deporte("FUTBOL", "Fútbol", true);

        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("P", "Pádel", padel, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "Pádel debe jugarse en dobles");

        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("F", "Fútbol", futbol, "Otra", null,
                    ModalidadTorneo.EQUIPOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "La categoría del equipo debe ser válida");

        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("T", "Tenis", tenis, null, null,
                    ModalidadTorneo.SENCILLOS, 7, inicio, liberacion, TipoTorneo.RECREATIVO, null);
        }, "Una disciplina individual requiere nivel");
    }

    @Test
    void testFechaDeLiberacionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TorneoInterno("T", "Copa", tenis, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, 7, inicio, inicio, TipoTorneo.RECREATIVO, null);
        }, "La liberación debe ser anterior al inicio");
    }

    @Test
    void testRedondeoDeCuposPreferenciales() {
        int[] capacidades = {1, 2, 4, 5, 6, 7, 10, 11, Integer.MAX_VALUE};

        for (int capacidad : capacidades) {
            TorneoInterno nuevo = new TorneoInterno("T", "Copa", tenis, null, NivelAcademia.PRINCIPIANTE,
                    ModalidadTorneo.SENCILLOS, capacidad, inicio, liberacion, TipoTorneo.RECREATIVO, null);
            TorneoInterno.Disponibilidad cupos = nuevo.getDisponibilidad();

            assertEquals((int) Math.ceil(capacidad / 5.0), cupos.preferenciales(), "Debe redondearse hacia arriba");
            assertEquals(capacidad, cupos.preferenciales() + cupos.regulares(), "No debe cambiar la capacidad total");
        }
    }

    @Test
    void testLiberarAntesDeLaFechaPermitida() {
        LocalDateTime antes = LocalDateTime.of(2026, 11, 7, 10, 0);

        assertThrows(IllegalArgumentException.class, () -> {
            torneo.liberarPreferenciales(antes);
        }, "No debe liberar cupos antes de la fecha establecida");

        assertEquals(2, torneo.getDisponibilidad().preferencialesDisponibles(), "Los cupos no deben cambiar");
        assertFalse(torneo.estanPreferencialesLiberados(), "La liberación no debe quedar registrada");
    }

    @Test
    void testLiberarEnLaFechaPermitida() {
        int liberados = torneo.liberarPreferenciales(liberacion);

        assertEquals(2, liberados, "Deben liberarse los dos preferenciales disponibles");
        assertTrue(torneo.estanPreferencialesLiberados(), "Debe registrarse la liberación");
        assertEquals(0, torneo.getDisponibilidad().preferencialesDisponibles(), "No deben quedar preferenciales libres");
        assertEquals(7, torneo.getDisponibilidad().regularesDisponibles(), "Los cupos liberados deben ser regulares");
    }
}
