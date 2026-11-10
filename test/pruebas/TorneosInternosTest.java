package pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aplicacion.EstadoClub;
import aplicacion.ResultadoOperacion;
import consola.MenuPorRol;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import gestores.GestorTorneos;
import modelo.deportes.Equipo;
import modelo.deportes.NivelAcademia;
import modelo.torneos.CandidatoTorneo;
import modelo.torneos.ModalidadTorneo;
import modelo.torneos.TipoCupo;
import modelo.torneos.TipoTorneo;
import modelo.torneos.TorneoInterno;

public class TorneosInternosTest {

    private EstadoClub club;
    private GestorTorneos gestor;
    private Sesion admin;
    private LocalDateTime inicio;
    private TorneoInterno torneo;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Bogota"));
        club = new EstadoClub(reloj);
        admin = SoporteComercial.administrador(club);
        gestor = club.getTorneos();
        inicio = LocalDateTime.of(2026, 11, 10, 10, 0);

        for (int i = 0; i < 8; i++) {
            String id = "J" + i;
            club.registrarJugador(id, "Alex", LocalDate.of(2000, 1, 1));
            SoporteComercial.habilitar(club, admin, id, Perfil.SOCIO);
            ResultadoOperacion inscripcion = club.getAcademias().inscribir(admin, id, "TENIS", NivelAcademia.PRINCIPIANTE);
            assertTrue(inscripcion.exito(), inscripcion.mensaje());
            ResultadoOperacion ranking = club.getOperaciones().actualizarRanking(admin, id, "TENIS",
                    1000 + 100 * i, LocalDate.of(2026, 11, 7), "CLASIFICACION");
            assertTrue(ranking.exito(), ranking.mensaje());
        }

        ResultadoOperacion creacion = gestor.crear(admin, "T", "Copa tenis", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, inicio, null, TipoTorneo.RECREATIVO, null);
        assertTrue(creacion.exito(), creacion.mensaje());
        ResultadoOperacion clasificacion = gestor.clasificarJugadores(admin, "T");
        assertTrue(clasificacion.exito(), clasificacion.mensaje());
        torneo = gestor.buscar("T");
    }

    @Test
    void testCrearCompetitivoSinCobrar() {
        ResultadoOperacion resultado = gestor.crear(admin, "C", "Copa fútbol", "FUTBOL", "Sub-15", null,
                ModalidadTorneo.EQUIPOS, 10, inicio, null, TipoTorneo.COMPETITIVO, new BigDecimal("2500.50"));
        TorneoInterno competitivo = gestor.buscar("C");

        assertTrue(resultado.exito(), resultado.mensaje());
        assertNotNull(competitivo, "El torneo debe quedar registrado");
        assertEquals(new BigDecimal("2500.50"), competitivo.getTarifa(), "La tarifa no coincide");
        assertTrue(club.getVentas().getVentasRegistradas().isEmpty(), "Crear un torneo no debe registrar pagos");
    }

    @Test
    void testRechazarDatosDeCreacionInvalidos() {
        ResultadoOperacion sinCupos = gestor.crear(admin, "N", "Nuevo", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 0, inicio, null, TipoTorneo.RECREATIVO, null);
        ResultadoOperacion sinDeporte = gestor.crear(admin, "N", "Nuevo", "OTRO", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, inicio, null, TipoTorneo.RECREATIVO, null);
        ResultadoOperacion sinTarifa = gestor.crear(admin, "N", "Nuevo", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, inicio, null, TipoTorneo.COMPETITIVO, null);

        assertFalse(sinCupos.exito(), "No debe aceptar cero cupos");
        assertTrue(sinCupos.mensaje().contains("cupos"), "Debe explicar el problema con los cupos");
        assertFalse(sinDeporte.exito(), "No debe aceptar un deporte inexistente");
        assertFalse(sinTarifa.exito(), "El competitivo necesita una tarifa válida");
        assertNull(gestor.buscar("N"), "No debe registrarse un torneo inválido");
    }

    @Test
    void testRechazarInicioEnElPresente() {
        LocalDateTime ahora = LocalDateTime.now(club.getReloj());
        ResultadoOperacion resultado = gestor.crear(admin, "N", "Nuevo", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, ahora, null, TipoTorneo.RECREATIVO, null);

        assertFalse(resultado.exito(), "El inicio debe ser futuro");
        assertTrue(resultado.mensaje().contains("inicio"), "Debe explicar el problema con la fecha");
        assertNull(gestor.buscar("N"), "No debe registrarse el torneo");
    }

    @Test
    void testTorneosIndependientesYDuplicados() {
        ResultadoOperacion nuevo = gestor.crear(admin, "T2", "Otra copa", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 10, inicio, null, TipoTorneo.RECREATIVO, null);
        ResultadoOperacion duplicado = gestor.crear(admin, " T ", "Reemplazo", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 20, inicio, null, TipoTorneo.RECREATIVO, null);
        ResultadoOperacion liberacion = gestor.liberarPreferenciales(admin, "T2");

        assertTrue(nuevo.exito(), nuevo.mensaje());
        assertFalse(duplicado.exito(), "No debe reemplazar un torneo existente");
        assertSame(torneo, gestor.buscar("T"), "El primer torneo debe conservarse");
        assertTrue(liberacion.exito(), liberacion.mensaje());
        assertEquals(2, torneo.getDisponibilidad().preferencialesDisponibles(), "El otro torneo no debe cambiar");
        assertEquals(0, gestor.buscar("T2").getDisponibilidad().preferencialesDisponibles(), "T2 debe liberar sus cupos");
    }

    @Test
    void testPermisosDeAdministrador() {
        Sesion socio = club.getAcceso().autenticar("J0", "ClavePrueba2026");
        ResultadoOperacion creacion = gestor.crear(socio, "N", "Nuevo", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.SENCILLOS, 7, inicio, null, TipoTorneo.RECREATIVO, null);
        ResultadoOperacion clasificacion = gestor.clasificarJugadores(socio, "T");
        ResultadoOperacion liberacion = gestor.liberarPreferenciales(socio, "T");
        ResultadoOperacion inscripcion = gestor.inscribir(socio, "T", List.of("J0"), null, TipoCupo.REGULAR);
        ResultadoOperacion retiro = gestor.retirar(socio, "T", "J0");

        assertFalse(creacion.exito(), "El socio no puede crear torneos");
        assertFalse(clasificacion.exito(), "El socio no puede asignar la clasificación");
        assertFalse(liberacion.exito(), "El socio no puede liberar cupos");
        assertFalse(inscripcion.exito(), "Esta inscripción administrativa requiere administrador");
        assertFalse(retiro.exito(), "Este retiro administrativo requiere administrador");
        assertTrue(liberacion.mensaje().contains("administrador"), "Debe indicar la falta de permiso");
        assertFalse(MenuPorRol.opciones(club, socio).containsKey("69"), "El socio no debe ver la opción 69");
        assertFalse(MenuPorRol.opciones(club, null).containsKey("69"), "Sin sesión no debe verse la opción");
        assertTrue(MenuPorRol.opciones(club, admin).containsKey("69"), "El administrador debe ver la opción");
    }

    @Test
    void testSesionDeOtroClub() {
        EstadoClub otroClub = new EstadoClub(club.getReloj());
        Sesion otraSesion = SoporteComercial.administrador(otroClub);
        ResultadoOperacion resultado = gestor.liberarPreferenciales(otraSesion, "T");

        assertFalse(resultado.exito(), "Una sesión de otro club no debe autorizar la operación");
        assertEquals(2, torneo.getDisponibilidad().preferencialesDisponibles(), "Los cupos no deben cambiar");
    }

    @Test
    void testClasificacionPorRankingYNivel() {
        ResultadoOperacion cambio = club.getAcademias().actualizar(admin, "J0", "TENIS", NivelAcademia.AVANZADO, true);
        ResultadoOperacion resultado = gestor.clasificarJugadores(admin, "T");
        List<CandidatoTorneo> elegibles = torneo.getElegiblesPreferenciales();

        assertTrue(cambio.exito(), cambio.mensaje());
        assertTrue(resultado.exito(), resultado.mensaje());
        assertEquals(7, torneo.getClasificacion().size(), "Debe excluir al jugador de otro nivel");
        assertEquals("J7", elegibles.get(0).id(), "El mejor ranking debe ir primero aunque los nombres sean iguales");
        assertEquals("J6", elegibles.get(1).id(), "Debe conservarse la identidad del segundo jugador");
        assertEquals(1700.0, elegibles.get(0).puntaje(), "El puntaje debe venir del ranking real");
    }

    @Test
    void testInscripcionPreferencial() {
        ResultadoOperacion resultado = gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL);
        TorneoInterno.Disponibilidad cupos = torneo.getDisponibilidad();

        assertTrue(resultado.exito(), resultado.mensaje());
        assertEquals(1, cupos.preferencialesOcupados(), "Debe ocupar un preferencial");
        assertEquals(1, cupos.preferencialesDisponibles(), "Debe quedar un preferencial libre");
        assertEquals(5, cupos.regularesDisponibles(), "Los regulares no deben cambiar");
    }

    @Test
    void testNoElegiblePuedeSolicitarRegular() {
        TorneoInterno.Disponibilidad antes = torneo.getDisponibilidad();
        ResultadoOperacion preferencial = gestor.inscribir(admin, "T", List.of("J0"), null, TipoCupo.PREFERENCIAL);

        assertFalse(preferencial.exito(), "Un jugador no elegible no debe ocupar un preferencial");
        assertTrue(preferencial.mensaje().contains("regular"), "Debe informar la alternativa regular");
        assertEquals(antes, torneo.getDisponibilidad(), "El rechazo no debe consumir cupos");

        ResultadoOperacion regular = gestor.inscribir(admin, "T", List.of("J0"), null, TipoCupo.REGULAR);

        assertTrue(regular.exito(), regular.mensaje());
        assertEquals(4, torneo.getDisponibilidad().regularesDisponibles(), "Debe consumir un regular");
    }

    @Test
    void testCuposRegularesAgotados() {
        for (int i = 0; i < 5; i++) {
            ResultadoOperacion resultado = gestor.inscribir(admin, "T", List.of("J" + i), null, TipoCupo.REGULAR);
            assertTrue(resultado.exito(), resultado.mensaje());
        }
        ResultadoOperacion sinCupo = gestor.inscribir(admin, "T", List.of("J5"), null, TipoCupo.REGULAR);

        assertFalse(sinCupo.exito(), "No debe ocupar un preferencial como regular sin liberarlo");
        assertEquals(2, torneo.getDisponibilidad().preferencialesDisponibles(), "Deben conservarse las reservas");

        assertTrue(gestor.liberarPreferenciales(admin, "T").exito(), "Debe permitir liberar las reservas");
        assertTrue(gestor.inscribir(admin, "T", List.of("J5"), null, TipoCupo.REGULAR).exito());
        assertTrue(gestor.inscribir(admin, "T", List.of("J6"), null, TipoCupo.REGULAR).exito());
        assertFalse(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.REGULAR).exito());
        assertEquals(7, torneo.getDisponibilidad().regularesOcupados(), "No debe superar la capacidad total");
    }

    @Test
    void testRetiroYReinscripcion() {
        assertTrue(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL).exito());
        assertFalse(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.REGULAR).exito(), "No debe duplicar un inscrito");

        ResultadoOperacion retiro = gestor.retirar(admin, "T", "J7");
        ResultadoOperacion repetido = gestor.retirar(admin, "T", "J7");

        assertTrue(retiro.exito(), retiro.mensaje());
        assertFalse(repetido.exito(), "No debe retirar dos veces la misma inscripción");
        assertEquals(2, torneo.getDisponibilidad().preferencialesDisponibles(), "Debe liberar una sola plaza");
        assertFalse(torneo.getInscripciones().get(0).activa(), "El registro retirado debe conservarse");

        assertTrue(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL).exito());
        assertEquals(2, torneo.getInscripciones().size(), "La reinscripción no debe borrar la historia");
    }

    @Test
    void testClasificacionNoCambiaConInscritos() {
        assertTrue(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL).exito());
        List<CandidatoTorneo> antes = torneo.getClasificacion();
        ResultadoOperacion resultado = gestor.clasificarJugadores(admin, "T");

        assertFalse(resultado.exito(), "No debe cambiar el criterio después de inscribir");
        assertEquals(antes, torneo.getClasificacion(), "La clasificación anterior debe conservarse");
    }

    @Test
    void testInscripcionDeParejas() {
        assertTrue(gestor.crear(admin, "D", "Dobles", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.DOBLES, 5, inicio, null, TipoTorneo.RECREATIVO, null).exito());
        ResultadoOperacion clasificacion = gestor.clasificarParejas(admin, "D",
                List.of(List.of("J0", "J1"), List.of("J6", "J7")));
        TorneoInterno dobles = gestor.buscar("D");
        CandidatoTorneo mejor = dobles.getElegiblesPreferenciales().get(0);

        assertTrue(clasificacion.exito(), clasificacion.mensaje());
        assertEquals(1650.0, mejor.puntaje(), "Debe utilizar el promedio ELO de la pareja");
        assertTrue(gestor.inscribir(admin, "D", List.of("J7", "J6"), null, TipoCupo.PREFERENCIAL).exito());
        assertFalse(gestor.inscribir(admin, "D", List.of("J6", "J1"), null, TipoCupo.REGULAR).exito(), "No debe repetir un integrante");
        assertFalse(gestor.liberarPreferenciales(admin, "D").exito(), "No debe liberar una plaza ocupada");
        assertTrue(gestor.retirar(admin, "D", mejor.id()).exito(), "Debe retirar la pareja completa");
        assertEquals(1, dobles.getDisponibilidad().preferencialesDisponibles(), "Debe liberar un cupo por pareja");
    }

    @Test
    void testClasificacionDeParejasInvalida() {
        assertTrue(gestor.crear(admin, "D", "Dobles", "TENIS", null, NivelAcademia.PRINCIPIANTE,
                ModalidadTorneo.DOBLES, 5, inicio, null, TipoTorneo.RECREATIVO, null).exito());
        assertTrue(gestor.clasificarParejas(admin, "D", List.of(List.of("J0", "J1"))).exito());
        TorneoInterno dobles = gestor.buscar("D");
        List<CandidatoTorneo> antes = dobles.getClasificacion();
        ResultadoOperacion repetido = gestor.clasificarParejas(admin, "D",
                List.of(List.of("J0", "J1"), List.of("J0", "J2")));
        ResultadoOperacion inexistente = gestor.clasificarParejas(admin, "D", List.of(List.of("J0", "NO-EXISTE")));

        assertFalse(repetido.exito(), "Un jugador no debe aparecer en dos parejas");
        assertFalse(inexistente.exito(), "No debe aceptar un jugador inexistente");
        assertEquals(antes, dobles.getClasificacion(), "Los rechazos no deben reemplazar la clasificación");
    }

    private void crearEquipo(String id) {
        Equipo equipo = new Equipo(id, "Mismo nombre", club.buscarDeporte("FUTBOL"), "Mayores", 7);
        assertTrue(club.getOperaciones().registrarEquipo(admin, equipo).exito());

        for (int i = 0; i < 7; i++) {
            String jugador = id + "J" + i;
            club.registrarJugador(jugador, jugador, LocalDate.of(2000, 1, 1));
            assertTrue(club.getOperaciones().inscribir(admin, jugador, id, "Jugador", i + 1).exito());
        }
    }

    @Test
    void testClasificacionDeEquipos() {
        crearEquipo("E0");
        crearEquipo("E1");
        Equipo incompleto = new Equipo("INCOMPLETO", "En formación", club.buscarDeporte("FUTBOL"), "Mayores", 7);
        assertTrue(club.getOperaciones().registrarEquipo(admin, incompleto).exito());
        assertTrue(gestor.crear(admin, "F", "Fútbol", "FUTBOL", "Mayores", null,
                ModalidadTorneo.EQUIPOS, 5, inicio, null, TipoTorneo.RECREATIVO, null).exito());

        ResultadoOperacion parcial = gestor.clasificarEquipos(admin, "F", "Puntos liga anterior", Map.of("E0", 3.0));
        ResultadoOperacion completa = gestor.clasificarEquipos(admin, "F", "Puntos liga anterior", Map.of("E0", 3.0, "E1", 9.0));
        TorneoInterno futbol = gestor.buscar("F");

        assertFalse(parcial.exito(), "Deben incluirse todos los equipos habilitados");
        assertTrue(completa.exito(), completa.mensaje());
        assertEquals("E1", futbol.getElegiblesPreferenciales().get(0).id(), "Debe elegirse el mejor desempeño");
        assertFalse(gestor.inscribir(admin, "F", null, "E0", TipoCupo.PREFERENCIAL).exito());
        assertTrue(gestor.inscribir(admin, "F", null, "E1", TipoCupo.PREFERENCIAL).exito());
        assertFalse(gestor.inscribir(admin, "F", null, "INCOMPLETO", TipoCupo.REGULAR).exito());
        assertEquals(7, futbol.getInscripciones().get(0).participante().jugadores().size(), "Debe inscribir el equipo completo");
    }

    @Test
    void testLiberarSinRetirarPreferencialesOcupados() {
        assertTrue(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL).exito());
        ResultadoOperacion resultado = gestor.liberarPreferenciales(admin, "T");
        TorneoInterno.Disponibilidad cupos = torneo.getDisponibilidad();

        assertTrue(resultado.exito(), resultado.mensaje());
        assertEquals(1, cupos.preferencialesOcupados(), "Debe conservar el preferencial inscrito");
        assertEquals(0, cupos.preferencialesDisponibles(), "Debe convertir el cupo libre");
        assertEquals(6, cupos.regularesDisponibles(), "Debe aumentar los regulares en una plaza");
        assertTrue(torneo.getInscripciones().get(0).activa(), "La inscripción original debe seguir activa");
        assertEquals(7, cupos.preferenciales() + cupos.regulares(), "La capacidad debe conservarse");
    }

    @Test
    void testLiberacionRepetida() {
        assertTrue(gestor.liberarPreferenciales(admin, "T").exito());
        TorneoInterno.Disponibilidad antes = torneo.getDisponibilidad();
        ResultadoOperacion resultado = gestor.liberarPreferenciales(admin, "T");

        assertFalse(resultado.exito(), "La segunda liberación debe rechazarse");
        assertEquals(antes, torneo.getDisponibilidad(), "No debe generar cupos adicionales");
    }

    @Test
    void testRetiroPreferencialDespuesDeLiberar() {
        assertTrue(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL).exito());
        assertTrue(gestor.liberarPreferenciales(admin, "T").exito());
        assertTrue(gestor.retirar(admin, "T", "J7").exito());

        assertEquals(0, torneo.getDisponibilidad().preferencialesDisponibles(), "No debe volver a reservar la plaza");
        assertEquals(7, torneo.getDisponibilidad().regularesDisponibles(), "La plaza retirada debe quedar regular");
        assertFalse(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.PREFERENCIAL).exito());
        assertTrue(gestor.inscribir(admin, "T", List.of("J7"), null, TipoCupo.REGULAR).exito());
    }

    @Test
    void testRechazarCambiosDesdeElInicio() {
        Clock relojInicio = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Bogota"));
        EstadoClub clubIniciado = new EstadoClub(relojInicio);
        Sesion administrador = SoporteComercial.administrador(clubIniciado);
        TorneoInterno iniciado = new TorneoInterno("I", "Iniciado", clubIniciado.buscarDeporte("TENIS"), null,
                NivelAcademia.PRINCIPIANTE, ModalidadTorneo.SENCILLOS, 7, inicio,
                LocalDateTime.of(2026, 11, 7, 10, 0), TipoTorneo.RECREATIVO, null);
        GestorTorneos gestorIniciado = clubIniciado.getTorneos();
        gestorIniciado.cargar(iniciado);
        TorneoInterno.Disponibilidad antes = iniciado.getDisponibilidad();

        ResultadoOperacion liberar = gestorIniciado.liberarPreferenciales(administrador, "I");
        ResultadoOperacion inscribir = gestorIniciado.inscribir(administrador, "I", List.of("J0"), null, TipoCupo.REGULAR);
        ResultadoOperacion retirar = gestorIniciado.retirar(administrador, "I", "J0");
        ResultadoOperacion clasificar = gestorIniciado.clasificarJugadores(administrador, "I");

        assertFalse(liberar.exito(), "No debe liberar desde el inicio");
        assertFalse(inscribir.exito(), "No debe inscribir desde el inicio");
        assertFalse(retirar.exito(), "No debe retirar desde el inicio");
        assertFalse(clasificar.exito(), "No debe cambiar la clasificación desde el inicio");
        assertTrue(liberar.mensaje().contains("terminó"), "Debe informar que terminó la inscripción");
        assertEquals(antes, iniciado.getDisponibilidad(), "Los rechazos no deben alterar el estado");
    }
}
