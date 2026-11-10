package gestores;

import aplicacion.ResultadoOperacion;
import java.time.*;
import java.util.*;
import modelo.deportes.*;
import modelo.usuarios.Jugador;

public class GestorEquipos {

    private final Map<String, Equipo> equipos = new LinkedHashMap<>();

    public boolean registrarEquipo(Equipo equipo) {
        if (equipo == null || equipo.getId() == null || equipo.getId().isBlank() || equipo.getNombre() == null
                || equipo.getNombre().isBlank() || equipo.getDeporte() == null || !equipo.getDeporte().isColectivo()
                || equipo.getCupoMaximo() <= 0 || equipo.getCategoria() == null
                || !Set.of("Sub-13", "Sub-15", "Sub-17", "Mayores").contains(equipo.getCategoria())
                || equipos.containsKey(equipo.getId())) {
            return false;
        }
        equipos.put(equipo.getId(), equipo);
        return true;
    }

    public List<Equipo> getEquipos() {
        return List.copyOf(equipos.values());
    }

    public Equipo buscarEquipo(String id) {
        return equipos.get(id);
    }

    public boolean estaInscritoEnDeporte(Jugador jugador, Deporte deporte) {
        return (jugador != null && deporte != null && equipos.values().stream()
                .anyMatch(e -> e.estaActivo() && e.getDeporte().es(deporte.getId()) && e.contieneJugador(jugador)));
    }

    public ResultadoOperacion inscribirConMotivo(Jugador j, Equipo e, String posicion, int dorsal,
            LocalDate referencia) {
        if (j == null || e == null || buscarEquipo(e.getId()) != e || posicion == null || posicion.isBlank()
                || dorsal <= 0 || referencia == null) {
            return ResultadoOperacion.rechazo("Datos de inscripción inválidos o equipo inexistente.");
        }

        if (!j.estaActivo() || !e.estaActivo()) {
            return ResultadoOperacion.rechazo("Jugador o equipo dado de baja.");
        }

        if (!e.esCategoriaCompatible(j, referencia)) {
            return ResultadoOperacion.rechazo("Edad incompatible con la categoría " + e.getCategoria() + ".");
        }

        if (estaInscritoEnDeporte(j, e.getDeporte())) {
            return ResultadoOperacion.rechazo("El jugador ya pertenece a un equipo del mismo deporte.");
        }

        if (!e.tieneCupo()) {
            return ResultadoOperacion.rechazo("El equipo no tiene cupos disponibles.");
        }

        if (!e.estaDisponibleNumero(dorsal)) {
            return ResultadoOperacion.rechazo("Dorsal ocupado; elige un número disponible.");
        }

        if (e.vincularInscripcion(new InscripcionEquipo(j, e, posicion, dorsal), referencia)) {
            return ResultadoOperacion.correcto("Inscripción registrada.");
        } else {
            return ResultadoOperacion.rechazo("Inscripción incompatible o duplicada.");
        }
    }

    public boolean inscribirJugador(Jugador j, Equipo e, String posicion, int dorsal) {
        return inscribirJugador(j, e, posicion, dorsal, LocalDate.now(ZoneId.of("America/Bogota")));
    }

    public boolean inscribirJugador(Jugador j, Equipo e, String posicion, int dorsal, LocalDate referencia) {
        return inscribirConMotivo(j, e, posicion, dorsal, referencia).exito();
    }

    public boolean cargarInscripcion(Jugador j, Equipo e, String posicion, int dorsal) {
        return cargarInscripcion(j, e, posicion, dorsal, true, "");
    }

    public boolean cargarInscripcion(Jugador j, Equipo e, String posicion, int dorsal, boolean activa, String motivo) {
        if (j == null || e == null || buscarEquipo(e.getId()) != e) {
            return false;
        }
        InscripcionEquipo i = new InscripcionEquipo(j, e, posicion, dorsal);
        if (!activa && !i.retirar(motivo)) {
            return false;
        }
        return e.vincularInscripcion(i, null);
    }

    public ResultadoOperacion retirarJugador(Jugador j, Equipo e, String motivo) {
        if (j == null || e == null || buscarEquipo(e.getId()) != e || motivo == null || motivo.isBlank()) {
            return ResultadoOperacion.rechazo("Jugador, equipo o motivo inválidos.");
        }
        for (InscripcionEquipo i : e.getInscripciones()) {
            if (i.getJugador() == j && i.retirar(motivo)) {
                return ResultadoOperacion.correcto("Retiro registrado; cupo y dorsal liberados, historia conservada.");
            }
        }
        return ResultadoOperacion.rechazo("El jugador no está inscrito en ese equipo.");
    }
}
