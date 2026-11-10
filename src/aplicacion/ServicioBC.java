package aplicacion;

import gestores.GestorAcceso.*;
import java.time.*;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.entrenamientos.*;
import modelo.partidos.*;
import modelo.reservas.*;
import modelo.usuarios.*;

public class ServicioBC {

    private final EstadoClub club;

    public ServicioBC(EstadoClub club) {
        this.club = club;
    }

    private boolean docente(Sesion s) {
        return (club.getOperaciones().esAdministrador(s) || club.getOperaciones().esEntrenador(s));
    }

    private FormatoLiga formato(String id) {
        Competencia c = club.getCompetencias().buscarCompetencia(id);
        if (c != null && c.getFormato() instanceof FormatoLiga f) {
            return f;
        } else {
            return null;
        }
    }

    private ResultadoOperacion r(boolean ok, String mensaje) {
        if (ok) {
            return ResultadoOperacion.correcto(mensaje);
        } else {
            return ResultadoOperacion.rechazo("Operación rechazada: " + mensaje);
        }
    }

    public ResultadoOperacion registrarExterno(Sesion s, String competencia, String id, String nombre) {
        FormatoLiga f = formato(competencia);
        if (!club.getOperaciones().esAdministrador(s) || f == null || club.getEquipos().buscarEquipo(id) != null) {
            return ResultadoOperacion.rechazo("Administrador, competencia o ID externo inválidos.");
        }
        try {
            return r(f.registrarParticipante(new ParticipanteCompetencia(id, nombre, null, true)),
                    "Participante registrado; ID único o grupos ya iniciados.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion configurarPuntuacion(Sesion s, String competencia, ReglasPuntuacion reglas, int sets) {
        FormatoLiga f = formato(competencia);
        Competencia c = club.getCompetencias().buscarCompetencia(competencia);
        if (!club.getOperaciones().esAdministrador(s) || f == null || reglas == null || (sets != 2 && sets != 3)
                || club.getPartidos().getPartidos().stream()
                        .anyMatch(p -> p instanceof PartidoCompetencia pc && pc.getCompetencia() == c)) {
            return ResultadoOperacion.rechazo("Configure antes de programar encuentros oficiales.");
        }

        if (c.getDeporte().es("FUTBOL") && !reglas.equals(new ReglasPuntuacion(3, 1, 0, false))) {
            return ResultadoOperacion.rechazo("Fútbol conserva puntuación 3/1/0.");
        }

        if (!f.configurarPuntuacion(reglas)) {
            return ResultadoOperacion.rechazo("La competencia ya tiene resultados.");
        }

        if (!c.configurarSetsParaGanar(sets)) {
            throw new IllegalStateException("La configuración cambió durante la operación.");
        }
        return ResultadoOperacion.correcto("Puntuación y sets configurados.");
    }

    public ResultadoOperacion registrarEncuentro(Sesion s, String competencia, String id, String local,
            String visitante) {
        FormatoLiga f = formato(competencia);
        return r(docente(s) && f != null && f.registrarEncuentro(id, local, visitante),
                "Encuentro de liga registrado; para fases utilice sus cruces generados.");
    }

    public ResultadoOperacion resultadoExterno(Sesion s, String competencia, String id, int local, int visitante,
            String desempate) {
        FormatoLiga f = formato(competencia);
        EncuentroTorneo e;
        if (f == null) {
            e = null;
        } else {
            e = f.buscarEncuentro(id);
        }

        if (!docente(s) || e == null) {
            return ResultadoOperacion.rechazo("Encuentro inexistente o sin permiso deportivo.");
        }

        if (f.buscarParticipante(e.localId()).equipoClubId() != null
                || f.buscarParticipante(e.visitanteId()).equipoClubId() != null) {
            return ResultadoOperacion
                    .rechazo("Los equipos del club deben registrar partido, alineaciones, resultado y cierre de acta.");
        }
        return r(f.registrarMarcador(id, local, visitante, desempate),
                "Resultado externo confirmado una vez; revise marcador/desempate si se rechaza.");
    }

    public ResultadoOperacion configurarGrupos(Sesion s, String competencia, Map<String, List<String>> grupos,
            int clasificados) {
        FormatoLiga f = formato(competencia);
        return r(
                club.getOperaciones().esAdministrador(s) && f instanceof FormatoFases fases
                        && fases.configurarGrupos(grupos, clasificados),
                "Grupos configurados; compruebe participantes únicos, tamaños y clasificación potencia de dos.");
    }

    public ResultadoOperacion avanzar(Sesion s, String competencia) {
        FormatoLiga f = formato(competencia);
        return r(docente(s) && f != null && f.avanzar(),
                "Avance confirmado; se requieren resultados completos y no procesados en el avance anterior.");
    }

    public ResultadoOperacion estadoParticipante(Sesion s, String competencia, String participante, boolean activo) {
        FormatoLiga f = formato(competencia);
        ParticipanteCompetencia p;
        if (f == null) {
            p = null;
        } else {
            p = f.buscarParticipante(participante);
        }

        if (!club.getOperaciones().esAdministrador(s) || p == null) {
            return ResultadoOperacion.rechazo("Participante inexistente o sin permiso administrativo.");
        }

        if (activo && p.equipoClubId() != null) {
            Equipo e = club.getEquipos().buscarEquipo(p.equipoClubId());
            NominaCompetencia n = club.getCompetencias().buscarNomina(competencia, p.equipoClubId());
            if (e == null || !e.estaActivo() || n == null || club.getHabilitacion().contarHabilitadosNomina(n,
                    LocalDateTime.now(club.getReloj())) < e.getDeporte().getMinimoJugadores()) {
                return ResultadoOperacion
                        .rechazo("El equipo no tiene nómina habilitada para reactivarse en el torneo.");
            }
        }

        if (!activo) {
            var oficiales = club.getPartidos().getPartidos().stream().filter(p0 -> p0 instanceof PartidoCompetencia pc
                    && pc.getCompetencia().getId().equals(competencia)
                    && (pc.getEquipoClub().getId().equals(participante) || pc.getRivalId().equals(participante)))
                    .toList();
            if (oficiales.stream().anyMatch(p0 -> p0.tieneResultado() && !p0.estaCerrado() && !p0.esLegado())) {
                return ResultadoOperacion.rechazo("Cierre primero las actas pendientes del participante.");
            }
            for (var p0 : oficiales) {
                if (!p0.tieneResultado() && !p0.estaCancelado()) {
                    club.getAgenda().cambiar(s, p0, p0.getInstalacion(), p0.getInicio(), p0.getFin(),
                            "Retiro del torneo", true);
                }
            }
        }
        f.cambiarActivo(participante, activo);
        return ResultadoOperacion.correcto("Estado de participante actualizado; resultados conservados.");
    }

    public ResultadoOperacion victoriaAdministrativa(Sesion s, String competencia, String encuentro, String ganador) {
        FormatoLiga f = formato(competencia);
        boolean jugado = club.getPartidos().getPartidos().stream()
                .anyMatch(p -> p instanceof PartidoCompetencia pc && pc.getCompetencia().getId().equals(competencia)
                        && pc.getEncuentroTorneoId().equals(encuentro) && pc.tieneResultado());
        return r(
                !jugado && club.getOperaciones().esAdministrador(s) && f != null
                        && f.victoriaAdministrativa(encuentro, ganador),
                "Victoria administrativa registrada; requiere perdedor retirado y ganador activo.");
    }

    public ResultadoOperacion actualizarNomina(Sesion s, String competencia, String equipoId, List<String> ids) {
        Competencia co = club.getCompetencias().buscarCompetencia(competencia);
        Equipo equipo = club.getEquipos().buscarEquipo(equipoId);
        NominaCompetencia actual = club.getCompetencias().buscarNomina(competencia, equipoId);
        if (!docente(s) || co == null || equipo == null || actual == null || ids == null) {
            return ResultadoOperacion.rechazo("Nómina o permiso inválidos.");
        }
        NominaCompetencia propuesta = new NominaCompetencia(co, equipo);
        for (String id : ids) {
            Jugador j = club.buscarJugador(id);
            if (j == null || !j.estaActivo() || club.getAcceso().tienePerfil(id, Perfil.ADMINISTRADOR)
                    || !propuesta.agregarJugador(j)) {
                return ResultadoOperacion.rechazo("Jugador repetido, inactivo o ajeno al equipo.");
            }
        }
        if (!equipo.estaActivo() || club.getHabilitacion().contarHabilitadosNomina(propuesta,
                LocalDateTime.now(club.getReloj())) < co.getDeporte().getMinimoJugadores()) {
            return ResultadoOperacion.rechazo("Nómina insuficiente en habilitados.");
        }
        actual.actualizarActuales(propuesta.getJugadoresActuales());
        return ResultadoOperacion.correcto("Nómina actualizada; selecciones y miembros históricos conservados.");
    }

    public ResultadoOperacion definirDesempate(Sesion s, String partido, String ganador) {
        if (!docente(s) || !(club.getPartidos().buscarPartido(partido) instanceof PartidoCompetencia pc)
                || !pc.tieneResultado() || !pc.getMarcadorClub().equals(pc.getMarcadorRival())
                || !pc.getDeporte().es("FUTBOL")) {
            return ResultadoOperacion.rechazo("Desempate solo para empate eliminatorio con resultado.");
        }
        FormatoLiga f = (FormatoLiga) pc.getCompetencia().getFormato();
        EncuentroTorneo e = f.buscarEncuentro(pc.getEncuentroTorneoId());
        return r(e != null && e.fase().startsWith("ELIMINACION") && pc.definirDesempate(ganador),
                "Ganador de desempate registrado antes del cierre.");
    }

    public List<FormatoLiga.Fila> tabla(Sesion s, String competencia, String grupo) {
        if (!docente(s) && !club.getAcceso().tienePerfil(s, Perfil.SOCIO)) {
            throw new IllegalArgumentException("Inicie sesión con perfil deportivo.");
        }
        FormatoLiga f = formato(competencia);
        if (f == null) {
            throw new IllegalArgumentException("Competencia inexistente.");
        }

        if (f instanceof FormatoFases fases && grupo != null) {
            return fases.getTablaGrupo(grupo);
        } else {
            return f.getTabla();
        }
    }

    public List<EncuentroTorneo> encuentros(Sesion s, String competencia) {
        tabla(s, competencia, null);
        return formato(competencia).getEncuentros();
    }

    public ResultadoOperacion reprogramar(Sesion s, ActividadAgenda a, String instalacion, LocalDateTime inicio,
            int minutos, String motivo) {
        Instalacion destino;
        if (instalacion == null) {
            destino = null;
        } else {
            destino = club.getReservas().buscarInstalacion(instalacion);
        }

        if (instalacion != null && destino == null) {
            return ResultadoOperacion.rechazo("Instalación inexistente.");
        }

        if (inicio == null || minutos < 1) {
            return ResultadoOperacion.rechazo("Inicio/duración inválidos.");
        }
        return club.getAgenda().cambiar(s, a, destino, inicio, inicio.plusMinutes(minutos), motivo, false);
    }

    public ResultadoOperacion cancelar(Sesion s, ActividadAgenda a, String motivo) {
        if (a == null) {
            return ResultadoOperacion.rechazo("Actividad inexistente.");
        } else {
            return club.getAgenda().cambiar(s, a, a.getInstalacion(), a.getInicio(), a.getFin(), motivo, true);
        }
    }

    public List<Instalacion> disponibles(Sesion s, String jugador, String deporte, LocalDateTime inicio,
            LocalDateTime fin, int cantidad, String modalidad) {
        if (!club.getOperaciones().esCaja(s)
                && !(club.getAcceso().tienePerfil(s, Perfil.SOCIO) && s.getUsuarioId().equals(jugador))) {
            throw new IllegalArgumentException("Consulta de disponibilidad sin permiso.");
        }
        Jugador j = club.buscarJugador(jugador);
        Deporte d = club.buscarDeporte(deporte);
        if (j == null || d == null || !club.getAcademias().estaInscrito(jugador, d.getId())) {
            return List.of();
        }
        return club.getReservas().getInstalaciones().stream().filter(i -> i.getDeporte() == d).filter(i -> {
            Reserva r = new Reserva(UUID.randomUUID().toString(), j, i, inicio, fin, cantidad, modalidad);
            return (club.getReservas().datosInvalidos(r) == null && club.getAgenda().validar(r) == null);
        }).toList();
    }

    public ResultadoOperacion programarAcademia(Sesion s, String id, String deporte, String modalidad, List<String> a,
            List<String> b, String instalacion, LocalDateTime inicio, int minutos, int sets) {
        try {
            var config = club.getAcademias().getConfiguracion();
            PartidoAcademia p = new PartidoAcademia(id, inicio, club.buscarDeporte(deporte), modalidad,
                    a.stream().map(club::buscarJugador).toList(), b.stream().map(club::buscarJugador).toList(),
                    config.diferenciaMaxima(), config.excepcionEntrenador(), config.k());
            Instalacion i = club.getReservas().buscarInstalacion(instalacion);
            if (i == null || !p.configurarSetsParaGanar(sets) || !p.configurarOcupacion(i, minutos)) {
                return ResultadoOperacion.rechazo("Instalación/duración/sets inválidos.");
            }
            return club.getAcademias().registrar(s, p);
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion programarGrupo(Sesion s, String id, String disciplina, NivelAcademia nivel,
            String entrenador, String instalacion, LocalDateTime inicio, int minutos) {
        Entrenamiento e = new Entrenamiento(id, inicio, minutos,
                club.getVentas().buscarEmpleadoPorCodigo("EMP-" + entrenador),
                club.getReservas().buscarInstalacion(instalacion));
        try {
            e.definirGrupoAcademia(club.buscarDeporte(disciplina), nivel);
        } catch (IllegalArgumentException ex) {
            return ResultadoOperacion.rechazo(ex.getMessage());
        }
        return club.getOperaciones().registrarEntrenamiento(s, e);
    }

    public List<gestores.GestorReportes.AcumuladoJugador> estadisticas(Sesion s, String deporte, String competencia,
            LocalDate inicio, LocalDate fin) {
        if (!docente(s) && !club.getAcceso().tienePerfil(s, Perfil.SOCIO)) {
            throw new IllegalArgumentException("Sin permiso deportivo.");
        }

        if ((deporte != null && club.buscarDeporte(deporte) == null)
                || (competencia != null && club.getCompetencias().buscarCompetencia(competencia) == null)) {
            throw new IllegalArgumentException("Disciplina/competencia inexistentes.");
        }
        return club.getReportes().acumularEstadisticas(deporte, competencia, inicio, fin).stream()
                .filter(f -> docente(s) || f.jugadorId().equals(s.getUsuarioId())).toList();
    }

    public List<gestores.GestorReportes.AcumuladoJugador> clasificacion(Sesion s, String deporte, String competencia,
            TipoEstadistica metrica, LocalDate inicio, LocalDate fin) {
        if (metrica == null) {
            throw new IllegalArgumentException("Elija la métrica de clasificación.");
        }
        return estadisticas(s, deporte, competencia, inicio, fin).stream().sorted(Comparator
                .comparingInt((gestores.GestorReportes.AcumuladoJugador f) -> f.metricas().getOrDefault(metrica, 0))
                .reversed().thenComparing(gestores.GestorReportes.AcumuladoJugador::jugadorId)
                .thenComparing(gestores.GestorReportes.AcumuladoJugador::deporte)).toList();
    }

    public List<ActividadAgenda> agenda(Sesion s, LocalDate inicio, LocalDate fin) {
        gestores.GestorSocios.validarPeriodo(inicio, fin);
        if (!docente(s) && !club.getOperaciones().esCaja(s) && !club.getAcceso().tienePerfil(s, Perfil.SOCIO)) {
            throw new IllegalArgumentException("Sin permiso de agenda.");
        }
        return club.getAgenda().getActividades().stream()
                .filter(a -> !a.getInicio().toLocalDate().isBefore(inicio) && !a.getInicio().toLocalDate().isAfter(fin))
                .filter(a -> docente(s) || club.getOperaciones().esCaja(s)
                        || a.getUsuariosAgenda().contains(s.getUsuarioId()))
                .sorted(Comparator.comparing(ActividadAgenda::getInicio)).toList();
    }
}
