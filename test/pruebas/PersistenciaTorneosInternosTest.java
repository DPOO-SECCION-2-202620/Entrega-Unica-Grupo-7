package pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aplicacion.EstadoClub;
import aplicacion.ResultadoOperacion;
import consola.ConsolaTorneosInternos;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import modelo.deportes.NivelAcademia;
import modelo.torneos.ModalidadTorneo;
import modelo.torneos.TipoCupo;
import modelo.torneos.TipoTorneo;
import modelo.torneos.TorneoInterno;
import persistencia.PersistenciaClubJson;

public class PersistenciaTorneosInternosTest {

    private EstadoClub club;
    private Sesion admin;
    private PersistenciaClubJson persistencia;
    private TorneoInterno torneo;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Bogota"));
        club = new EstadoClub(reloj);
        admin = SoporteComercial.administrador(club);
        persistencia = new PersistenciaClubJson(reloj);

        for (int i = 0; i < 3; i++) {
            String id = "J" + i;
            club.registrarJugador(id, "Alex", LocalDate.of(2000, 1, 1));
            SoporteComercial.habilitar(club, admin, id, Perfil.SOCIO);
            ResultadoOperacion inscripcion = club.getAcademias().inscribir(admin, id, "TENIS", NivelAcademia.PRINCIPIANTE);
            assertTrue(inscripcion.exito(), inscripcion.mensaje());
            ResultadoOperacion ranking = club.getOperaciones().actualizarRanking(admin, id, "TENIS",
                    1000 + i * 100, LocalDate.of(2026, 11, 7), "CLASIFICACION");
            assertTrue(ranking.exito(), ranking.mensaje());
        }

        ResultadoOperacion creacion = club.getTorneos().crear(admin, "T", "Copa tenis", "TENIS", null,
                NivelAcademia.PRINCIPIANTE, ModalidadTorneo.SENCILLOS, 5,
                LocalDateTime.of(2026, 11, 10, 10, 0), null, TipoTorneo.RECREATIVO, null);
        assertTrue(creacion.exito(), creacion.mensaje());
        assertTrue(club.getTorneos().clasificarJugadores(admin, "T").exito());
        torneo = club.getTorneos().buscar("T");
    }

    @Test
    void testGuardarYCargarTorneo() throws IOException {
        assertTrue(club.getTorneos().inscribir(admin, "T", List.of("J2"), null, TipoCupo.PREFERENCIAL).exito());
        assertFalse(club.getTorneos().liberarPreferenciales(admin, "T").exito(), "No debe liberar un cupo ocupado");
        assertTrue(club.getTorneos().inscribir(admin, "T", List.of("J0"), null, TipoCupo.REGULAR).exito());
        File archivo = File.createTempFile("torneo-prueba-", ".json");

        try {
            persistencia.salvar(archivo.toPath(), club);
            EstadoClub cargado = persistencia.cargar(archivo.toPath());
            TorneoInterno recuperado = cargado.getTorneos().buscar("T");

            assertNotNull(recuperado, "El torneo debe recuperarse");
            assertEquals(torneo.getNombre(), recuperado.getNombre(), "El nombre debe conservarse");
            assertSame(cargado.buscarDeporte("TENIS"), recuperado.getDeporte(), "Debe usarse el deporte del estado cargado");
            assertEquals(torneo.getDisponibilidad(), recuperado.getDisponibilidad(), "Los cupos deben conservarse");
            assertEquals(3, recuperado.getClasificacion().size(), "Debe conservarse la clasificación");
            assertEquals(2, recuperado.getInscripciones().size(), "Debe conservarse cada inscripción una sola vez");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    @Test
    void testConservarLiberacionYRetiroTrasRecargar() throws IOException {
        assertTrue(club.getTorneos().liberarPreferenciales(admin, "T").exito());
        assertTrue(club.getTorneos().inscribir(admin, "T", List.of("J0"), null, TipoCupo.REGULAR).exito());
        File archivo = File.createTempFile("torneo-liberado-", ".json");

        try {
            persistencia.salvar(archivo.toPath(), club);
            EstadoClub cargado = persistencia.cargar(archivo.toPath());
            Sesion administrador = SoporteComercial.administrador(cargado);
            TorneoInterno recuperado = cargado.getTorneos().buscar("T");
            ResultadoOperacion repetida = cargado.getTorneos().liberarPreferenciales(administrador, "T");
            ResultadoOperacion retiro = cargado.getTorneos().retirar(administrador, "T", "J0");

            assertTrue(recuperado.estanPreferencialesLiberados(), "Debe conservarse la liberación");
            assertFalse(repetida.exito(), "Recargar no debe permitir liberar nuevamente");
            assertTrue(retiro.exito(), retiro.mensaje());
            assertFalse(recuperado.getInscripciones().get(0).activa(), "El retiro debe conservar la inscripción histórica");
            assertEquals(5, recuperado.getDisponibilidad().regularesDisponibles(), "Debe liberarse un solo cupo");

            persistencia.salvar(archivo.toPath(), cargado);
            EstadoClub segundaCarga = persistencia.cargar(archivo.toPath());
            TorneoInterno segundo = segundaCarga.getTorneos().buscar("T");

            assertEquals(5, segundo.getDisponibilidad().regularesDisponibles(), "Una segunda carga no debe alterar los cupos");
            assertFalse(segundo.getInscripciones().get(0).activa(), "Debe conservarse el retiro después de reiniciar");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    @Test
    void testRecuperarPreferencialOcupadoDespuesDeLiberar() throws IOException {
        assertTrue(club.getTorneos().crear(admin, "M", "Mixto", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, LocalDateTime.of(2026, 11, 10, 10, 0),
                null, TipoTorneo.RECREATIVO, null).exito());
        assertTrue(club.getTorneos().clasificarJugadores(admin, "M").exito());
        assertTrue(club.getTorneos().inscribir(admin, "M", List.of("J2"), null, TipoCupo.PREFERENCIAL).exito());
        assertTrue(club.getTorneos().liberarPreferenciales(admin, "M").exito());
        File archivo = File.createTempFile("torneo-mixto-", ".json");

        try {
            persistencia.salvar(archivo.toPath(), club);
            EstadoClub cargado = persistencia.cargar(archivo.toPath());
            TorneoInterno recuperado = cargado.getTorneos().buscar("M");
            Sesion administrador = SoporteComercial.administrador(cargado);

            assertTrue(recuperado.estanPreferencialesLiberados(), "Debe conservarse la liberación");
            assertEquals(1, recuperado.getDisponibilidad().preferencialesOcupados(), "El preferencial inscrito debe mantenerse");
            assertEquals(6, recuperado.getDisponibilidad().regularesDisponibles(), "Solo se convierte la plaza libre");
            assertTrue(cargado.getTorneos().retirar(administrador, "M", "J2").exito());
            assertEquals(7, recuperado.getDisponibilidad().regularesDisponibles(), "La plaza retirada debe quedar regular");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    @Test
    void testCargarVersionAnterior() throws IOException {
        File archivo = File.createTempFile("torneo-version-seis-", ".json");

        try {
            persistencia.salvar(archivo.toPath(), club);
            JSONObject datos = new JSONObject(Files.readString(archivo.toPath()));
            datos.put("version", 6);
            datos.remove("torneosInternos");
            Files.writeString(archivo.toPath(), datos.toString());
            EstadoClub cargado = persistencia.cargar(archivo.toPath());

            assertTrue(cargado.getTorneos().consultarParaPersistencia().isEmpty(), "Un archivo antiguo no debe inventar torneos");
            assertEquals(3, cargado.getJugadores().size(), "Los jugadores anteriores deben conservarse");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    @Test
    void testRechazarArchivoInvalidoSinSobrescribir() throws IOException {
        File archivo = File.createTempFile("torneo-invalido-", ".json");

        try {
            persistencia.salvar(archivo.toPath(), club);
            JSONObject datos = new JSONObject(Files.readString(archivo.toPath()));
            datos.getJSONArray("torneosInternos").getJSONObject(0).put("capacidad", 0);
            Files.writeString(archivo.toPath(), datos.toString());
            String antes = Files.readString(archivo.toPath());

            assertThrows(IOException.class, () -> {
                persistencia.cargar(archivo.toPath());
            }, "Debe rechazar un torneo con capacidad inválida");

            assertEquals(antes, Files.readString(archivo.toPath()), "El archivo rechazado no debe sobrescribirse");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    @Test
    void testConsolaCrearTorneo() {
        String datos = "1\nCLI\nCopa consola\nTENIS\nPRINCIPIANTE\nSENCILLOS\n7\n2026-11-10T10:00\n0\nRECREATIVO\n";
        Scanner entrada = new Scanner(datos);
        ConsolaTorneosInternos consola = new ConsolaTorneosInternos(club, admin, entrada);

        boolean cambio = consola.ejecutar();
        TorneoInterno creado = club.getTorneos().buscar("CLI");

        assertTrue(cambio, "La consola debe indicar que realizó un cambio");
        assertNotNull(creado, "El torneo debe quedar registrado");
        assertEquals(7, creado.getCapacidad(), "Debe guardar los cupos ingresados");
        assertEquals(2, creado.getDisponibilidad().preferencialesDisponibles(), "Debe reservar el 20 % hacia arriba");
    }

    @Test
    void testConsolaAsignarInscribirYLiberar() throws IOException {
        Scanner clasificacion = new Scanner("2\nT\n");
        Scanner inscripcion = new Scanner("5\nT\nJ0\nREGULAR\n");
        Scanner liberacion = new Scanner("3\nT\n");

        assertTrue(new ConsolaTorneosInternos(club, admin, clasificacion).ejecutar(), "Debe asignar la clasificación");
        assertTrue(new ConsolaTorneosInternos(club, admin, inscripcion).ejecutar(), "Debe registrar la inscripción");
        assertTrue(new ConsolaTorneosInternos(club, admin, liberacion).ejecutar(), "Debe liberar el preferencial disponible");
        assertTrue(torneo.estanPreferencialesLiberados(), "Debe registrar la liberación");
        assertEquals(4, torneo.getDisponibilidad().regularesDisponibles(), "Debe haber una plaza ocupada y cuatro libres");

        File archivo = File.createTempFile("torneo-consola-", ".json");
        try {
            persistencia.salvar(archivo.toPath(), club);
            EstadoClub cargado = persistencia.cargar(archivo.toPath());
            TorneoInterno recuperado = cargado.getTorneos().buscar("T");

            assertTrue(recuperado.estanPreferencialesLiberados(), "Los cambios de consola deben persistir");
            assertEquals("J0", recuperado.getInscripciones().get(0).participante().id(), "Debe conservarse el inscrito");
        } finally {
            Files.deleteIfExists(archivo.toPath());
        }
    }

    @Test
    void testConsolaRechazarCuposDecimales() {
        Scanner entrada = new Scanner("1\nINVALIDO\nCopa inválida\nTENIS\nPRINCIPIANTE\nSENCILLOS\n7.5\n");
        ConsolaTorneosInternos consola = new ConsolaTorneosInternos(club, admin, entrada);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> {
            consola.ejecutar();
        }, "Los cupos deben ser enteros");

        assertTrue(error.getMessage().contains("entero positivo"), "Debe explicar por qué rechaza los cupos");
        assertNull(club.getTorneos().buscar("INVALIDO"), "El torneo inválido no debe quedar registrado");
    }
}
