package persistencia;

import aplicacion.EstadoClub;
import gestores.GestorAcademias;
import java.time.*;
import java.util.*;
import modelo.deportes.*;
import modelo.entrenamientos.Entrenamiento;
import modelo.partidos.*;
import modelo.reservas.*;
import org.json.*;

final class PersistenciaAcademiasAgenda {

    static JSONArray cambios(ActividadAgenda a) {
        JSONArray j = new JSONArray();
        for (CambioAgenda c : a.getCambiosAgenda()) {
            Object instalacionAnteriorIdJson;
            if (c.instalacionAnteriorId() == null) {
                instalacionAnteriorIdJson = JSONObject.NULL;
            } else {
                instalacionAnteriorIdJson = c.instalacionAnteriorId();
            }
            Object instalacionNuevaIdJson;
            if (c.instalacionNuevaId() == null) {
                instalacionNuevaIdJson = JSONObject.NULL;
            } else {
                instalacionNuevaIdJson = c.instalacionNuevaId();
            }
            j.put(new JSONObject().put("instalacionAnteriorId", instalacionAnteriorIdJson)
                    .put("inicioAnterior", c.inicioAnterior().toString()).put("finAnterior", c.finAnterior().toString())
                    .put("instalacionNuevaId", instalacionNuevaIdJson).put("inicioNuevo", c.inicioNuevo().toString())
                    .put("finNuevo", c.finNuevo().toString()).put("motivo", c.motivo())
                    .put("cancelacion", c.cancelacion()));
        }
        return j;
    }

    static void cargarCambios(ActividadAgenda a, JSONArray array, EstadoClub club) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject j = array.getJSONObject(i);
            String anterior;
            if (j.isNull("instalacionAnteriorId")) {
                anterior = null;
            } else {
                anterior = j.getString("instalacionAnteriorId");
            }
            String nuevo;
            if (j.isNull("instalacionNuevaId")) {
                nuevo = null;
            } else {
                nuevo = j.getString("instalacionNuevaId");
            }

            if ((anterior != null && club.getReservas().buscarInstalacion(anterior) == null)
                    || (nuevo != null && club.getReservas().buscarInstalacion(nuevo) == null)) {
                throw new IllegalArgumentException("Cambio de agenda con instalación inexistente.");
            }
            CambioAgenda cambio = new CambioAgenda(anterior, LocalDateTime.parse(j.getString("inicioAnterior")),
                    LocalDateTime.parse(j.getString("finAnterior")), nuevo,
                    LocalDateTime.parse(j.getString("inicioNuevo")), LocalDateTime.parse(j.getString("finNuevo")),
                    j.getString("motivo"), j.getBoolean("cancelacion"));
            if (a instanceof Reserva r) {
                r.cargarCambio(cambio);
            } else if (a instanceof Entrenamiento e) {
                e.cargarCambio(cambio);
            } else if (a instanceof Partido p) {
                p.cargarCambio(cambio);
            }
        }
    }

    static void validarCambios(EstadoClub club) {
        for (ActividadAgenda a : club.getAgenda().getActividades()) {
            CambioAgenda previo = null;
            for (CambioAgenda cambio : a.getCambiosAgenda()) {
                if (previo != null && (previo.cancelacion()
                        || !Objects.equals(previo.instalacionNuevaId(), cambio.instalacionAnteriorId())
                        || !previo.inicioNuevo().equals(cambio.inicioAnterior())
                        || !previo.finNuevo().equals(cambio.finAnterior()))) {
                    throw new IllegalArgumentException("Historia de agenda discontinua.");
                }

                if (cambio.cancelacion()
                        && (!Objects.equals(cambio.instalacionNuevaId(), cambio.instalacionAnteriorId())
                                || !cambio.inicioNuevo().equals(cambio.inicioAnterior())
                                || !cambio.finNuevo().equals(cambio.finAnterior()))) {
                    throw new IllegalArgumentException("Una cancelación no cambia el intervalo.");
                }
                previo = cambio;
            }
            String instalacionActualId = null;
            if (a.getInstalacion() != null) {
                instalacionActualId = a.getInstalacion().getId();
            }

            if (previo != null && (previo.cancelacion() != a.estaCancelada()
                    || !Objects.equals(previo.instalacionNuevaId(), instalacionActualId)
                    || !previo.inicioNuevo().equals(a.getInicio()) || !previo.finNuevo().equals(a.getFin()))) {
                throw new IllegalArgumentException("Horario actual difiere de su historial de cambios.");
            }
        }
    }

    static void salvar(JSONObject raiz, EstadoClub c) {
        JSONArray inscripciones = new JSONArray(), maximos = new JSONArray();
        for (InscripcionDisciplina i : c.getAcademias().getInscripciones()) {
            inscripciones.put(new JSONObject().put("jugadorId", i.getJugador().getId())
                    .put("disciplinaId", i.getDisciplina().getId()).put("nivel", i.getNivel().name())
                    .put("activa", i.estaActiva()));
        }
        var config = c.getAcademias().getConfiguracion();
        raiz.put("academias", inscripciones).put("configuracionAcademias",
                new JSONObject().put("diferenciaMaxima", config.diferenciaMaxima())
                        .put("excepcionEntrenador", config.excepcionEntrenador()).put("k", config.k())
                        .put("rankingInicial", config.rankingInicial()));
        c.getAgenda().getMaximos()
                .forEach((d, m) -> maximos.put(new JSONObject().put("disciplinaId", d).put("maximo", m)));
        raiz.put("maximosSimultaneos", maximos);
        validarCambios(c);
        c.getAcademias().validarHistorial();
        c.getAgenda().validarMaximos();
        PersistenciaTorneos.validarVinculos(c);
    }

    static void cargarInscripciones(JSONObject raiz, EstadoClub c) {
        JSONObject j = raiz.getJSONObject("configuracionAcademias");
        c.getAcademias().cargarConfiguracion(new GestorAcademias.Configuracion(j.getInt("diferenciaMaxima"),
                j.getBoolean("excepcionEntrenador"), j.getInt("k"), j.getDouble("rankingInicial")));
        JSONArray array = raiz.getJSONArray("academias");
        for (int i = 0; i < array.length(); i++) {
            JSONObject in = array.getJSONObject(i);
            InscripcionDisciplina inscripcion = new InscripcionDisciplina(c.buscarJugador(in.getString("jugadorId")),
                    c.buscarDeporte(in.getString("disciplinaId")), NivelAcademia.valueOf(in.getString("nivel")));
            inscripcion.restaurarActiva(in.getBoolean("activa"));
            c.getAcademias().cargarInscripcion(inscripcion);
        }
    }

    static void cargarMaximosYValidar(JSONObject raiz, EstadoClub c) {
        JSONArray array = raiz.getJSONArray("maximosSimultaneos");
        for (int i = 0; i < array.length(); i++) {
            JSONObject j = array.getJSONObject(i);
            c.getAgenda().cargarMaximo(j.getString("disciplinaId"), j.getInt("maximo"));
        }
        validarCambios(c);
        c.getAgenda().validarMaximos();
        c.getAcademias().validarHistorial();
        PersistenciaTorneos.validarVinculos(c);
    }
}
