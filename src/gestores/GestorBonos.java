package gestores;

import aplicacion.EstadoClub;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import modelo.ventas.BonoDescuento;
import gestores.GestorAcceso.Perfil;
import gestores.GestorAcceso.Sesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import modelo.torneos.CandidatoTorneo;
import modelo.torneos.TipoTorneo;
import modelo.torneos.TorneoInterno;

public class GestorBonos {
    public static final BigDecimal PORCENTAJE_BONO = new BigDecimal("0.10");

    private final EstadoClub club;
    private final Map<String, BonoDescuento> bonos = new LinkedHashMap<>();

    public GestorBonos(EstadoClub club) {
        this.club = club;
    }

    public BonoDescuento buscar(String id) {
        return id == null ? null : bonos.get(id);
    }

    public List<BonoDescuento> consultarParaPersistencia() {
        return List.copyOf(bonos.values());
    }

    public void cargar(BonoDescuento bono) {
        if (bono == null || bonos.putIfAbsent(bono.getId(), bono) != null) {
            throw new IllegalArgumentException("Bono nulo o duplicado.");
        }
    }


    public List<BonoDescuento> emitir(TorneoInterno torneo) {
        if (torneo == null || !torneo.isFinalizado() || torneo.getTipo() != TipoTorneo.RECREATIVO) {
            return List.of();
        }
        CandidatoTorneo ganador = torneo.buscarInscritoActivo(torneo.getGanadorId());
        if (ganador == null) {
            return List.of();
        }
        List<BonoDescuento> emitidos = new ArrayList<>();
        for (String jugadorId : ganador.jugadores()) {
            if (!club.getAcceso().tienePerfil(jugadorId, Perfil.SOCIO)) {
                continue;
            }
            String id = "BONO-" + torneo.getId() + "-" + jugadorId;
            if (bonos.containsKey(id)) {
                continue;
            }
            BonoDescuento bono = new BonoDescuento(id, jugadorId, torneo.getId(), PORCENTAJE_BONO,
                    LocalDate.now(club.getReloj()));
            bonos.put(id, bono);
            emitidos.add(bono);
        }
        return emitidos;
    }

    public List<BonoDescuento> consultarPropios(Sesion sesion) {
        if (!club.getAcceso().estaAutenticada(sesion)) {
            throw new IllegalArgumentException("Inicie sesión para consultar sus bonos.");
        }
        List<BonoDescuento> propios = new ArrayList<>();
        for (BonoDescuento bono : bonos.values()) {
            if (bono.getTitularId().equals(sesion.getUsuarioId())) {
                propios.add(bono);
            }
        }
        return propios;
    }
}
