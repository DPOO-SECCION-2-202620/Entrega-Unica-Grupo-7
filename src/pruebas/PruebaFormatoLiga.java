package pruebas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import modelo.competencias.Competencia;
import modelo.competencias.FormatoLiga;
import modelo.competencias.NominaCompetencia;
import modelo.deportes.Deporte;
import modelo.deportes.Equipo;
import modelo.deportes.InscripcionEquipo;
import modelo.partidos.PartidoCompetencia;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;

public class PruebaFormatoLiga {

    private static int exitosas = 0;
    private static int ejecutadas = 0;

    private static void verificar(String nombre, boolean condicion) {
        ejecutadas++;

        if (condicion) {
            exitosas++;
            System.out.println("OK - " + nombre);
        } else {
            System.out.println("ERROR - " + nombre);
        }
    }

    public static void main(String[] args) {
        Deporte futbol = new Deporte("DEP1", "Fútbol", true);

        Equipo equipo = new Equipo("EQ1", "Seneca FC", futbol, "Mayores", 20);

        FormatoLiga liga = new FormatoLiga();

        Competencia competencia = new Competencia(
            "COMP1",
            "Torneo de los Cerros",
            "Organizador",
            2026,
            futbol,
            "Mayores",
            liga
        );

        NominaCompetencia nomina = new NominaCompetencia(competencia, equipo);

        for (int i = 1; i <= 7; i++) {
            Usuario usuario = new Usuario(
                "U" + i,
                "Jugador " + i,
                "jugador" + i,
                "clave" + i
            );

            Jugador jugador = new Jugador(LocalDate.of(2000, 1, 1), usuario);

            InscripcionEquipo inscripcion = new InscripcionEquipo(
                jugador,
                equipo,
                "Jugador de campo",
                i
            );

            equipo.agregarInscripcion(inscripcion);
            nomina.agregarJugador(jugador);
        }

        verificar("Nomina con minimo de jugadores", nomina.cumpleMinimo());

        LocalDateTime fecha = LocalDateTime.of(2026, 10, 19, 10, 0);

        PartidoCompetencia victoria = new PartidoCompetencia(
            "P1",
            fecha,
            futbol,
            competencia,
            equipo,
            "Universidad A",
            "Cancha A"
        );

        PartidoCompetencia empate = new PartidoCompetencia(
            "P2",
            fecha.plusDays(1),
            futbol,
            competencia,
            equipo,
            "Universidad B",
            "Cancha B"
        );

        PartidoCompetencia derrota = new PartidoCompetencia(
            "P3",
            fecha.plusDays(2),
            futbol,
            competencia,
            equipo,
            "Universidad C",
            "Cancha C"
        );

        PartidoCompetencia sinResultado = new PartidoCompetencia(
            "P4",
            fecha.plusDays(3),
            futbol,
            competencia,
            equipo,
            "Universidad D",
            "Cancha D"
        );

        verificar(
            "Registrar victoria 2-1",
            victoria.registrarResultado(2, 1, nomina)
        );

        verificar(
            "Registrar empate 1-1",
            empate.registrarResultado(1, 1, nomina)
        );

        verificar(
            "Registrar derrota 0-2",
            derrota.registrarResultado(0, 2, nomina)
        );

        verificar(
            "Victoria suma 3 puntos",
            liga.procesarResultado(victoria) &&
                liga.obtenerPuntos(equipo.getId()) == 3
        );

        verificar(
            "Empate acumula 1 punto",
            liga.procesarResultado(empate) &&
                liga.obtenerPuntos(equipo.getId()) == 4
        );

        verificar(
            "Derrota acumula 0 puntos",
            liga.procesarResultado(derrota) &&
                liga.obtenerPuntos(equipo.getId()) == 4
        );

        verificar(
            "No procesar dos veces el mismo partido",
            !liga.procesarResultado(victoria) &&
                liga.obtenerPuntos(equipo.getId()) == 4
        );

        verificar(
            "Rechazar partido sin resultado",
            !liga.procesarResultado(sinResultado) &&
                liga.obtenerPuntos(equipo.getId()) == 4
        );

        verificar("Rechazar partido nulo", !liga.procesarResultado(null));

        System.out.println();
        System.out.println(
            "RESULTADO: " + exitosas + "/" + ejecutadas + " pruebas exitosas."
        );
    }
}
