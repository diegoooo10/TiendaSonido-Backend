package producto_service.security;

/**
 * Nombres de rol aceptados por producto-service.
 *
 * Los roles objetivo del proyecto son ADMINISTRADOR, VENDEDOR y CLIENTE.
 * Mientras autenticacion-service siga emitiendo ADMIN y EMPLEADO, se aceptan
 * tambien esos nombres como compatibilidad temporal. Esta decision debe
 * coordinarse con el responsable de autenticacion antes de retirar el puente.
 */
public final class Roles {

    public static final String[] ADMIN = { "ADMINISTRADOR", "ADMIN" };
    public static final String[] GESTION = { "ADMINISTRADOR", "VENDEDOR", "ADMIN", "EMPLEADO" };
    public static final String[] CLIENTE = { "CLIENTE" };

    private Roles() {
    }
}
