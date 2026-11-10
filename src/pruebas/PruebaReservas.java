package pruebas;

import gestores.GestorReservas;
import java.time.LocalDate;
import java.time.LocalDateTime;
import modelo.deportes.Deporte;
import modelo.reservas.Instalacion;
import modelo.usuarios.Jugador;
import modelo.usuarios.Usuario;

public class PruebaReservas {

    public static void main(String[] args) {
        Deporte tenis = new Deporte("D01", "Tenis", false);
        Deporte padel = new Deporte("D02", "Pádel", false);

        Instalacion canchaTenis = new Instalacion(
            "I01",
            "Cancha de tenis 1",
            tenis,
            4
        );

        Instalacion canchaPadel = new Instalacion(
            "I02",
            "Cancha de pádel 1",
            padel,
            4
        );

        Usuario usuario = new Usuario("U01", "Juan", "juan", "clave1");

        Jugador juan = new Jugador(LocalDate.of(2005, 5, 12), usuario);

        GestorReservas gestor = new GestorReservas();

        gestor.registrarInstalacion(canchaTenis);
        gestor.registrarInstalacion(canchaPadel);

        LocalDateTime diez = LocalDateTime.of(2026, 11, 3, 10, 0);
        LocalDateTime diezYMedia = LocalDateTime.of(2026, 11, 3, 10, 30);
        LocalDateTime once = LocalDateTime.of(2026, 11, 3, 11, 0);
        LocalDateTime onceYMedia = LocalDateTime.of(2026, 11, 3, 11, 30);
        LocalDateTime doce = LocalDateTime.of(2026, 11, 3, 12, 0);

        System.out.println("=== PRUEBAS DE RESERVAS ===");

        System.out.println(
            "1. Reserva válida: " +
                gestor.realizarReserva(
                    "R01",
                    juan,
                    canchaTenis,
                    diez,
                    once,
                    2,
                    "Sencillos"
                )
        );

        System.out.println(
            "2. Horario solapado: " +
                gestor.realizarReserva(
                    "R02",
                    juan,
                    canchaTenis,
                    diezYMedia,
                    onceYMedia,
                    2,
                    "Sencillos"
                )
        );

        System.out.println(
            "3. Horario consecutivo: " +
                gestor.realizarReserva(
                    "R03",
                    juan,
                    canchaTenis,
                    once,
                    doce,
                    2,
                    "Sencillos"
                )
        );

        System.out.println(
            "4. Pádel sencillos: " +
                gestor.realizarReserva(
                    "R04",
                    juan,
                    canchaPadel,
                    diez,
                    once,
                    2,
                    "Sencillos"
                )
        );

        System.out.println(
            "5. Pádel dobles: " +
                gestor.realizarReserva(
                    "R05",
                    juan,
                    canchaPadel,
                    diez,
                    once,
                    4,
                    "Dobles"
                )
        );

        System.out.println(
            "6. Tenis con 3 participantes: " +
                gestor.realizarReserva(
                    "R06",
                    juan,
                    canchaTenis,
                    doce,
                    doce.plusHours(1),
                    3,
                    "Dobles"
                )
        );

        System.out.println(
            "7. Modalidad desconocida: " +
                gestor.realizarReserva(
                    "R07",
                    juan,
                    canchaTenis,
                    doce,
                    doce.plusHours(1),
                    2,
                    "Triple"
                )
        );

        System.out.println(
            "8. Identificador repetido: " +
                gestor.realizarReserva(
                    "R01",
                    juan,
                    canchaPadel,
                    once,
                    doce,
                    4,
                    "Dobles"
                )
        );

        System.out.println(
            "9. Buscar reserva existente: " +
                (gestor.buscarReserva("R01") != null)
        );

        System.out.println(
            "10. Buscar reserva rechazada: " +
                (gestor.buscarReserva("R02") == null)
        );
    }
}
