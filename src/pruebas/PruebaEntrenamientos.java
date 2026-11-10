package pruebas;

import gestores.GestorEntrenamientos;
import gestores.GestorReservas;
import java.time.LocalDate;
import java.time.LocalDateTime;
import modelo.deportes.Deporte;
import modelo.entrenamientos.AsistenciaEvaluacion;
import modelo.entrenamientos.Entrenamiento;
import modelo.entrenamientos.EstadoAsistencia;
import modelo.reservas.Instalacion;
import modelo.usuarios.Entrenador;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;

public class PruebaEntrenamientos {

    public static void main(String[] args) {
        System.out.println("=== PRUEBAS DE ENTRENAMIENTOS ===");

        Deporte tenis = new Deporte("D01", "Tenis", false);

        Instalacion cancha = new Instalacion(
            "I01",
            "Cancha de tenis 1",
            tenis,
            4
        );

        Usuario usuario = new Usuario("U01", "Carlos Pérez", "carlos", "1234");

        Entrenador entrenador = new Entrenador(usuario);

        GestorEntrenamientos gestor = new GestorEntrenamientos();

        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 0);

        Entrenamiento entrenamiento = new Entrenamiento(
            "E01",
            inicio,
            60,
            entrenador,
            cancha
        );

        System.out.println(
            "Prueba 1 (esperado true): " +
                gestor.registrarEntrenamiento(entrenamiento)
        );

        System.out.println(
            "Prueba 2 (esperado false): " +
                gestor.registrarEntrenamiento(entrenamiento)
        );

        System.out.println(
            "Prueba 3 (esperado true): " +
                (gestor.buscarEntrenamiento("E01") == entrenamiento)
        );

        Usuario usuarioJugador = new Usuario(
            "U02",
            "Laura Gómez",
            "laura",
            "1234"
        );

        Jugador jugador = new Jugador(
            LocalDate.of(2005, 5, 15),
            usuarioJugador
        );

        GestorReservas gestorReservas = new GestorReservas();
        gestorReservas.registrarInstalacion(cancha);

        System.out.println(
            "Prueba 4 (esperado false): " +
                gestorReservas.realizarReserva(
                    "R01",
                    jugador,
                    cancha,
                    LocalDateTime.of(2026, 10, 20, 10, 30),
                    LocalDateTime.of(2026, 10, 20, 11, 30),
                    2,
                    "sencillos"
                )
        );

        System.out.println(
            "Prueba 5 (esperado true): " +
                gestorReservas.realizarReserva(
                    "R02",
                    jugador,
                    cancha,
                    LocalDateTime.of(2026, 10, 20, 11, 0),
                    LocalDateTime.of(2026, 10, 20, 12, 0),
                    2,
                    "sencillos"
                )
        );

        Entrenamiento entrenamiento2 = new Entrenamiento(
            "E02",
            LocalDateTime.of(2026, 10, 20, 11, 30),
            60,
            entrenador,
            cancha
        );

        System.out.println(
            "Prueba 6 (esperado false): " +
                gestor.registrarEntrenamiento(entrenamiento2)
        );

        Entrenamiento entrenamiento3 = new Entrenamiento(
            "E03",
            LocalDateTime.of(2026, 10, 20, 12, 0),
            60,
            entrenador,
            cancha
        );

        System.out.println(
            "Prueba 7 (esperado true): " +
                gestor.registrarEntrenamiento(entrenamiento3)
        );

        System.out.println(
            "Prueba 8 (esperado true): " +
                entrenamiento.convocarJugador(jugador)
        );

        System.out.println(
            "Prueba 9 (esperado false): " +
                entrenamiento.convocarJugador(jugador)
        );

        AsistenciaEvaluacion asistencia = new AsistenciaEvaluacion(
            jugador,
            entrenamiento,
            EstadoAsistencia.ASISTIO,
            4.5,
            "Buen desempeño"
        );

        System.out.println(
            "Prueba 10 (esperado true): " +
                entrenamiento.registrarAsistencia(asistencia)
        );

        System.out.println(
            "Prueba 11 (esperado false): " +
                entrenamiento.registrarAsistencia(asistencia)
        );

        Usuario usuarioOtro = new Usuario(
            "U03",
            "Andrés López",
            "andres",
            "1234"
        );

        Jugador otroJugador = new Jugador(
            LocalDate.of(2004, 8, 10),
            usuarioOtro
        );

        AsistenciaEvaluacion asistenciaInvalida = new AsistenciaEvaluacion(
            otroJugador,
            entrenamiento,
            EstadoAsistencia.FALTO,
            null,
            null
        );

        System.out.println(
            "Prueba 12 (esperado false): " +
                entrenamiento.registrarAsistencia(asistenciaInvalida)
        );
    }
}
