package gestores;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import modelo.usuarios.Usuario;

public class GestorAcceso {

    public enum Perfil {
        BASE, ADMINISTRADOR, EMPLEADO, SOCIO, ENTRENADOR, FISIOTERAPEUTA,
    }

    public static final class Sesion {

        private final String usuarioId;

        private Sesion(String id) {
            usuarioId = id;
        }

        public String getUsuarioId() {
            return usuarioId;
        }
    }

    public record Cuenta(String usuarioId, String login, Set<Perfil> perfiles, String sal, String hash,
            boolean activa) {
        public Cuenta {
            perfiles = Set.copyOf(perfiles);
            login = normalizarLogin(login);
        }

        public Cuenta(String usuarioId, Set<Perfil> perfiles, String sal, String hash) {
            this(usuarioId, usuarioId, perfiles, sal, hash, true);
        }
    }

    private static String normalizarLogin(String login) {
        if (login == null) {
            return "";
        } else {
            return login.trim().toLowerCase(Locale.ROOT);
        }
    }

    private Cuenta porLogin(String login) {
        String clave = normalizarLogin(login);
        return cuentas.values().stream().filter(c -> c.login().equals(clave)).findFirst().orElse(null);
    }

    public Set<Perfil> getPerfiles(Sesion sesion) {
        if (estaAutenticada(sesion)) {
            return cuentas.get(sesion.usuarioId).perfiles();
        } else {
            return Set.of();
        }
    }

    public boolean tienePerfilExplicito(Sesion sesion, Perfil perfil) {
        return getPerfiles(sesion).contains(perfil);
    }

    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private final Set<Sesion> sesiones = Collections.newSetFromMap(new IdentityHashMap<>());

    private static byte[] derivar(String clave, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, 210000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la clave.", e);
        } finally {
            spec.clearPassword();
        }
    }

    public boolean tieneAdministrador() {
        return cuentas.values().stream().anyMatch(c -> c.activa() && c.perfiles().contains(Perfil.ADMINISTRADOR));
    }

    public boolean puedeCrearAdministradorInicial() {
        return cuentas.values().stream().allMatch(c -> c.perfiles().equals(Set.of(Perfil.BASE)));
    }

    public boolean registrarCuentaBase(Usuario usuario, String login, String clave) {
        return agregar(usuario, login, clave, Set.of(Perfil.BASE));
    }

    public boolean crearAdministradorInicial(Usuario usuario, String clave) {
        if (usuario == null || !puedeCrearAdministradorInicial()) {
            return false;
        }
        return agregar(usuario, usuario.getId(), clave, Set.of(Perfil.ADMINISTRADOR));
    }

    public boolean registrarCuenta(Sesion administrador, Usuario usuario, String clave, Set<Perfil> perfiles) {
        String usuarioId;
        if (usuario == null) {
            usuarioId = null;
        } else {
            usuarioId = usuario.getId();
        }
        return registrarCuenta(administrador, usuario, usuarioId, clave, perfiles);
    }

    public boolean registrarCuenta(Sesion administrador, Usuario usuario, String login, String clave,
            Set<Perfil> perfiles) {
        return (tienePerfil(administrador, Perfil.ADMINISTRADOR) && agregar(usuario, login, clave, perfiles));
    }

    private boolean agregar(Usuario usuario, String login, String clave, Set<Perfil> perfiles) {
        if (usuario == null || !usuario.esIdentificado() || usuario.getId() == null || usuario.getId().isBlank()
                || clave == null || !usuario.estaActivo() || normalizarLogin(login).isBlank() || porLogin(login) != null
                || clave.length() < 8 || perfiles == null || perfiles.isEmpty()
                || perfiles.stream().anyMatch(Objects::isNull) || cuentas.containsKey(usuario.getId())) {
            return false;
        }
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        Cuenta cuenta = new Cuenta(usuario.getId(), login, perfiles, Base64.getEncoder().encodeToString(sal),
                Base64.getEncoder().encodeToString(derivar(clave, sal)), true);
        cuentas.put(usuario.getId(), cuenta);
        return true;
    }

    public Sesion autenticar(String id, String clave) {
        Cuenta cuenta = porLogin(id);
        if (cuenta == null || !cuenta.activa() || clave == null
                || !MessageDigest.isEqual(Base64.getDecoder().decode(cuenta.hash()),
                        derivar(clave, Base64.getDecoder().decode(cuenta.sal())))) {
            return null;
        }
        Sesion sesion = new Sesion(cuenta.usuarioId());
        sesiones.add(sesion);
        return sesion;
    }

    public boolean estaAutenticada(Sesion sesion) {
        return (sesion != null && sesiones.contains(sesion) && cuentas.containsKey(sesion.usuarioId)
                && cuentas.get(sesion.usuarioId).activa());
    }

    public boolean tienePerfil(Sesion sesion, Perfil perfil) {
        return estaAutenticada(sesion) && tienePerfil(sesion.usuarioId, perfil);
    }

    public boolean tienePerfil(String id, Perfil perfil) {
        Cuenta cuenta = cuentas.get(id);
        return (cuenta != null && cuenta.activa()
                && (cuenta.perfiles().contains(perfil)
                        || (perfil == Perfil.EMPLEADO && (cuenta.perfiles().contains(Perfil.ENTRENADOR)
                                || cuenta.perfiles().contains(Perfil.FISIOTERAPEUTA)))));
    }

    public boolean actualizarCuenta(Sesion administrador, String id, String login, Set<Perfil> perfiles) {
        Cuenta actual = cuentas.get(id), ocupada = porLogin(login);
        if (!tienePerfil(administrador, Perfil.ADMINISTRADOR) || actual == null || normalizarLogin(login).isBlank()
                || perfiles == null || perfiles.isEmpty() || perfiles.stream().anyMatch(Objects::isNull)
                || (ocupada != null && !ocupada.usuarioId().equals(id))
                || (actual.activa() && actual.perfiles().contains(Perfil.ADMINISTRADOR)
                        && !perfiles.contains(Perfil.ADMINISTRADOR) && cuentas.values().stream()
                                .filter(c -> c.activa() && c.perfiles().contains(Perfil.ADMINISTRADOR)).count() <= 1)) {
            return false;
        }
        cuentas.put(id, new Cuenta(id, login, perfiles, actual.sal(), actual.hash(), actual.activa()));
        sesiones.removeIf(s -> s.usuarioId.equals(id));
        return true;
    }

    public boolean cambiarEstadoCuenta(Sesion administrador, String id, boolean activa) {
        Cuenta cuenta = cuentas.get(id);
        if (!tienePerfil(administrador, Perfil.ADMINISTRADOR) || cuenta == null
                || (!activa && cuenta.perfiles().contains(Perfil.ADMINISTRADOR) && cuentas.values().stream()
                        .filter(c -> c.activa() && c.perfiles().contains(Perfil.ADMINISTRADOR)).count() <= 1)) {
            return false;
        }
        cuentas.put(id, new Cuenta(id, cuenta.login(), cuenta.perfiles(), cuenta.sal(), cuenta.hash(), activa));
        sesiones.removeIf(s -> s.usuarioId.equals(id));
        return true;
    }

    public boolean restablecerClave(Sesion administrador, String id, String clave) {
        Cuenta cuenta = cuentas.get(id);
        if (!tienePerfil(administrador, Perfil.ADMINISTRADOR) || cuenta == null || clave == null
                || clave.length() < 8) {
            return false;
        }
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        cuentas.put(id, new Cuenta(id, cuenta.login(), cuenta.perfiles(), Base64.getEncoder().encodeToString(sal),
                Base64.getEncoder().encodeToString(derivar(clave, sal)), cuenta.activa()));
        sesiones.removeIf(s -> s.usuarioId.equals(id));
        return true;
    }

    public void cerrarSesion(Sesion sesion) {
        sesiones.remove(sesion);
    }

    public List<Cuenta> getCuentas() {
        return new ArrayList<>(cuentas.values());
    }

    public void cargarCuenta(Cuenta cuenta) {
        if (cuenta == null || cuenta.usuarioId() == null || cuenta.usuarioId().isBlank() || cuenta.perfiles().isEmpty()
                || cuenta.login().isBlank() || porLogin(cuenta.login()) != null
                || cuentas.containsKey(cuenta.usuarioId()) || Base64.getDecoder().decode(cuenta.sal()).length != 16
                || Base64.getDecoder().decode(cuenta.hash()).length != 32) {
            throw new IllegalArgumentException("Cuenta inválida o duplicada.");
        }
        cuentas.put(cuenta.usuarioId(), cuenta);
    }
}
