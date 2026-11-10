package modelo.competencias;

import java.util.*;
import modelo.deportes.Deporte;
import modelo.partidos.PartidoCompetencia;

public class FormatoLiga implements FormatoCompetencia {

    public record Fila(String participanteId, String nombre, int jugados, int ganados, int empatados, int perdidos,
            int favor, int contra, int puntos) {
        public int diferencia() {
            return favor - contra;
        }
    }

    protected final Map<String, ParticipanteCompetencia> participantes = new LinkedHashMap<>();
    protected final Map<String, EncuentroTorneo> encuentros = new LinkedHashMap<>();
    private final Map<String, String> vinculos = new LinkedHashMap<>();
    private Map<String, Integer> legado = new LinkedHashMap<>();
    private List<String> procesadosLegado = new ArrayList<>();
    protected Deporte deporte;
    protected int setsParaGanar = 3;
    private ReglasPuntuacion reglas;

    public void configurarDeporte(Deporte deporte) {
        if (deporte == null) {
            throw new IllegalArgumentException("Deporte inexistente.");
        }
        this.deporte = deporte;
        if (reglas == null) {
            if (deporte.es("FUTBOL")) {
                reglas = new ReglasPuntuacion(3, 1, 0, false);
            } else if (deporte.es("BALONCESTO")) {
                reglas = new ReglasPuntuacion(2, 0, 1, false);
            } else {
                reglas = new ReglasPuntuacion(3, 0, 0, true);
            }
        }
    }

    public boolean configurarPuntuacion(ReglasPuntuacion nuevas) {
        if (nuevas == null || encuentros.values().stream().anyMatch(EncuentroTorneo::completo)
                || (deporte != null && deporte.es("FUTBOL") && !nuevas.equals(new ReglasPuntuacion(3, 1, 0, false)))) {
            return false;
        }
        reglas = nuevas;
        return true;
    }

    public boolean configurarSets(int objetivo) {
        if ((objetivo != 2 && objetivo != 3) || encuentros.values().stream().anyMatch(EncuentroTorneo::completo)) {
            return false;
        }
        setsParaGanar = objetivo;
        return true;
    }

    public int getSetsParaGanar() {
        return setsParaGanar;
    }

    public ReglasPuntuacion getReglas() {
        return reglas;
    }

    public List<ParticipanteCompetencia> getParticipantes() {
        return List.copyOf(participantes.values());
    }

    public ParticipanteCompetencia buscarParticipante(String id) {
        return participantes.get(id);
    }

    public List<EncuentroTorneo> getEncuentros() {
        return List.copyOf(encuentros.values());
    }

    public EncuentroTorneo buscarEncuentro(String id) {
        return encuentros.get(id);
    }

    public Map<String, String> getVinculos() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(vinculos));
    }

    public boolean registrarParticipante(ParticipanteCompetencia p) {
        return p != null && participantes.putIfAbsent(p.id(), p) == null;
    }

    public boolean cambiarActivo(String id, boolean activo) {
        ParticipanteCompetencia p = participantes.get(id);
        if (p == null) {
            return false;
        }
        participantes.put(id, new ParticipanteCompetencia(id, p.nombre(), p.equipoClubId(), activo));
        return true;
    }

    public boolean registrarEncuentro(String id, String local, String visitante) {
        return agregarEncuentro(new EncuentroTorneo(id, local, visitante, "LIGA", "", null, null, null, false));
    }

    protected boolean agregarEncuentro(EncuentroTorneo e) {
        if (e == null || !participantes.containsKey(e.localId()) || !participantes.containsKey(e.visitanteId())
                || encuentros.containsKey(e.id())) {
            return false;
        }
        encuentros.put(e.id(), e);
        return true;
    }

    protected boolean marcadorValido(EncuentroTorneo e, int local, int visitante, String desempate) {
        if (deporte == null || !deporte.marcadorValido(local, visitante)) {
            return false;
        }

        if (deporte.es("VOLEIBOL")
                && (Math.max(local, visitante) != setsParaGanar || Math.min(local, visitante) >= setsParaGanar)) {
            return false;
        }

        if (desempate != null && (!e.fase().startsWith("ELIMINACION") || !deporte.es("FUTBOL") || local != visitante
                || (!desempate.equals(e.localId()) && !desempate.equals(e.visitanteId())))) {
            return false;
        }
        return (!e.fase().startsWith("ELIMINACION") || local != visitante || desempate != null);
    }

    public boolean registrarMarcador(String id, int local, int visitante, String desempate) {
        EncuentroTorneo e = encuentros.get(id);
        if (e == null || !participantes.get(e.localId()).activo() || !participantes.get(e.visitanteId()).activo()) {
            return false;
        }
        return completar(e, local, visitante, desempate, false);
    }

    public boolean victoriaAdministrativa(String id, String ganador) {
        EncuentroTorneo e = encuentros.get(id);
        if (e == null || e.completo() || ganador == null
                || (!ganador.equals(e.localId()) && !ganador.equals(e.visitanteId()))) {
            return false;
        }
        String perdedor;
        if (ganador.equals(e.localId())) {
            perdedor = e.visitanteId();
        } else {
            perdedor = e.localId();
        }

        if (!participantes.get(ganador).activo() || participantes.get(perdedor).activo()) {
            return false;
        }
        int valor;
        if (deporte.es("VOLEIBOL")) {
            valor = setsParaGanar;
        } else {
            valor = 1;
        }
        int marcadorLocalAdministrativo;
        if (ganador.equals(e.localId())) {
            marcadorLocalAdministrativo = valor;
        } else {
            marcadorLocalAdministrativo = 0;
        }
        int marcadorVisitanteAdministrativo;
        if (ganador.equals(e.visitanteId())) {
            marcadorVisitanteAdministrativo = valor;
        } else {
            marcadorVisitanteAdministrativo = 0;
        }
        return completar(e, marcadorLocalAdministrativo, marcadorVisitanteAdministrativo, null, true);
    }

    protected boolean completar(EncuentroTorneo e, int local, int visitante, String desempate, boolean administrativo) {
        if (e.completo() || !marcadorValido(e, local, visitante, desempate)) {
            return false;
        }
        encuentros.put(e.id(), new EncuentroTorneo(e.id(), e.localId(), e.visitanteId(), e.fase(), e.grupo(), local,
                visitante, desempate, administrativo));
        return true;
    }

    public boolean restaurarMarcador(String id, int local, int visitante, String desempate, boolean administrativo) {
        EncuentroTorneo e = encuentros.get(id);
        return (e != null && completar(e, local, visitante, desempate, administrativo));
    }

    private int puntos(EncuentroTorneo e, boolean local) {
        int a;
        if (local) {
            a = e.local();
        } else {
            a = e.visitante();
        }
        int b;
        if (local) {
            b = e.visitante();
        } else {
            b = e.local();
        }

        if (reglas.ajustadaPorSets() && deporte.es("VOLEIBOL")) {
            if (a > b) {
                if (b == setsParaGanar - 1) {
                    return 2;
                } else {
                    return 3;
                }
            } else if (a == setsParaGanar - 1) {
                return 1;
            } else {
                return 0;
            }
        }

        if (a > b) {
            return reglas.victoria();
        } else if (a == b) {
            return reglas.empate();
        } else {
            return reglas.derrota();
        }
    }

    public List<Fila> getTabla() {
        return tabla(encuentros.values(), participantes.keySet());
    }

    protected List<Fila> tabla(Collection<EncuentroTorneo> juegos, Collection<String> ids) {
        Map<String, int[]> valores = new LinkedHashMap<>();
        for (String id : ids) {
            valores.put(id, new int[7]);
        }
        for (EncuentroTorneo e : juegos) {
            if (e.completo()) {
                for (boolean local : new boolean[] { true, false }) {
                    String participanteId;
                    if (local) {
                        participanteId = e.localId();
                    } else {
                        participanteId = e.visitanteId();
                    }
                    int[] v = valores.get(participanteId);
                    if (v == null) {
                        continue;
                    }
                    int a;
                    if (local) {
                        a = e.local();
                    } else {
                        a = e.visitante();
                    }
                    int b;
                    if (local) {
                        b = e.visitante();
                    } else {
                        b = e.local();
                    }
                    v[0]++;
                    if (a > b) {
                        v[1]++;
                    } else if (a == b) {
                        v[2]++;
                    } else {
                        v[3]++;
                    }
                    v[4] += a;
                    v[5] += b;
                    v[6] += puntos(e, local);
                }
            }
        }
        List<Fila> filas = new ArrayList<>();
        valores.forEach((id, v) -> filas
                .add(new Fila(id, participantes.get(id).nombre(), v[0], v[1], v[2], v[3], v[4], v[5], v[6])));
        filas.sort(Comparator.comparingInt(Fila::puntos).reversed()
                .thenComparing(Comparator.comparingInt(Fila::diferencia).reversed())
                .thenComparing(Comparator.comparingInt(Fila::favor).reversed()).thenComparing(Fila::participanteId));
        return List.copyOf(filas);
    }

    public boolean yaProcesado(String id) {
        return vinculos.containsKey(id) || procesadosLegado.contains(id);
    }

    public int obtenerPuntos(String id) {
        return getTabla().stream().filter(f -> f.participanteId().equals(id)).mapToInt(Fila::puntos).findFirst()
                .orElse(legado.getOrDefault(id, 0));
    }

    public Map<String, Integer> getPuntosEquipos() {
        Map<String, Integer> puntos = new LinkedHashMap<>(legado);
        for (Fila f : getTabla()) {
            if (participantes.get(f.participanteId()).equipoClubId() != null) {
                puntos.put(participantes.get(f.participanteId()).equipoClubId(), f.puntos());
            }
        }
        return puntos;
    }

    public List<String> getPartidosProcesados() {
        if (vinculos.isEmpty()) {
            return List.copyOf(procesadosLegado);
        } else {
            return List.copyOf(vinculos.keySet());
        }
    }

    public void cargarEstado(Map<String, Integer> puntos, List<String> procesados) {
        if (!vinculos.isEmpty() || !legado.isEmpty() || !procesadosLegado.isEmpty()
                || puntos.values().stream().anyMatch(v -> v < 0)
                || new HashSet<>(procesados).size() != procesados.size()) {
            throw new IllegalArgumentException("Estado de liga inválido.");
        }
        legado = new LinkedHashMap<>(puntos);
        procesadosLegado = new ArrayList<>(procesados);
    }

    public void reconstruirLegado(List<PartidoCompetencia> juegos) {
        Map<String, Integer> antes = new LinkedHashMap<>(legado);
        legado.clear();
        procesadosLegado.clear();
        for (PartidoCompetencia p : juegos) {
            if (!procesarResultado(p)) {
                throw new IllegalArgumentException("No se pudo reconstruir la liga histórica.");
            }
        }
        if (!getPuntosEquipos().equals(antes)) {
            throw new IllegalArgumentException("Puntos históricos inconsistentes.");
        }
    }

    public boolean procesarResultado(PartidoCompetencia p) {
        if (p == null || p.getCompetencia() == null || p.getCompetencia().getFormato() != this || !p.tieneResultado()
                || p.estaCancelado() || yaProcesado(p.getId())) {
            return false;
        }
        String propio = p.getEquipoClub().getId(), rival = p.getRivalId(), id = p.getEncuentroTorneoId();
        EncuentroTorneo e = encuentros.get(id);
        if (e == null && this instanceof FormatoFases) {
            return false;
        }

        if (e == null) {
            e = new EncuentroTorneo(id, propio, rival, "LIGA", "", null, null, null, false);
        }

        if (e.completo() || (!(e.localId().equals(propio) && e.visitanteId().equals(rival))
                && !(e.visitanteId().equals(propio) && e.localId().equals(rival)))) {
            return false;
        }
        boolean local = e.localId().equals(propio);
        int l;
        if (local) {
            l = p.getMarcadorClub();
        } else {
            l = p.getMarcadorRival();
        }
        int v;
        if (local) {
            v = p.getMarcadorRival();
        } else {
            v = p.getMarcadorClub();
        }

        if (!marcadorValido(e, l, v, p.getGanadorDesempateId())) {
            return false;
        }
        participantes.putIfAbsent(propio,
                new ParticipanteCompetencia(propio, p.getEquipoClub().getNombre(), propio, true));
        String equipoRivalId;
        if (p.getEquipoRivalClub() == null) {
            equipoRivalId = null;
        } else {
            equipoRivalId = p.getEquipoRivalClub().getId();
        }
        participantes.putIfAbsent(rival,
                new ParticipanteCompetencia(rival, p.getInstitucionRival(), equipoRivalId, true));
        if (!encuentros.containsKey(id)) {
            encuentros.put(id, e);
        }

        if (!completar(e, l, v, p.getGanadorDesempateId(), false)) {
            throw new IllegalStateException("Resultado cambió durante su confirmación.");
        }
        vinculos.put(p.getId(), id);
        return true;
    }

    public void cargarVinculo(String partido, String encuentro) {
        if (partido == null || !encuentros.containsKey(encuentro) || !encuentros.get(encuentro).completo()
                || vinculos.putIfAbsent(partido, encuentro) != null) {
            throw new IllegalArgumentException("Vínculo de torneo inválido.");
        }
    }

    public boolean avanzar() {
        return false;
    }

    public String getCampeon() {
        return null;
    }
}
