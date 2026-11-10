package consola;

import aplicacion.EstadoClub;
import gestores.GestorAcceso.*;
import java.util.*;

public final class MenuPorRol {

    public record Opcion(String grupo, String etiqueta) {}

    private MenuPorRol() {}

    private static void agregar(Map<String, Opcion> opciones, String grupo, String... pares) {
        for (int i = 0; i < pares.length; i += 2) {
            opciones.put(pares[i], new Opcion(grupo, pares[i + 1]));
        }
    }

    public static Map<String, Opcion> opciones(EstadoClub club, Sesion sesion) {
        Map<String, Opcion> o = new LinkedHashMap<>();
        var acceso = club.getAcceso();

        if (!acceso.estaAutenticada(sesion)) {
            agregar(o, "Acceso", "13", "Iniciar sesión", "18", "Crear perfil base");

            if (acceso.puedeCrearAdministradorInicial()) {
                agregar(o, "Acceso", "15", "Crear administrador inicial");
            }

            agregar(o, "Acceso", "0", "Salir");
            return Collections.unmodifiableMap(o);
        }

        boolean admin = acceso.tienePerfil(sesion, Perfil.ADMINISTRADOR);
        boolean caja = admin || acceso.tienePerfilExplicito(sesion, Perfil.EMPLEADO);
        boolean entrenador = acceso.tienePerfil(sesion, Perfil.ENTRENADOR);
        boolean socio = acceso.tienePerfil(sesion, Perfil.SOCIO);
        boolean laboral = acceso.tienePerfil(sesion, Perfil.EMPLEADO);

        if (admin) {
            agregar(o, "Administración",
                    "1", "Registrar jugador",
                    "2", "Listar jugadores",
                    "3", "Registrar instalación",
                    "16", "Registrar empleado",
                    "17", "Registrar usuario",
                    "47", "Baja/reactivación de usuario",
                    "48", "Restablecer clave",
                    "56", "Actualizar login y perfiles de cuenta",
                    "57", "Asignar perfiles y habilitar cuentas");
        }

        if (caja || socio || entrenador) {
            agregar(o, "Instalaciones y deporte", "4", "Listar instalaciones");
        }

        if (caja) {
            agregar(o, "Instalaciones y deporte",
                    "5", "Registrar reserva para un socio",
                    "6", "Listar reservas");
        } else if (socio && !admin) {
            agregar(o, "Instalaciones y deporte",
                    "5", "Reservar para mí",
                    "6", "Mis reservas");
        }

        if (admin || entrenador) {
            agregar(o, "Instalaciones y deporte",
                    "32", "Consultar equipos",
                    "33", "Inscribir jugador en equipo",
                    "35", "Consultar entrenamientos",
                    "39", "Reportes");
        } else if (socio) {
            agregar(o, "Instalaciones y deporte",
                    "32", "Consultar equipos",
                    "33", "Inscribirme en equipo",
                    "35", "Mis entrenamientos",
                    "39", "Mi historial deportivo");
        }

        if (admin) {
            agregar(o, "Instalaciones y deporte",
                    "31", "Registrar equipo",
                    "49", "Registrar competencia");
        }

        if (admin || entrenador) {
            agregar(o, "Instalaciones y deporte",
                    "34", "Programar entrenamiento",
                    "36", "Convocar jugador",
                    "37", "Registrar asistencia y evaluación",
                    "38", "Registrar cambio de ranking",
                    "50", "Registrar nómina",
                    "51", "Programar partido",
                    "52", "Registrar resultado",
                    "53", "Integrar resultado a tabla de liga",
                    "54", "Registrar métrica de partido",
                    "55", "Registrar tarjeta",
                    "58", "Consultar jugadores habilitados",
                    "59", "Confirmar alineación de partido",
                    "60", "Cerrar acta y aplicar disciplina",
                    "61", "Cancelar partido",
                    "64", "Consultar historial disciplinario");
        }

        if (caja || socio || laboral) {
            agregar(o, "Operación comercial", "9", "Consultar catálogo e inventario");
        }

        if (admin) {
            agregar(o, "Operación comercial",
                    "8", "Registrar producto",
                    "19", "Registrar ubicación",
                    "20", "Trasladar producto",
                    "22", "Aprobar/rechazar sugerencia");
        }

        if (caja) {
            agregar(o, "Operación comercial",
                    "10", "Reabastecer producto",
                    "11", "Registrar venta",
                    "12", "Consultar ventas",
                    "29", "Registrar mensualidad");
        }

        if (caja) {
            agregar(o, "Operación comercial", "30", "Consultar mensualidades");
        } else if (socio) {
            agregar(o, "Operación comercial", "30", "Mis mensualidades");
        }

        if (laboral) {
            agregar(o, "Operación comercial",
                    "21", "Sugerir producto",
                    "23", "Mis sugerencias",
                    "24", "Completar alérgenos de sugerencia",
                    "28", "Consultar códigos de empleados");
        } else if (admin) {
            agregar(o, "Operación comercial",
                    "23", "Consultar sugerencias",
                    "28", "Consultar códigos de empleados");
        }

        if (laboral || caja) {
            agregar(o, "Ingreso y seguridad",
                    "25", "Registrar ingreso a instalación",
                    "26", "Registrar salida",
                    "27", "Confirmar consumo/entrega de bebida");
        } else if (socio && !admin) {
            agregar(o, "Ingreso y seguridad",
                    "25", "Ingresar a mi reserva",
                    "26", "Finalizar mi uso",
                    "27", "Confirmar mi consumo/entrega de bebida");
        }

        if (admin || entrenador) {
            agregar(o, "Instalaciones y deporte", "62", "Retirar jugador de equipo");
        } else if (socio) {
            agregar(o, "Instalaciones y deporte", "62", "Retirarme de equipo");
        }

        if (admin) {
            agregar(o, "Instalaciones y deporte", "63", "Baja/reactivación de equipo");
        }

        if (admin || entrenador || socio) {
            agregar(o, "Academia y torneos",
                    "65", "Academia: inscripción, niveles y partidos",
                    "66", "Torneos: participantes, fases y tablas",
                    "68", "Estadísticas acumuladas");
        }

        if (admin || entrenador || socio || caja) {
            agregar(o, "Agenda", "67", "Disponibilidad, agenda y cambios de horario");
        }

        if (admin) {
            agregar(o, "Torneos internos", "69", "Crear torneos y administrar cupos preferenciales");
        }

        if (admin) {
            agregar(o, "Personal",
                    "40", "Configurar horario de atención",
                    "41", "Registrar turno",
                    "45", "Resolver solicitud de turno",
                    "46", "Consultar cobertura laboral");
        }

        if (admin) {
            agregar(o, "Personal",
                    "42", "Consultar turnos",
                    "44", "Consultar solicitudes de turno");
        } else if (laboral) {
            agregar(o, "Personal",
                    "42", "Mis turnos",
                    "44", "Mis solicitudes de turno");
        }

        if (laboral) {
            agregar(o, "Personal", "43", "Solicitar cambio de turno");
        }

        agregar(o, "Sesión", "7", "Guardar", "14", "Cerrar sesión");
        agregar(o, "Sesión", "18", "Crear perfil base");
        agregar(o, "Sesión", "0", "Salir");
        return Collections.unmodifiableMap(o);
    }
}
