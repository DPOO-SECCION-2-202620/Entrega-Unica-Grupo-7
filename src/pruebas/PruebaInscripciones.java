package pruebas;

import gestores.GestorEquipos;
import java.time.LocalDate;
import modelo.deportes.Deporte;
import modelo.deportes.Equipo;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;

public class PruebaInscripciones {

    public static void main(String[] args) {
        Deporte futbol = new Deporte("D01", "Fútbol", true);
        Deporte baloncesto = new Deporte("D02", "Baloncesto", true);

        Equipo equipoA = new Equipo("E01", "Titanes", futbol, "Sub-15", 2);

        Equipo equipoB = new Equipo("E02", "Leones", futbol, "Sub-15", 5);

        Equipo equipoC = new Equipo("E03", "Halcones", baloncesto, "Sub-15", 5);

        Usuario usuario1 = new Usuario("U01", "Juan", "juan", "clave1");

        Usuario usuario2 = new Usuario("U02", "Pedro", "pedro", "clave2");

        Usuario usuario3 = new Usuario("U03", "Carlos", "carlos", "clave3");

        Usuario usuario4 = new Usuario("U04", "Luis", "luis", "clave4");

        Jugador juan = new Jugador(LocalDate.of(2012, 1, 1), usuario1);

        Jugador pedro = new Jugador(LocalDate.of(2012, 5, 10), usuario2);

        Jugador carlos = new Jugador(LocalDate.of(2012, 8, 20), usuario3);

        Jugador luis = new Jugador(LocalDate.of(2015, 3, 15), usuario4);

        GestorEquipos gestor = new GestorEquipos();

        gestor.registrarEquipo(equipoA);
        gestor.registrarEquipo(equipoB);
        gestor.registrarEquipo(equipoC);

        System.out.println("=== PRUEBAS DE INSCRIPCIÓN ===");

        boolean prueba1 = gestor.inscribirJugador(
            juan,
            equipoA,
            "Delantero",
            10
        );
        System.out.println("1. Inscripción válida: " + prueba1);

        boolean prueba2 = gestor.inscribirJugador(juan, equipoA, "Defensa", 7);
        System.out.println("2. Jugador duplicado: " + prueba2);

        boolean prueba3 = gestor.inscribirJugador(
            pedro,
            equipoA,
            "Defensa",
            10
        );
        System.out.println("3. Camiseta repetida: " + prueba3);

        boolean prueba4 = gestor.inscribirJugador(pedro, equipoA, "Defensa", 7);
        System.out.println("4. Segunda inscripción válida: " + prueba4);

        boolean prueba5 = gestor.inscribirJugador(
            carlos,
            equipoA,
            "Portero",
            1
        );
        System.out.println("5. Equipo sin cupos: " + prueba5);

        boolean prueba6 = gestor.inscribirJugador(
            juan,
            equipoB,
            "Delantero",
            9
        );
        System.out.println("6. Otro equipo del mismo deporte: " + prueba6);

        boolean prueba7 = gestor.inscribirJugador(juan, equipoC, "Base", 4);
        System.out.println("7. Inscripción en otro deporte: " + prueba7);

        boolean prueba8 = gestor.inscribirJugador(luis, equipoB, "Defensa", 5);
        System.out.println("8. Categoría incompatible: " + prueba8);

        System.out.println(
            "Inscritos en Titanes: " + equipoA.getCantidadInscritos()
        );
    }
}
