package producto_service.security;

/**
 * Nombres de rol reales emitidos por usuario-service en el claim "rol".
 *
 * El contrato del token es unico: ADMIN, EMPLEADO y CLIENTE. Spring Security
 * agrega el prefijo ROLE_ al construir las autoridades, por lo que las reglas
 * de autorizacion deben usar hasRole("ADMIN"), etc.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String EMPLEADO = "EMPLEADO";
    public static final String CLIENTE = "CLIENTE";

    private Roles() {
    }
}
