package pruebas;

import gestores.GestorSocios;
import java.time.LocalDate;
import java.util.Map;
import modelo.socios.PagoMensualidad;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;

public class PruebaSocios {

    public static void main(String[] args) {
        System.out.println(
            "=================================================="
        );
        System.out.println(
            "   INICIANDO PRUEBAS DEL MÓDULO DE SOCIOS         "
        );
        System.out.println(
            "=================================================="
        );

        GestorSocios gestorSocios = new GestorSocios();

        Jugador jugador1 = new Jugador(
            LocalDate.of(2006, 1, 1),
            new Usuario("JUG-01", "Carlos Pérez", "carlos", "")
        );
        Jugador jugador2 = new Jugador(
            LocalDate.of(2007, 1, 1),
            new Usuario("JUG-02", "Ana Gómez", "ana", "")
        );

        jugador1.actualizarRanking("Tenis", 1250.0);
        jugador2.actualizarRanking("Tenis", 1310.0);

        boolean reg1 = gestorSocios.registrarJugador(jugador1);
        boolean reg2 = gestorSocios.registrarJugador(jugador2);

        if (reg1 && reg2) {
            System.out.println(
                "[OK] Jugadores registrados exitosamente en el sistema."
            );
        }

        LocalDate hoy = LocalDate.now();
        PagoMensualidad pago1 = new PagoMensualidad(
            "PAGO-01",
            jugador1,
            80000.0,
            hoy,
            "2026-03"
        );
        PagoMensualidad pago2 = new PagoMensualidad(
            "PAGO-02",
            jugador2,
            80000.0,
            hoy,
            "2026-03"
        );

        gestorSocios.registrarPagoMensualidad(pago1);
        gestorSocios.registrarPagoMensualidad(pago2);
        System.out.println(
            "[OK] Pagos de mensualidad registrados correctamente."
        );

        System.out.println("\n----------------------------------");
        System.out.println("PRUEBA: Consultas Financieras y Deportivas");
        System.out.println("----------------------------------");

        LocalDate inicio = hoy.minusDays(5);
        LocalDate fin = hoy.plusDays(5);

        double totalMensualidades =
            gestorSocios.consultarTotalMensualidadesPorPeriodo(inicio, fin);
        System.out.println(
            "-> Total recaudado en mensualidades en el periodo: $" +
                totalMensualidades
        );

        Map<String, Double> rankingsTenis =
            gestorSocios.consultarRankingsDisciplina("Tenis");
        System.out.println("-> Rankings de Tenis actuales: " + rankingsTenis);

        System.out.println(
            "\n=================================================="
        );
        System.out.println(
            "   ¡PRUEBAS DE SOCIOS FINALIZADAS CON ÉXITO!      "
        );
        System.out.println(
            "=================================================="
        );
    }
}
