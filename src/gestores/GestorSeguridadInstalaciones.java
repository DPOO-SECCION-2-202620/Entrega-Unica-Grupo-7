package gestores;

import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import java.time.*;
import java.util.*;
import modelo.reservas.*;

public class GestorSeguridadInstalaciones {

    private final GestorReservas reservas;
    private final GestorAcceso acceso;
    private final Clock reloj;
    private final java.util.function.Function<String, modelo.usuarios.Usuario> usuarios;
    private final Set<String> bebidasCalientes = new LinkedHashSet<>();
    private final Map<String, String> usos = new LinkedHashMap<>();

    public GestorSeguridadInstalaciones(GestorReservas reservas, GestorAcceso acceso, Clock reloj,
            java.util.function.Function<String, modelo.usuarios.Usuario> usuarios) {
        this.reservas = reservas;
        this.acceso = acceso;
        this.reloj = reloj;
        this.usuarios = usuarios;
    }

    private boolean puedeGestionar(Sesion sesion, String usuarioId) {
        return (acceso.estaAutenticada(sesion) && (sesion.getUsuarioId().equals(usuarioId)
                || acceso.tienePerfil(sesion, Perfil.EMPLEADO) || acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR)));
    }

    public boolean tieneBebidaCaliente(String usuarioId) {
        return bebidasCalientes.contains(usuarioId);
    }

    public boolean puedeDespacharCaliente(String usuarioId) {
        Reserva reserva = reservas.buscarReserva(usos.get(usuarioId));
        return (reserva == null || !estaVigente(reserva) || !reserva.getInstalacion().restringeBebidasCalientes());
    }

    private boolean estaVigente(Reserva reserva) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return (!reserva.estaCancelada() && !ahora.isBefore(reserva.getInicio()) && ahora.isBefore(reserva.getFin()));
    }

    public boolean iniciarUso(Sesion sesion, String reservaId) {
        Reserva reserva = reservas.buscarReserva(reservaId);
        return (reserva != null && iniciarUso(sesion, reservaId, reserva.getJugador().getId()));
    }

    public boolean iniciarUso(Sesion sesion, String reservaId, String usuarioId) {
        Reserva reserva = reservas.buscarReserva(reservaId);
        modelo.usuarios.Usuario usuario = usuarios.apply(usuarioId);
        if (reserva == null || !estaVigente(reserva) || usuario == null || !usuario.esIdentificado()
                || !usuario.estaActivo() || acceso.tienePerfil(usuarioId, Perfil.ADMINISTRADOR)) {
            return false;
        }
        boolean personal = acceso.tienePerfil(sesion, Perfil.EMPLEADO)
                || acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR);
        if (!puedeGestionar(sesion, usuarioId) || (!personal && !reserva.getJugador().getId().equals(usuarioId))
                || (tieneBebidaCaliente(usuarioId) && reserva.getInstalacion().restringeBebidasCalientes())) {
            return false;
        }
        Reserva actual = reservas.buscarReserva(usos.get(usuarioId));
        if (actual != null && estaVigente(actual)) {
            return false;
        }

        if (usos.values().stream().filter(reservaId::equals).count() >= reserva.getCantidadParticipantes()) {
            return false;
        }
        usos.put(usuarioId, reservaId);
        return true;
    }

    public boolean finalizarUso(Sesion sesion, String usuarioId) {
        if (!puedeGestionar(sesion, usuarioId)) {
            return false;
        }
        return usos.remove(usuarioId) != null;
    }

    public boolean registrarPosesion(Sesion sesion, String usuarioId) {
        if (usuarios.apply(usuarioId) == null || !usuarios.apply(usuarioId).esIdentificado()
                || !usuarios.apply(usuarioId).estaActivo()
                || (!acceso.tienePerfil(sesion, Perfil.EMPLEADO) && !acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR))
                || !puedeDespacharCaliente(usuarioId)) {
            return false;
        }
        bebidasCalientes.add(usuarioId);
        return true;
    }

    public boolean finalizarBebida(Sesion sesion, String usuarioId) {
        return (puedeGestionar(sesion, usuarioId) && bebidasCalientes.remove(usuarioId));
    }

    public Set<String> getPosesiones() {
        return Set.copyOf(bebidasCalientes);
    }

    public Map<String, String> getUsos() {
        return new LinkedHashMap<>(usos);
    }

    /** Reconstrucción: no simula compras o ingresos de nuevo. */
    public void cargarPosesion(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank() || !bebidasCalientes.add(usuarioId)) {
            throw new IllegalArgumentException("Posesión inválida o duplicada.");
        }
    }

    public void cargarUso(String usuarioId, String reservaId) {
        Reserva reserva = reservas.buscarReserva(reservaId);
        if (reserva == null || usuarios.apply(usuarioId) == null || !usuarios.apply(usuarioId).esIdentificado()
                || acceso.tienePerfil(usuarioId, Perfil.ADMINISTRADOR) || usos.containsKey(usuarioId)
                || usos.values().stream().filter(reservaId::equals).count() >= reserva.getCantidadParticipantes()
                || (tieneBebidaCaliente(usuarioId) && reserva.getInstalacion().restringeBebidasCalientes()
                        && estaVigente(reserva))) {
            throw new IllegalArgumentException("Uso inválido o incompatible con bebida caliente.");
        }
        usos.put(usuarioId, reservaId);
    }
}
