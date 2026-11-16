package tienda_sonido.usuario_service.config;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import tienda_sonido.usuario_service.model.Rol;
import tienda_sonido.usuario_service.model.Usuario;
import tienda_sonido.usuario_service.repository.UsuarioRepository;

/**
 * Crea el administrador inicial de forma idempotente al arrancar la aplicacion.
 *
 * Reglas:
 * - Solo actua si no existe un usuario con el correo configurado.
 * - Nunca sobrescribe la contrasena ni el rol de un usuario existente.
 * - La contrasena se guarda siempre con el PasswordEncoder de la aplicacion.
 * - Si el RUN configurado ya pertenece a otro usuario, se usa un RUN
 *   alternativo para no impedir la creacion del administrador.
 * - No se registran credenciales en los logs.
 *
 * Los valores llegan por variables de entorno (ver application.properties y
 * .env.docker.example). Los predeterminados corresponden solo al entorno
 * academico local.
 */
@Component
public class AdminInicialInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private final boolean habilitado;
    private final String correo;
    private final String password;
    private final String nombre;
    private final String apellido;
    private final String run;
    private final String direccion;
    private final LocalDate fechaNacimiento;

    public AdminInicialInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.inicial.habilitado}") boolean habilitado,
            @Value("${admin.inicial.correo}") String correo,
            @Value("${admin.inicial.password}") String password,
            @Value("${admin.inicial.nombre}") String nombre,
            @Value("${admin.inicial.apellido}") String apellido,
            @Value("${admin.inicial.run}") String run,
            @Value("${admin.inicial.fecha-nacimiento}") String fechaNacimiento,
            @Value("${admin.inicial.direccion}") String direccion) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.habilitado = habilitado;
        this.correo = correo == null ? "" : correo.trim();
        this.password = password == null ? "" : password;
        this.nombre = nombre;
        this.apellido = apellido;
        this.run = run == null ? "" : run.trim();
        this.direccion = direccion;
        this.fechaNacimiento = parsearFecha(fechaNacimiento);
    }

    private LocalDate parsearFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return LocalDate.of(1990, 1, 1);
        }

        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException(
                    "admin.inicial.fecha-nacimiento debe tener formato ISO (yyyy-MM-dd)", ex);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            log.info("Creacion del administrador inicial deshabilitada (admin.inicial.habilitado=false)");
            return;
        }

        if (correo.isBlank() || password.isBlank() || run.isBlank()) {
            log.error("No se crea el administrador inicial: correo, contrasena o RUN sin configurar");
            return;
        }

        if (usuarioRepository.existsByCorreo(correo)) {
            log.info("El administrador inicial ya existe: no se crea ni se modifica");
            return;
        }

        // El RUN es obligatorio y unico, pero el administrador se identifica por
        // correo. Si el RUN configurado ya pertenece a otro usuario se usa un
        // sufijo aleatorio para no bloquear la creacion del administrador.
        String runAdministrador = run;
        if (usuarioRepository.existsByRun(runAdministrador)) {
            runAdministrador = run + "-" + UUID.randomUUID().toString().substring(0, 8);
            log.warn("El RUN configurado ya pertenece a otro usuario: se usa un RUN alternativo para el administrador inicial");
        }

        Usuario administrador = new Usuario();
        administrador.setCorreo(correo);
        administrador.setPassword(passwordEncoder.encode(password));
        administrador.setRol(Rol.ADMIN);
        administrador.setNombre(nombre);
        administrador.setApellido(apellido);
        administrador.setRun(runAdministrador);
        administrador.setFechaNacimiento(fechaNacimiento);
        administrador.setDireccion(direccion);

        try {
            usuarioRepository.save(administrador);
            log.info("Administrador inicial creado con rol ADMIN");
        } catch (DataIntegrityViolationException ex) {
            // Otra instancia pudo crearlo entre la comprobacion y el insert.
            log.warn("El administrador inicial ya estaba registrado: no se modifica");
        }
    }
}
