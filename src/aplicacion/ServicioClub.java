package aplicacion;

import gestores.GestorAcceso.*;
import java.time.*;
import java.util.*;
import modelo.competencias.*;
import modelo.deportes.*;
import modelo.entrenamientos.*;
import modelo.partidos.*;
import modelo.reservas.*;
import modelo.socios.PagoMensualidad;
import modelo.usuarios.*;

public class ServicioClub {

    private final EstadoClub club;

    public ServicioClub(EstadoClub club) {
        this.club = club;
    }

    public boolean esAdministrador(Sesion s) {
        return club.getAcceso().tienePerfil(s, Perfil.ADMINISTRADOR);
    }

    public boolean esCaja(Sesion s) {
        return (esAdministrador(s) || club.getAcceso().tienePerfilExplicito(s, Perfil.EMPLEADO));
    }

    public boolean esEntrenador(Sesion s) {
        return club.getAcceso().tienePerfil(s, Perfil.ENTRENADOR);
    }

    private boolean gestionaDeporte(Sesion s) {
        return esAdministrador(s) || esEntrenador(s);
    }

    private ResultadoOperacion resultado(boolean exito, String correcto, String rechazo) {
        if (exito) {
            return ResultadoOperacion.correcto(correcto);
        } else {
            return ResultadoOperacion.rechazo(rechazo);
        }
    }

    public ResultadoOperacion registrarJugador(Sesion s, String id, String nombre, LocalDate nacimiento) {
        if (!esAdministrador(s)) {
            return ResultadoOperacion.rechazo("Solo el administrador registra jugadores.");
        }
        try {
            club.registrarJugador(id, nombre, nacimiento);
            return ResultadoOperacion.correcto("Jugador registrado; habilite su cuenta con credenciales propias.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion registrarInstalacion(Sesion s, Instalacion instalacion) {
        if (!esAdministrador(s) || instalacion == null || instalacion.getDeporte() == null
                || club.buscarDeporte(instalacion.getDeporte().getId()) != instalacion.getDeporte()
                || instalacion.getId() == null || instalacion.getId().isBlank() || instalacion.getNombre() == null
                || instalacion.getNombre().isBlank() || instalacion.getCapacidadMaxima() <= 0) {
            return ResultadoOperacion.rechazo("Instalación inválida o sin permiso administrativo.");
        }
        return resultado(club.getReservas().registrarInstalacion(instalacion), "Instalación registrada.",
                "ID de instalación duplicado.");
    }

    public ResultadoOperacion reservar(Sesion s, String id, String jugadorId, String instalacionId,
            LocalDateTime inicio, LocalDateTime fin, int cantidad, String modalidad) {
        Jugador jugador = club.buscarJugador(jugadorId);
        boolean propio = club.getAcceso().estaAutenticada(s) && s.getUsuarioId().equals(jugadorId);
        if (jugador == null || !jugador.estaActivo() || !club.getAcceso().tienePerfil(jugadorId, Perfil.SOCIO)
                || club.getAcceso().tienePerfil(jugadorId, Perfil.ADMINISTRADOR)
                || (!esCaja(s) && !(propio && club.getAcceso().tienePerfil(s, Perfil.SOCIO)))) {
            return ResultadoOperacion.rechazo(
                    "Solo un socio activo puede reservar; el personal puede registrar para él. El administrador no reserva personalmente.");
        }
        return club.getReservas().registrarConMotivo(new Reserva(id, jugador,
                club.getReservas().buscarInstalacion(instalacionId), inicio, fin, cantidad, modalidad));
    }

    public List<Reserva> consultarReservas(Sesion s) {
        if (!club.getAcceso().estaAutenticada(s)) {
            return List.of();
        }
        return club.getReservas().getReservas().stream()
                .filter(r -> esCaja(s) || r.getJugador().getId().equals(s.getUsuarioId())).toList();
    }

    public ResultadoOperacion registrarEquipo(Sesion s, Equipo equipo) {
        if (!esAdministrador(s) || equipo == null || equipo.getDeporte() == null || !equipo.getDeporte().isColectivo()
                || club.buscarDeporte(equipo.getDeporte().getId()) != equipo.getDeporte() || equipo.getCupoMaximo() <= 0
                || equipo.getNombre() == null || equipo.getNombre().isBlank()
                || !Set.of("Sub-13", "Sub-15", "Sub-17", "Mayores").contains(equipo.getCategoria())) {
            return ResultadoOperacion.rechazo("Equipo inválido o sin permiso administrativo.");
        }
        return resultado(club.getEquipos().registrarEquipo(equipo), "Equipo registrado.", "Equipo duplicado.");
    }

    public ResultadoOperacion inscribir(Sesion s, String jugadorId, String equipoId, String posicion, int dorsal) {
        Jugador j = club.buscarJugador(jugadorId);
        Equipo e = club.getEquipos().buscarEquipo(equipoId);
        boolean propio = club.getAcceso().tienePerfil(s, Perfil.SOCIO) && s.getUsuarioId().equals(jugadorId);
        if ((!gestionaDeporte(s) && !propio) || j == null || !j.estaActivo()
                || club.getAcceso().tienePerfil(jugadorId, Perfil.ADMINISTRADOR)) {
            return ResultadoOperacion
                    .rechazo("Sin permiso o jugador inactivo/administrador no habilitado para participar.");
        }
        return club.getEquipos().inscribirConMotivo(j, e, posicion, dorsal, LocalDate.now(club.getReloj()));
    }

    public ResultadoOperacion registrarPago(Sesion s, PagoMensualidad pago) {
        if (!esCaja(s) || pago == null || pago.getJugador() == null
                || !club.getAcceso().tienePerfil(pago.getJugador().getId(), Perfil.SOCIO)) {
            return ResultadoOperacion.rechazo("Solo caja/administrador registra pagos de socios habilitados.");
        }
        return resultado(club.getSocios().registrarPagoMensualidad(pago), "Mensualidad registrada.",
                "Pago inválido: ID repetido, socio ajeno/inactivo, importe, fecha o periodo AAAA-MM incorrectos.");
    }

    public List<PagoMensualidad> consultarPagos(Sesion s) {
        if (!club.getAcceso().estaAutenticada(s)) {
            return List.of();
        }
        return club.getSocios().getPagosMensualidades().stream()
                .filter(p -> esCaja(s) || p.getJugador().getId().equals(s.getUsuarioId())).toList();
    }

    public ResultadoOperacion cambiarEstadoUsuario(Sesion admin, String id, boolean activo) {
        Usuario u = club.buscarUsuario(id);
        if (!esAdministrador(admin) || u == null || !u.esIdentificado()) {
            return ResultadoOperacion.rechazo("Usuario inexistente o sin permiso administrativo.");
        }

        if (!activo && actaPendienteDeJugador(id)) {
            return ResultadoOperacion.rechazo("Cierre las actas pendientes antes de dar de baja al jugador.");
        }
        boolean cuenta = club.getAcceso().getCuentas().stream().anyMatch(c -> c.usuarioId().equals(id));
        if (cuenta && !club.getAcceso().cambiarEstadoCuenta(admin, id, activo)) {
            return ResultadoOperacion.rechazo("No puede desactivar al último administrador activo.");
        }
        u.restaurarActivo(activo);
        String mensajeResultado;
        if (activo) {
            mensajeResultado = "Usuario reactivado; historia conservada.";
        } else {
            mensajeResultado = "Baja lógica registrada; sesiones invalidadas e historia conservada.";
        }
        return ResultadoOperacion.correcto(mensajeResultado);
    }

    public ResultadoOperacion actualizarRanking(Sesion s, String jugadorId, String disciplina, double valor,
            LocalDate fecha, String referencia) {
        Jugador j = club.buscarJugador(jugadorId);
        Deporte deporte = club.buscarDeporte(disciplina);
        if (!gestionaDeporte(s) || j == null || !j.estaActivo() || deporte == null) {
            return ResultadoOperacion.rechazo("Ranking sin jugador/deporte canónico o sin permiso deportivo.");
        }
        try {
            j.actualizarRanking(deporte.getId(), valor, fecha, referencia);
            return ResultadoOperacion.correcto("Ranking actualizado con movimiento histórico.");
        } catch (IllegalArgumentException e) {
            return ResultadoOperacion.rechazo(e.getMessage());
        }
    }

    public ResultadoOperacion registrarEntrenamiento(Sesion s, Entrenamiento e) {
        if (!gestionaDeporte(s) || e == null || e.getEntrenador() == null || e.getInstalacion() == null
                || e.getInicio() == null || e.getDuracionMinutos() < 1
                || club.getVentas().buscarEmpleadoPorCodigo(e.getEntrenador().getCodigoDescuento()) != e.getEntrenador()
                || !club.getAcceso().tienePerfil(e.getEntrenador().getId(), Perfil.ENTRENADOR)
                || (!esAdministrador(s) && !s.getUsuarioId().equals(e.getEntrenador().getId()))
                || club.getReservas().buscarInstalacion(e.getInstalacion().getId()) != e.getInstalacion()) {
            return ResultadoOperacion.rechazo("Entrenador, instalación o permiso inválidos.");
        }

        if (e.getEquipo() != null) {
            if (club.getEquipos().buscarEquipo(e.getEquipo().getId()) != e.getEquipo() || !e.getEquipo().estaActivo()
                    || e.getEquipo().getDeporte() != e.getInstalacion().getDeporte()) {
                return ResultadoOperacion.rechazo("Equipo incompatible o dado de baja.");
            }
        } else if (e.getDisciplina() == null || e.getNivelGrupo() == null || e.getDisciplina().isColectivo()
                || e.getInstalacion().getDeporte() != e.getDisciplina()) {
            return ResultadoOperacion.rechazo("Defina disciplina individual y nivel del grupo.");
        }
        String causa = club.getAgenda().validar(e);
        if (causa != null) {
            return ResultadoOperacion.rechazo(causa);
        }
        return resultado(club.getEntrenamientos().registrarEntrenamiento(e), "Entrenamiento registrado.",
                "Entrenamiento duplicado o cruce de instalación/entrenador.");
    }

    private boolean responsable(Sesion s, Entrenamiento e) {
        return (e != null && (e.getEquipo() == null || e.getEquipo().estaActivo())
                && (esAdministrador(s) || (esEntrenador(s) && e.getEntrenador().getId().equals(s.getUsuarioId()))));
    }

    public ResultadoOperacion convocar(Sesion s, String entrenamientoId, String jugadorId) {
        Entrenamiento e = club.getEntrenamientos().buscarEntrenamiento(entrenamientoId);
        Jugador j = club.buscarJugador(jugadorId);
        if (!responsable(s, e) || e.estaCancelada() || j == null || !j.estaActivo()
                || club.getAcceso().tienePerfil(jugadorId, Perfil.ADMINISTRADOR)) {
            return ResultadoOperacion.rechazo("Sin permiso o participante inactivo.");
        }

        if (e.getEquipo() != null && !e.getEquipo().contieneJugador(j)) {
            return ResultadoOperacion.rechazo("Jugador ajeno al equipo.");
        }

        if (e.getEquipo() == null) {
            InscripcionDisciplina i = club.getAcademias().buscar(jugadorId, e.getDisciplina().getId());
            if (i == null || !i.estaActiva() || i.getNivel() != e.getNivelGrupo()) {
                return ResultadoOperacion.rechazo("Socio sin inscripción/nivel del grupo.");
            }
        }
        java.util.Set<String> usuarios = new java.util.HashSet<>(e.getUsuariosAgenda());
        usuarios.add(jugadorId);
        String causa = club.getAgenda().validar(e, e.getInstalacion(), e.getInicio(), e.getFin(), usuarios);
        if (causa != null) {
            return ResultadoOperacion.rechazo(causa);
        }

        if (e.getConvocados().size() >= e.getInstalacion().getCapacidadMaxima()) {
            return ResultadoOperacion.rechazo("Cupo del grupo completo.");
        }
        return resultado(e.convocarJugador(j), "Convocatoria registrada.",
                "Convocatoria repetida o actividad cancelada.");
    }

    public ResultadoOperacion registrarAsistencia(Sesion s, String id, String jugadorId, EstadoAsistencia estado,
            Double nota, String comentario) {
        Entrenamiento e = club.getEntrenamientos().buscarEntrenamiento(id);
        Jugador j = club.buscarJugador(jugadorId);
        return resultado(
                responsable(s, e) && j != null
                        && e.registrarAsistencia(new AsistenciaEvaluacion(j, e, estado, nota, comentario)),
                "Asistencia registrada.",
                "Sin permiso, jugador no convocado, registro repetido o nota no finita/fuera de 1 a 5.");
    }

    public ResultadoOperacion registrarCompetencia(Sesion s, Competencia c) {
        return resultado(
                esAdministrador(s) && c != null && c.getDeporte() != null
                        && club.buscarDeporte(c.getDeporte().getId()) == c.getDeporte()
                        && club.getCompetencias().registrarCompetencia(c),
                "Competencia registrada.", "Competencia inválida/duplicada o sin permiso.");
    }

    public ResultadoOperacion registrarNomina(Sesion s, String competenciaId, String equipoId, List<String> jugadores) {
        Competencia c = club.getCompetencias().buscarCompetencia(competenciaId);
        Equipo e = club.getEquipos().buscarEquipo(equipoId);
        if (!gestionaDeporte(s) || c == null || e == null || jugadores == null) {
            return ResultadoOperacion.rechazo("Nómina sin referencias válidas o permiso deportivo.");
        }
        NominaCompetencia n = new NominaCompetencia(c, e);
        for (String id : jugadores) {
            Jugador j = club.buscarJugador(id);
            if (j == null || !j.estaActivo() || club.getAcceso().tienePerfil(id, Perfil.ADMINISTRADOR)
                    || !n.agregarJugador(j)) {
                return ResultadoOperacion.rechazo("Nómina contiene un jugador ajeno, repetido o inactivo.");
            }
        }
        int minimo = c.getDeporte().getMinimoJugadores();
        if (!e.estaActivo() || minimo == 0
                || club.getHabilitacion().contarHabilitadosNomina(n, LocalDateTime.now(club.getReloj())) < minimo) {
            return ResultadoOperacion
                    .rechazo("Nómina insuficiente: requiere " + minimo + " jugadores habilitados en el equipo.");
        }
        return resultado(club.getCompetencias().registrarNomina(n), "Nómina registrada.",
                "Nómina duplicada o incompatible.");
    }

    public ResultadoOperacion registrarPartido(Sesion s, Partido p) {
        if (p instanceof PartidoAcademia academia) {
            return club.getAcademias().registrar(s, academia);
        }
        boolean valido = gestionaDeporte(s) && p != null && p.getDeporte() != null
                && club.buscarDeporte(p.getDeporte().getId()) == p.getDeporte();
        if (valido && p instanceof PartidoInterno i) {
            String equipoLocalId;
            if (i.getLocal() == null) {
                equipoLocalId = null;
            } else {
                equipoLocalId = i.getLocal().getId();
            }
            valido = club.getEquipos().buscarEquipo(equipoLocalId) == i.getLocal() && i.getLocal() != null
                    && i.getLocal().estaActivo() && i.getVisitante() != null && i.getVisitante().estaActivo()
                    && club.getEquipos().buscarEquipo(i.getVisitante().getId()) == i.getVisitante()
                    && i.sonEquiposCompatibles();
        } else if (valido && p instanceof PartidoCompetencia c) {
            valido = c.getCompetencia() != null && c.getEquipoClub() != null
                    && club.getCompetencias().buscarCompetencia(c.getCompetencia().getId()) == c.getCompetencia()
                    && club.getEquipos().buscarEquipo(c.getEquipoClub().getId()) == c.getEquipoClub()
                    && c.getEquipoClub().estaActivo() && c.getInstitucionRival() != null
                    && !c.getInstitucionRival().isBlank() && c.getEscenario() != null && !c.getEscenario().isBlank()
                    && club.getCompetencias().buscarNomina(c.getCompetencia().getId(),
                            c.getEquipoClub().getId()) != null;
        } else {
            valido = false;
        }

        if (valido && p instanceof PartidoCompetencia pc && pc.getCompetencia().getFormato() instanceof FormatoLiga f) {
            ParticipanteCompetencia propio = f.buscarParticipante(pc.getEquipoClub().getId());
            if (propio == null || !propio.activo()) {
                return ResultadoOperacion.rechazo("Equipo no inscrito o retirado del torneo.");
            }

            if (club.getPartidos().getPartidos().stream()
                    .anyMatch(p0 -> p0 instanceof PartidoCompetencia otro && !otro.estaCancelado()
                            && otro.getCompetencia() == pc.getCompetencia()
                            && otro.getEncuentroTorneoId().equals(pc.getEncuentroTorneoId()))) {
                return ResultadoOperacion.rechazo("El cruce ya tiene partido programado.");
            }
            ParticipanteCompetencia rival = f.buscarParticipante(pc.getRivalId());
            if (rival != null) {
                Equipo equipoRival;
                if (rival.equipoClubId() == null) {
                    equipoRival = null;
                } else {
                    equipoRival = club.getEquipos().buscarEquipo(rival.equipoClubId());
                }

                if (!rival.activo() || (equipoRival != null && (!equipoRival.estaActivo() || club.getCompetencias()
                        .buscarNomina(pc.getCompetencia().getId(), equipoRival.getId()) == null))) {
                    return ResultadoOperacion.rechazo("Rival retirado o sin nómina.");
                }

                if (!pc.vincularEncuentro(pc.getEncuentroTorneoId(), rival.id(), rival.nombre(), equipoRival)) {
                    return ResultadoOperacion.rechazo("Rival incompatible.");
                }
            } else if (f instanceof FormatoFases) {
                return ResultadoOperacion.rechazo("El rival debe estar inscrito en las fases.");
            }
            EncuentroTorneo encuentro = f.buscarEncuentro(pc.getEncuentroTorneoId());
            if (f instanceof FormatoFases && encuentro == null) {
                return ResultadoOperacion.rechazo("Seleccione un cruce generado por las fases.");
            }

            if (encuentro != null && (encuentro.completo() || (!(encuentro.localId().equals(pc.getEquipoClub().getId())
                    && encuentro.visitanteId().equals(pc.getRivalId()))
                    && !(encuentro.visitanteId().equals(pc.getEquipoClub().getId())
                            && encuentro.localId().equals(pc.getRivalId()))))) {
                return ResultadoOperacion.rechazo("Cruce incompatible o con resultado previo.");
            }
        }

        if (valido) {
            String causa = club.getAgenda().validar(p);
            if (causa != null) {
                return ResultadoOperacion.rechazo(causa);
            }
        }
        return resultado(valido && club.getPartidos().registrarPartido(p), "Partido registrado.",
                "Partido duplicado, no canónico, incompatible o sin permiso.");
    }

    public ResultadoOperacion registrarResultado(Sesion s, String partidoId, int local, int visitante) {
        if (!gestionaDeporte(s)) {
            return ResultadoOperacion.rechazo("Solo entrenador/administrador registra resultados.");
        }
        Partido p = club.getPartidos().buscarPartido(partidoId);
        boolean exito = false;
        if (p instanceof PartidoAcademia) {
            return club.getAcademias().registrarResultado(s, partidoId, local, visitante);
        }
        ResultadoOperacion validacion = club.getHabilitacion().validarResultado(p);
        if (!validacion.exito()) {
            return validacion;
        }

        if (p instanceof PartidoInterno i) {
            exito = i.registrarResultado(local, visitante);
        } else if (p instanceof PartidoCompetencia c) {
            exito = c.registrarResultado(local, visitante,
                    club.getCompetencias().buscarNomina(c.getCompetencia().getId(), c.getEquipoClub().getId()));
        }

        if (exito) {
            club.getHabilitacion().confirmarPlantillas(p);
        }
        return resultado(exito, "Resultado registrado; registre las incidencias y cierre el acta.",
                "Resultado inválido, repetido o sin mínimo de jugadores/nómina.");
    }

    public gestores.GestorReportes.ResumenFinanciero consultarFinanzas(Sesion s, String rubro, LocalDate inicio,
            LocalDate fin) {
        if (!esAdministrador(s)) {
            throw new IllegalArgumentException("Solo el administrador consulta el reporte financiero integrado.");
        }
        return club.getReportes().consultarDesgloseFinanciero(rubro, inicio, fin);
    }

    public ResultadoOperacion procesarTablaLiga(Sesion s, String id) {
        Partido p = club.getPartidos().buscarPartido(id);
        return resultado(
                gestionaDeporte(s) && p instanceof PartidoCompetencia pc && (pc.estaCerrado() || pc.esLegado())
                        && pc.getCompetencia().getFormato() instanceof FormatoLiga f && f.procesarResultado(pc),
                "Resultado integrado a la tabla.",
                "Sin permiso, resultado pendiente, ya procesado o formato/deporte todavía no soportado.");
    }

    private boolean participa(Partido p, Jugador j) {
        if (p instanceof PartidoAcademia pa) {
            return (pa.esParticipante(j) && club.getAcademias().validarParticipantes(pa).exito());
        }

        if (!p.getSelecciones().isEmpty()) {
            return p.esParticipante(j);
        }

        if (p instanceof PartidoCompetencia) {
            return false;
        }
        for (Equipo equipo : p.getEquiposClub()) {
            if (equipo.contieneJugador(j) && club.getHabilitacion().motivo(p, equipo, j) == null) {
                return true;
            }
        }
        return false;
    }

    public ResultadoOperacion registrarMetrica(Sesion s, String partidoId, String jugadorId, TipoEstadistica tipo,
            int valor) {
        Partido p = club.getPartidos().buscarPartido(partidoId);
        Jugador j = club.buscarJugador(jugadorId);
        if (!gestionaDeporte(s) || p == null || p.estaCerrado() || p.estaCancelado() || j == null || !participa(p, j)) {
            return ResultadoOperacion.rechazo("Sin permiso o jugador ajeno al partido/nómina.");
        }

        if (p instanceof PartidoAcademia && tipo == TipoEstadistica.SETS) {
            return ResultadoOperacion.rechazo("Los sets de academia se generan con el resultado.");
        }
        EstadisticaPartido e = p.getEstadisticas().stream().filter(a -> a.getJugador() == j).findFirst().orElse(null);
        if (e == null) {
            e = new EstadisticaPartido(j, p);
            return resultado(e.registrarMetrica(tipo, valor) && p.registrarEstadistica(e), "Métrica registrada.",
                    "Métrica incompatible o repetida.");
        }
        return resultado(e.registrarMetrica(tipo, valor), "Métrica registrada.", "Métrica incompatible o repetida.");
    }

    public ResultadoOperacion registrarTarjeta(Sesion s, String partidoId, String jugadorId, TipoTarjeta tipo) {
        Partido p = club.getPartidos().buscarPartido(partidoId);
        Jugador j = club.buscarJugador(jugadorId);
        return resultado(
                gestionaDeporte(s) && p != null && j != null && participa(p, j)
                        && p.registrarSancion(new Sancion(j, p, tipo)),
                "Tarjeta registrada.", "Sin permiso, participante ajeno, deporte o tarjeta incompatibles.");
    }

    public ResultadoOperacion alinear(Sesion sesion, String partidoId, String equipoId, List<String> jugadores) {
        return club.getHabilitacion().alinear(sesion, partidoId, equipoId, jugadores);
    }

    public ResultadoOperacion cerrarActa(Sesion sesion, String partidoId) {
        Partido partido = club.getPartidos().buscarPartido(partidoId);
        if (partido instanceof PartidoCompetencia p && p.getCompetencia().getFormato() instanceof FormatoFases f
                && p.tieneResultado() && p.getMarcadorClub().equals(p.getMarcadorRival())
                && f.buscarEncuentro(p.getEncuentroTorneoId()) != null
                && f.buscarEncuentro(p.getEncuentroTorneoId()).fase().startsWith("ELIMINACION")
                && p.getGanadorDesempateId() == null) {
            return ResultadoOperacion.rechazo("Defina ganador de desempate antes de cerrar la eliminatoria.");
        }
        return club.getHabilitacion().cerrar(sesion, partidoId);
    }

    public ResultadoOperacion cancelarPartido(Sesion sesion, String partidoId) {
        return club.getHabilitacion().cancelar(sesion, partidoId);
    }

    public ResultadoOperacion retirarJugador(Sesion sesion, String jugadorId, String equipoId, String motivo) {
        boolean propio = club.getAcceso().tienePerfil(sesion, Perfil.SOCIO) && sesion.getUsuarioId().equals(jugadorId);
        if (!gestionaDeporte(sesion) && !propio) {
            return ResultadoOperacion.rechazo("Sin permiso para retirar a ese jugador.");
        }

        if (actaPendienteDeJugador(jugadorId)) {
            return ResultadoOperacion.rechazo("Cierre las actas pendientes del jugador antes del retiro.");
        }
        return club.getEquipos().retirarJugador(club.buscarJugador(jugadorId), club.getEquipos().buscarEquipo(equipoId),
                motivo);
    }

    public ResultadoOperacion cambiarEstadoEquipo(Sesion sesion, String equipoId, boolean activo) {
        Equipo equipo = club.getEquipos().buscarEquipo(equipoId);
        if (!esAdministrador(sesion) || equipo == null) {
            return ResultadoOperacion.rechazo("Equipo inexistente o sin permiso administrativo.");
        }

        if (!activo) {
            for (Partido p : club.getPartidos().getPartidos()) {
                if (p.getEquiposClub().contains(equipo) && p.tieneResultado() && !p.estaCerrado() && !p.esLegado()) {
                    return ResultadoOperacion.rechazo("Cierre primero el acta pendiente " + p.getId() + ".");
                }
            }
            for (InscripcionEquipo i : equipo.getInscripciones()) {
                i.retirar("Baja administrativa del equipo");
            }
            for (Partido p : club.getPartidos().getPartidos()) {
                if (p.getEquiposClub().contains(equipo) && !p.tieneResultado() && !p.estaCancelado()) {
                    club.getAgenda().cambiar(sesion, p, p.getInstalacion(), p.getInicio(), p.getFin(),
                            "Baja del equipo", true);
                }
            }
            for (Competencia competencia : club.getCompetencias().getCompetencias()) {
                if (competencia.getFormato() instanceof FormatoLiga f) {
                    f.cambiarActivo(equipo.getId(), false);
                }
            }
        }
        equipo.restaurarActivo(activo);
        String mensajeResultado2;
        if (activo) {
            mensajeResultado2 = "Equipo reactivado; las inscripciones previas siguen retiradas.";
        } else {
            mensajeResultado2 = "Equipo dado de baja; retiros y cancelaciones registrados sin borrar historia.";
        }
        return ResultadoOperacion.correcto(mensajeResultado2);
    }

    private boolean actaPendienteDeJugador(String jugadorId) {
        return club.getPartidos().getPartidos().stream()
                .anyMatch(p -> p.tieneResultado() && !p.estaCerrado() && !p.esLegado() && p.getPlantillas().values()
                        .stream().flatMap(List::stream).anyMatch(j -> j.getId().equals(jugadorId)));
    }
}
