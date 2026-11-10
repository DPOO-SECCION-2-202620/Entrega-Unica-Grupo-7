package consola;

import aplicacion.EstadoClub;
import aplicacion.ResultadoOperacion;
import gestores.GestorAcceso.Sesion;
import gestores.GestorTorneos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import modelo.deportes.Deporte;
import modelo.deportes.NivelAcademia;
import modelo.torneos.*;

public class ConsolaTorneosInternos {

    private final EstadoClub club;
    private final Sesion sesion;
    private final Scanner entrada;
    private final GestorTorneos gestor;

    public ConsolaTorneosInternos(EstadoClub club, Sesion sesion, Scanner entrada) {
        this.club = club;
        this.sesion = sesion;
        this.entrada = entrada;
        this.gestor = club.getTorneos();
    }

    private String leer(String mensaje) {
        System.out.print(mensaje + ": ");
        if (!entrada.hasNextLine()) {
            throw new NoSuchElementException();
        }
        String valor = entrada.nextLine().trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El campo es obligatorio.");
        }
        return valor;
    }

    private int leerCupos() {
        try {
            int cupos = Integer.parseInt(leer("Cupos totales, contados por equipo/jugador/pareja"));
            if (cupos > 0) {
                return cupos;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Los cupos deben ser un número entero positivo.");
        }
        throw new IllegalArgumentException("Los cupos deben ser un número entero positivo.");
    }

    private List<String> jugadores(String mensaje) {
        return Arrays.stream(leer(mensaje).split(",", -1)).map(String::trim).toList();
    }

    private TorneoInterno seleccionar() {
        TorneoInterno torneo = gestor.buscar(leer("ID del torneo interno"));
        if (torneo == null) {
            throw new IllegalArgumentException("El torneo no existe.");
        }
        return torneo;
    }

    public boolean ejecutar() {
        gestor.consultar(sesion);
        System.out.println("1 Crear torneo | 2 Asignar clasificación preferencial | 3 Liberar preferenciales");
        System.out.println(
                "4 Consultar detalle y cupos | 5 Registrar inscripción administrativa | 6 Retirar inscripción | 7 Listar torneos | 8 Clasificar externos | 9 Inscribir empleado");
        ResultadoOperacion resultado;
        switch (leer("Acción de torneos internos")) {
        case "1" -> resultado = crear();
        case "2" -> resultado = clasificar();
        case "3" -> resultado = gestor.liberarPreferenciales(sesion, seleccionar().getId());
        case "4" -> {
            mostrar(seleccionar());
            return false;
        }
        case "5" -> resultado = inscribir();
        case "6" -> resultado = gestor.retirar(sesion, seleccionar().getId(), leer("ID del participante inscrito"));
        case "7" -> {
            List<TorneoInterno> torneos = gestor.consultar(sesion);
            if (torneos.isEmpty()) {
                System.out.println("No hay torneos internos registrados.");
            }
            torneos.forEach(this::mostrar);
            return false;
        }
        case "8" -> resultado = clasificarExternos();
        case "9" -> resultado = inscribirEmpleado();
        default -> throw new IllegalArgumentException("Acción de torneo desconocida.");
        }
        System.out.println(resultado.mensaje());
        return resultado.exito();
    }

    private ResultadoOperacion crear() {
        String id = leer("ID del torneo");
        String nombre = leer("Nombre");
        Deporte deporte = club.buscarDeporte(leer("Deporte o disciplina"));
        if (deporte == null) {
            throw new IllegalArgumentException("Deporte desconocido.");
        }
        String categoria = null;
        NivelAcademia nivel = null;
        ModalidadTorneo modalidad;
        if (deporte.isColectivo()) {
            categoria = leer("Categoría Sub-13/Sub-15/Sub-17/Mayores");
            modalidad = ModalidadTorneo.EQUIPOS;
        } else {
            nivel = NivelAcademia.valueOf(leer("Nivel PRINCIPIANTE/INTERMEDIO/AVANZADO").toUpperCase(Locale.ROOT));
            modalidad = ModalidadTorneo.valueOf(leer("Modalidad SENCILLOS/DOBLES").toUpperCase(Locale.ROOT));
        }
        int cupos = leerCupos();
        LocalDateTime inicio = LocalDateTime.parse(leer("Inicio AAAA-MM-DDTHH:MM"));
        String fecha = leer("Liberación permitida desde AAAA-MM-DDTHH:MM, o 0 desde ahora");
        LocalDateTime liberacion;
        if (fecha.equals("0")) {
            liberacion = null;
        } else {
            liberacion = LocalDateTime.parse(fecha);
        }
        TipoTorneo tipo = TipoTorneo.valueOf(leer("Tipo RECREATIVO/COMPETITIVO").toUpperCase(Locale.ROOT));
        BigDecimal tarifa;
        if (tipo == TipoTorneo.COMPETITIVO) {
            tarifa = new BigDecimal(leer("Tarifa de inscripción"));
        } else {
            tarifa = BigDecimal.ZERO;
        }
        return gestor.crear(sesion, id, nombre, deporte.getId(), categoria, nivel, modalidad, cupos, inicio, liberacion,
                tipo, tarifa);
    }

    private ResultadoOperacion clasificar() {
        TorneoInterno torneo = seleccionar();
        if (torneo.getModalidad() == ModalidadTorneo.SENCILLOS) {
            return gestor.clasificarJugadores(sesion, torneo.getId());
        }

        if (torneo.getModalidad() == ModalidadTorneo.DOBLES) {
            List<List<String>> parejas = new ArrayList<>();
            for (String pareja : leer("Parejas con IDs: J1,J2;J3,J4").split(";", -1)) {
                parejas.add(Arrays.stream(pareja.split(",", -1)).map(String::trim).toList());
            }
            return gestor.clasificarParejas(sesion, torneo.getId(), parejas);
        }
        String criterio = leer("Criterio y origen del desempeño de los equipos");
        Map<String, Double> puntajes = new LinkedHashMap<>();
        for (String dato : leer("Todos los equipos habilitados de la categoría: EQUIPO1=20,EQUIPO2=15").split(",",
                -1)) {
            String[] partes = dato.split("=", -1);
            if (partes.length != 2
                    || puntajes.putIfAbsent(partes[0].trim(), Double.parseDouble(partes[1].trim())) != null) {
                throw new IllegalArgumentException("Use un puntaje por equipo, sin repetir IDs.");
            }
        }
        return gestor.clasificarEquipos(sesion, torneo.getId(), criterio, puntajes);
    }

    private ResultadoOperacion clasificarExternos() {
        TorneoInterno torneo = seleccionar();
        Map<String, Double> puntajes = new LinkedHashMap<>();
        for (String dato : leer("Participantes externos y desempeño: ID1=20,ID2=15").split(",", -1)) {
            String[] partes = dato.split("=", -1);
            if (partes.length != 2
                    || puntajes.putIfAbsent(partes[0].trim(), Double.parseDouble(partes[1].trim())) != null) {
                throw new IllegalArgumentException("Use un puntaje válido por participante externo, sin repetir IDs.");
            }
        }
        return gestor.clasificarParticipantesExternos(sesion, torneo.getId(), puntajes);
    }

    private ResultadoOperacion inscribirEmpleado() {
        TorneoInterno torneo = seleccionar();
        return gestor.inscribirEmpleado(sesion, torneo.getId(), List.of(leer("ID del empleado")), null,
                TipoCupo.REGULAR);
    }

    private ResultadoOperacion inscribir() {
        TorneoInterno torneo = seleccionar();
        List<String> jugadores = null;
        String equipo = null;
        if (torneo.getModalidad() == ModalidadTorneo.EQUIPOS) {
            equipo = leer("ID del equipo");
        } else {
            jugadores = jugadores("IDs de jugadores separados por coma");
        }
        TipoCupo cupo = TipoCupo.valueOf(leer("Cupo REGULAR/PREFERENCIAL").toUpperCase(Locale.ROOT));
        return gestor.inscribir(sesion, torneo.getId(), jugadores, equipo, cupo);
    }

    private void mostrar(TorneoInterno torneo) {
        var d = torneo.getDisponibilidad();
        boolean abierto = torneo.admiteInscripciones(LocalDateTime.now(club.getReloj()));
        String estadoInscripciones;
        if (abierto) {
            estadoInscripciones = "abiertas";
        } else {
            estadoInscripciones = "cerradas";
        }
        String categoriaONivel;
        if (torneo.getCategoria() == null) {
            categoriaONivel = torneo.getNivel().toString();
        } else {
            categoriaONivel = torneo.getCategoria();
        }
        System.out.println(torneo.getId() + " | " + torneo.getNombre() + " | " + torneo.getDeporte().getNombre() + " | "
                + categoriaONivel + " | " + torneo.getModalidad() + " | " + torneo.getTipo() + " | tarifa: "
                + torneo.getTarifa() + " | inicio: " + torneo.getInicio() + " | inscripciones " + estadoInscripciones);
        System.out.println("Preferenciales: " + d.preferencialesOcupados() + " ocupados, "
                + d.preferencialesDisponibles() + " disponibles | Regulares: " + d.regularesOcupados() + " ocupados, "
                + d.regularesDisponibles() + " disponibles");
        System.out.println("Liberación desde: " + torneo.getFechaLiberacion() + " | aplicada: "
                + torneo.estanPreferencialesLiberados());
        String criterioMostrado;
        if (torneo.getCriterio().isBlank()) {
            criterioMostrado = "sin clasificación asignada";
        } else {
            criterioMostrado = torneo.getCriterio();
        }
        System.out.println("Criterio: " + criterioMostrado);
        torneo.getClasificacion()
                .forEach(c -> System.out.println(c.id() + " | " + c.nombre() + " | puntaje: " + c.puntaje()));
        System.out.println("Elegibles preferenciales: "
                + torneo.getElegiblesPreferenciales().stream().map(CandidatoTorneo::id).toList());
        torneo.getInscripciones().forEach(i -> System.out
                .println(i.id() + " | " + i.participante().id() + " | " + i.cupo() + " | activa: " + i.activa()));
    }
}
