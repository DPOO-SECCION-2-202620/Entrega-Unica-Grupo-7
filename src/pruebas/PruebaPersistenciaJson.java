package pruebas;

import aplicacion.EstadoClub;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import modelo.reservas.Instalacion;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaPersistenciaJson {

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: crear|verificar|invalidos archivo.json");
        }
        Path ruta = Path.of(args[1]);
        PersistenciaClubJson p = new PersistenciaClubJson();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 10, 0);
        if (args[0].equals("crear")) {
            EstadoClub c = new EstadoClub();
            c.registrarJugador("J01", "María Séneca", LocalDate.of(2000, 5, 10));
            SoporteComercial.academiaHistorica(c, "J01", "TENIS");
            Instalacion i = new Instalacion("I01", "Cancha de tenis", c.buscarDeporte("TENIS"), 4);
            comprobar(c.getReservas().registrarInstalacion(i), "Registrar instalación");
            comprobar(c.getReservas().realizarReserva("R01", c.buscarJugador("J01"), i, inicio, inicio.plusHours(1), 2,
                    "Sencillos"), "Registrar reserva");
            p.salvar(ruta, c);
            comprobar(Files.size(ruta) > 0, "Archivo JSON escrito");
        } else if (args[0].equals("verificar")) {
            EstadoClub c = p.cargar(ruta);
            comprobar(c.getJugadores().size() == 1, "Jugador recuperado en otro proceso");
            comprobar(c.buscarJugador("J01").getUsuario().getNombre().equals("María Séneca"),
                    "Nombre y acentos conservados");
            comprobar(c.getReservas().getInstalaciones().size() == 1, "Instalación recuperada");
            comprobar(c.getReservas().getReservas().size() == 1, "Reserva recuperada");
            var r = c.getReservas().buscarReserva("R01");
            comprobar(r.getJugador() == c.buscarJugador("J01"), "Referencia única del jugador");
            Instalacion i = c.getReservas().buscarInstalacion("I01");
            comprobar(r.getInstalacion() == i, "Referencia única de la instalación");
            comprobar(
                    !c.getReservas().realizarReserva("R02", c.buscarJugador("J01"), i, inicio.plusMinutes(30),
                            inicio.plusMinutes(90), 2, "Sencillos"),
                    "Horario ocupado sigue bloqueado después de reiniciar");
            comprobar(!c.getReservas().realizarReserva("R01", c.buscarJugador("J01"), i, inicio.plusHours(1),
                    inicio.plusHours(2), 2, "Sencillos"), "ID repetido rechazado");
            comprobar(c.getReservas().realizarReserva("R03", c.buscarJugador("J01"), i, inicio.plusHours(1),
                    inicio.plusHours(2), 2, "Sencillos"), "Horario consecutivo aceptado");
            p.salvar(ruta, c);
            comprobar(p.cargar(ruta).getReservas().getReservas().size() == 2, "Segundo guardado recuperable");
        } else if (args[0].equals("invalidos")) {
            Path carpeta = Files.createTempDirectory(ruta.toAbsolutePath().getParent(), "invalidos-");
            Path archivo = carpeta.resolve("prueba.json");
            comprobar(p.cargar(archivo).getJugadores().isEmpty(), "Primer arranque sin archivo");
            Files.writeString(archivo, "{json roto", StandardCharsets.UTF_8);
            rechazar(p, archivo, "JSON mal formado rechazado sin sobrescritura");
            String valido = Files.readString(ruta, StandardCharsets.UTF_8);
            JSONObject raiz = new JSONObject(valido);
            raiz.getJSONArray("reservas").getJSONObject(0).put("jugadorId", "NO_EXISTE");
            Files.writeString(archivo, raiz.toString());
            rechazar(p, archivo, "Referencia inexistente rechazada");
            raiz = new JSONObject(valido);
            JSONArray reservas = raiz.getJSONArray("reservas");
            reservas.put(new JSONObject(reservas.getJSONObject(0).toString()).put("id", "CRUCE"));
            Files.writeString(archivo, raiz.toString());
            rechazar(p, archivo, "Cruce en archivo rechazado");
            raiz = new JSONObject(valido);
            raiz.getJSONArray("jugadores").put(raiz.getJSONArray("jugadores").getJSONObject(0));
            Files.writeString(archivo, raiz.toString());
            rechazar(p, archivo, "Jugador duplicado rechazado");
        } else {
            throw new IllegalArgumentException("Modo desconocido");
        }
    }

    private static void rechazar(PersistenciaClubJson p, Path archivo, String mensaje) throws IOException {
        String antes = Files.readString(archivo);
        boolean fallo = false;
        try {
            p.cargar(archivo);
        } catch (IOException e) {
            fallo = true;
        }
        comprobar(fallo && antes.equals(Files.readString(archivo)), mensaje);
    }
}
