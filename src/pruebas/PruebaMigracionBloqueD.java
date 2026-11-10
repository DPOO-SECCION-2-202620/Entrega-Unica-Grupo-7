package pruebas;

import aplicacion.EstadoClub;
import gestores.GestorAcceso.Perfil;
import java.nio.file.*;
import java.util.*;
import modelo.ventas.Venta;
import org.json.*;
import persistencia.PersistenciaClubJson;

public class PruebaMigracionBloqueD {

    private static void ok(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
        System.out.println("OK: " + mensaje);
    }

    public static void main(String[] args) throws Exception {
        PersistenciaClubJson p = new PersistenciaClubJson();
        Path carpeta = Files.createTempDirectory(Path.of(".build"), "migracion-d-");
        for (String nombre : List.of("club.json", "ejemplo-ventas.json", "ejemplo.json")) {
            Path origen = Path.of("datos", nombre);
            String antes = Files.readString(origen);
            JSONObject viejo = new JSONObject(antes);
            EstadoClub c = p.cargar(origen);
            int cantidad;
            if (viejo.has("ventas")) {
                cantidad = viejo.getJSONArray("ventas").length();
            } else {
                cantidad = 0;
            }
            ok(c.getVentas().getVentasRegistradas().size() == cantidad, "ventas originales conservadas: " + nombre);
            for (Venta venta : c.getVentas().getVentasRegistradas()) {
                ok(!venta.getComprador().esIdentificado()
                        && venta.getComprador().getNombre().contains("no identificado")
                        && c.getVentas().consultarPuntos(venta.getComprador().getId()) == 0,
                        "ausencia de comprador explícita y sin puntos retroactivos");
            }
            var admin = SoporteComercial.administrador(c);
            for (Venta venta : c.getVentas().getVentasRegistradas()) {
                ok(!c.habilitarCuenta(admin, venta.getComprador().getId(), "ClavePrueba2026", Set.of(Perfil.SOCIO)),
                        "marcador histórico no se autentica ni se convierte en socio");
            }
            Path destino = carpeta.resolve(nombre);
            p.salvar(destino, c);
            EstadoClub recargado = p.cargar(destino);
            ok(recargado.getVentas().getVentasRegistradas().size() == cantidad
                    && antes.equals(Files.readString(origen)),
                    "migración recuperable sin modificar original: " + nombre);
            JSONArray ventas = viejo.optJSONArray("ventas");
            if (ventas != null) {
                for (int i = 0; i < ventas.length(); i++) {
                    JSONObject v = ventas.getJSONObject(i);
                    ok(Math.abs(recargado.getVentas().buscarVenta(v.getString("id")).getValorFinalPagado()
                            - v.getDouble("total")) < 0.001, "total histórico conservado");
                }
            }
        }
        EstadoClub c = new EstadoClub();
        SoporteComercial.administrador(c);
        Path v2 = carpeta.resolve("version2.json");
        p.salvar(v2, c);
        JSONObject j = new JSONObject(Files.readString(v2));
        j.put("version", 2);
        j.remove("cuentas");
        j.remove("usosInstalaciones");
        j.remove("posesionesCalientes");
        Files.writeString(v2, j.toString());
        EstadoClub migrado = p.cargar(v2);
        ok(!migrado.getAcceso().tieneAdministrador(), "versión 2 no inventa credenciales o perfiles");
        p.salvar(carpeta.resolve("version3.json"), migrado);
        ok(new JSONObject(Files.readString(carpeta.resolve("version3.json"))).getInt("version") == 7,
                "guardado de versión anterior usa esquema 7");
    }
}
