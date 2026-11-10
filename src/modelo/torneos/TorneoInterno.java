package modelo.torneos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import modelo.competencias.FormatoLiga;
import modelo.deportes.Deporte;
import modelo.deportes.NivelAcademia;

public class TorneoInterno {

    public record Disponibilidad(int capacidad, int preferenciales, int regulares, int preferencialesOcupados,
            int regularesOcupados) {
        public int preferencialesDisponibles() {
            return preferenciales - preferencialesOcupados;
        }

        public int regularesDisponibles() {
            return regulares - regularesOcupados;
        }
    }

    private final String id;
    private final String nombre;
    private final Deporte deporte;
    private final String categoria;
    private final NivelAcademia nivel;
    private final ModalidadTorneo modalidad;
    private final int capacidad;
    private final LocalDateTime inicio;
    private final LocalDateTime fechaLiberacion;
    private final TipoTorneo tipo;
    private final BigDecimal tarifa;
    private final List<CandidatoTorneo> clasificacion = new ArrayList<>();
    private final List<InscripcionTorneo> inscripciones = new ArrayList<>();
    private String criterio = "";
    private boolean preferencialesLiberados;
    private FormatoLiga formato;
    private boolean finalizado;
    private List<String> posicionesFinales = List.of();
    private String ganadorId;

    public TorneoInterno(String id, String nombre, Deporte deporte, String categoria, NivelAcademia nivel,
            ModalidadTorneo modalidad, int capacidad, LocalDateTime inicio, LocalDateTime fechaLiberacion,
            TipoTorneo tipo, BigDecimal tarifa) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank() || deporte == null || modalidad == null
                || capacidad < 1 || inicio == null || fechaLiberacion == null || !fechaLiberacion.isBefore(inicio)
                || tipo == null) {
            throw new IllegalArgumentException(
                    "Complete los datos y use cupos positivos y liberación anterior al inicio.");
        }

        if (deporte.isColectivo()) {
            if (modalidad != ModalidadTorneo.EQUIPOS || nivel != null || categoria == null
                    || !Set.of("Sub-13", "Sub-15", "Sub-17", "Mayores").contains(categoria)) {
                throw new IllegalArgumentException(
                        "El deporte colectivo requiere equipos y una categoría válida, sin nivel de academia.");
            }
        } else if (modalidad == ModalidadTorneo.EQUIPOS || nivel == null || categoria != null
                || (deporte.es("PADEL") && modalidad != ModalidadTorneo.DOBLES)) {
            throw new IllegalArgumentException(
                    "La disciplina individual requiere nivel y sencillos/dobles; pádel exige dobles.");
        }

        if (tipo == TipoTorneo.COMPETITIVO
                && (tarifa == null || tarifa.signum() <= 0 || tarifa.stripTrailingZeros().scale() > 2)) {
            throw new IllegalArgumentException(
                    "La tarifa competitiva debe ser positiva y tener como máximo dos decimales.");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
        this.deporte = deporte;
        this.categoria = categoria;
        this.nivel = nivel;
        this.modalidad = modalidad;
        this.capacidad = capacidad;
        this.inicio = inicio;
        this.fechaLiberacion = fechaLiberacion;
        this.tipo = tipo;
        if (tipo == TipoTorneo.RECREATIVO) {
            this.tarifa = BigDecimal.ZERO.setScale(2);
        } else {
            this.tarifa = tarifa.setScale(2);
        }
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public String getCategoria() {
        return categoria;
    }

    public NivelAcademia getNivel() {
        return nivel;
    }

    public ModalidadTorneo getModalidad() {
        return modalidad;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFechaLiberacion() {
        return fechaLiberacion;
    }

    public TipoTorneo getTipo() {
        return tipo;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public String getCriterio() {
        return criterio;
    }

    public FormatoLiga getFormato() {
        return formato;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    public List<String> getPosicionesFinales() {
        return posicionesFinales;
    }

    public String getGanadorId() {
        return ganadorId;
    }

    public boolean estanPreferencialesLiberados() {
        return preferencialesLiberados;
    }

    public List<CandidatoTorneo> getClasificacion() {
        return List.copyOf(clasificacion);
    }

    public List<InscripcionTorneo> getInscripciones() {
        return List.copyOf(inscripciones);
    }

    public int getReservaInicial() {
        int preferenciales = capacidad / 5;
        if (capacidad % 5 != 0) {
            preferenciales++;
        }
        return preferenciales;
    }

    public boolean admiteInscripciones(LocalDateTime ahora) {
        return ahora != null && ahora.isBefore(inicio);
    }

    public List<CandidatoTorneo> getElegiblesPreferenciales() {
        return clasificacion.stream().limit(getReservaInicial()).toList();
    }

    public Disponibilidad getDisponibilidad() {
        int preferencialesOcupados = (int) inscripciones.stream()
                .filter(i -> i.activa() && i.cupo() == TipoCupo.PREFERENCIAL).count();
        int regularesOcupados = (int) inscripciones.stream().filter(i -> i.activa() && i.cupo() == TipoCupo.REGULAR)
                .count();
        int preferenciales;
        if (preferencialesLiberados) {
            preferenciales = preferencialesOcupados;
        } else {
            preferenciales = getReservaInicial();
        }
        return new Disponibilidad(capacidad, preferenciales, capacidad - preferenciales, preferencialesOcupados,
                regularesOcupados);
    }

    public void asignarClasificacion(String criterio, List<CandidatoTorneo> candidatos) {
        if (criterio == null || criterio.isBlank() || candidatos == null || candidatos.stream().anyMatch(c -> c == null)
                || !inscripciones.isEmpty() || preferencialesLiberados) {
            throw new IllegalArgumentException("Defina un criterio antes de inscribir participantes o liberar cupos.");
        }
        Set<String> ids = new HashSet<>();
        Set<String> jugadores = new HashSet<>();
        for (CandidatoTorneo c : candidatos) {
            validarModalidad(c);
            if (c.id().startsWith("EXTERNO:") != c.jugadores().stream().anyMatch(j -> j.startsWith("EXTERNO:"))) {
                throw new IllegalArgumentException("Identidad externa de participante inconsistente.");
            }

            if (modalidad != ModalidadTorneo.SENCILLOS
                    && c.jugadores().stream().anyMatch(j -> j.startsWith("EXTERNO:"))) {
                throw new IllegalArgumentException("Los participantes externos solo se admiten en sencillos.");
            }

            if (!ids.add(c.id()) || c.jugadores().stream().anyMatch(j -> !jugadores.add(j))) {
                throw new IllegalArgumentException("La clasificación repite un participante o jugador.");
            }
        }
        List<CandidatoTorneo> ordenados = new ArrayList<>(candidatos);
        ordenados.sort(
                Comparator.comparingDouble(CandidatoTorneo::puntaje).reversed().thenComparing(CandidatoTorneo::id));
        clasificacion.clear();
        clasificacion.addAll(ordenados);
        this.criterio = criterio.trim();
    }

    private void validarModalidad(CandidatoTorneo candidato) {
        boolean equipo = candidato.equipoId() != null;
        if ((modalidad == ModalidadTorneo.EQUIPOS) != equipo
                || (modalidad == ModalidadTorneo.SENCILLOS && candidato.jugadores().size() != 1)
                || (modalidad == ModalidadTorneo.DOBLES && candidato.jugadores().size() != 2)
                || (equipo && candidato.jugadores().size() < deporte.getMinimoJugadores())) {
            throw new IllegalArgumentException("El participante no corresponde a la modalidad o al mínimo deportivo.");
        }
    }

    public void registrarInscripcion(CandidatoTorneo participante, TipoCupo cupo) {
        validarInscripcion(participante, cupo);
        inscripciones.add(new InscripcionTorneo("INS-" + (inscripciones.size() + 1), participante, cupo, true));
    }

    private void validarInscripcion(CandidatoTorneo participante, TipoCupo cupo) {
        if (participante == null || cupo == null) {
            throw new IllegalArgumentException("Seleccione participante y tipo de cupo.");
        }
        validarModalidad(participante);
        if (cupo == TipoCupo.PREFERENCIAL && participante.id().startsWith("EXTERNO:") && (clasificacion.isEmpty()
                || !clasificacion.subList(0, Math.min(getReservaInicial(), clasificacion.size())).stream().anyMatch(
                        c -> c.id().equals(participante.id()) && c.jugadores().equals(participante.jugadores())))) {
            throw new IllegalArgumentException(
                    "No es elegible para cupo preferencial; puede solicitar un cupo regular disponible.");
        }

        if (modalidad != ModalidadTorneo.SENCILLOS
                && participante.jugadores().stream().anyMatch(j -> j.startsWith("EXTERNO:"))) {
            throw new IllegalArgumentException("Los participantes externos solo se admiten en sencillos.");
        }
        boolean repetido = inscripciones.stream()
                .anyMatch(i -> i.activa() && (i.participante().id().equals(participante.id())
                        || i.participante().jugadores().stream().anyMatch(participante.jugadores()::contains)));
        if (repetido) {
            throw new IllegalArgumentException("El participante o uno de sus integrantes ya está inscrito.");
        }
        Disponibilidad d = getDisponibilidad();
        if (cupo == TipoCupo.PREFERENCIAL) {
            if (preferencialesLiberados || getElegiblesPreferenciales().stream()
                    .noneMatch(c -> c.id().equals(participante.id()) && c.jugadores().equals(participante.jugadores())
                            && java.util.Objects.equals(c.equipoId(), participante.equipoId()))) {
                throw new IllegalArgumentException(
                        "No es elegible para cupo preferencial; puede solicitar un cupo regular disponible.");
            }

            if (d.preferencialesDisponibles() < 1) {
                throw new IllegalArgumentException("No quedan cupos preferenciales.");
            }
        } else if (d.regularesDisponibles() < 1) {
            throw new IllegalArgumentException(
                    "No quedan cupos regulares; los preferenciales sin liberar siguen reservados.");
        }
    }

    public void retirarInscripcion(String participanteId) {
        for (int i = 0; i < inscripciones.size(); i++) {
            InscripcionTorneo inscripcion = inscripciones.get(i);
            if (inscripcion.activa() && inscripcion.participante().id().equals(participanteId)) {
                inscripciones.set(i,
                        new InscripcionTorneo(inscripcion.id(), inscripcion.participante(), inscripcion.cupo(), false));
                return;
            }
        }
        throw new IllegalArgumentException(
                "La inscripción no existe o ya fue retirada; no se liberaron cupos adicionales.");
    }

    public int liberarPreferenciales(LocalDateTime ahora) {
        if (!admiteInscripciones(ahora)) {
            throw new IllegalArgumentException("El periodo de inscripción terminó.");
        }

        if (ahora.isBefore(fechaLiberacion)) {
            throw new IllegalArgumentException("Aún no llega la fecha establecida para liberar cupos.");
        }
        int disponibles = getDisponibilidad().preferencialesDisponibles();
        if (preferencialesLiberados || disponibles == 0) {
            throw new IllegalArgumentException("No hay nuevos cupos preferenciales por liberar.");
        }
        preferencialesLiberados = true;
        return disponibles;
    }

    public void restaurarInscripcion(InscripcionTorneo inscripcion) {
        if (inscripcion == null || !inscripcion.id().equals("INS-" + (inscripciones.size() + 1))) {
            throw new IllegalArgumentException("Secuencia de inscripciones inválida.");
        }
        validarModalidad(inscripcion.participante());
        if (inscripcion.cupo() == TipoCupo.PREFERENCIAL && !inscripcion.participante().id().startsWith("EXTERNO:")
                && getElegiblesPreferenciales().stream().noneMatch(c -> c.id().equals(inscripcion.participante().id())
                        && c.jugadores().equals(inscripcion.participante().jugadores()))) {
            throw new IllegalArgumentException("Inscripción preferencial sin elegibilidad registrada.");
        }

        if (inscripcion.activa() && inscripciones.stream().anyMatch(
                i -> i.activa() && (i.participante().id().equals(inscripcion.participante().id()) || i.participante()
                        .jugadores().stream().anyMatch(inscripcion.participante().jugadores()::contains)))) {
            throw new IllegalArgumentException("Participante activo duplicado.");
        }
        inscripciones.add(inscripcion);
    }

    public void restaurarLiberacion(boolean liberados) {
        preferencialesLiberados = liberados;
        Disponibilidad d = getDisponibilidad();
        if (d.regularesDisponibles() < 0 || d.preferencialesDisponibles() < 0
                || d.preferencialesOcupados() > getReservaInicial()) {
            throw new IllegalArgumentException("Las inscripciones exceden la distribución de cupos.");
        }
    }

    public EstadoTorneo estado(LocalDateTime now){
        if (now == null){
            throw new IllegalArgumentException("Se necesita la fecha de la consulta");
        }
        if (finalizado) {
            return EstadoTorneo.FINALIZADO;
        }
        if (!admiteInscripciones(now)) {
            return EstadoTorneo.EN_CURSO;
        }
        return EstadoTorneo.INSCRIPCIONES_ABIERTAS;
    }

    public void iniciar(FormatoLiga nuevoFormatoL) {
        if (nuevoFormatoL == null) {
            throw new IllegalArgumentException("Se requiere el nuevo formato de la competencia");
        }
        if (formato != null) {
            throw new IllegalArgumentException("La competencia del torneo ya ha sido iniciada");
        }
        formato = nuevoFormatoL;
    }

    public void finalizar(List<String> posiciones, String ganador) {
        if (finalizado) {
            throw new IllegalArgumentException("El torneo ya termino");
        }
        if (formato == null || posiciones == null || posiciones.isEmpty() || ganador == null
                || !posiciones.get(0).equals(ganador)) {
            throw new IllegalArgumentException("Posiciones finales no validas");
        }
        posicionesFinales = List.copyOf(posiciones);
        ganadorId = ganador;
        finalizado = true;
    }

    public CandidatoTorneo buscarInscritoActivo(String participanteId) {
        for (InscripcionTorneo inscripcion : inscripciones) {
            if (inscripcion.activa() && inscripcion.participante().id().equals(participanteId)) {
                return inscripcion.participante();
            }
        }
        return null;
    }
}
