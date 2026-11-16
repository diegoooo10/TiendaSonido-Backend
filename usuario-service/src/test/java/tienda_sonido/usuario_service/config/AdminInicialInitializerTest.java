package tienda_sonido.usuario_service.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import tienda_sonido.usuario_service.model.Rol;
import tienda_sonido.usuario_service.model.Usuario;
import tienda_sonido.usuario_service.repository.UsuarioRepository;

class AdminInicialInitializerTest {

    private static final String CORREO = "admin@duocuc.cl";
    private static final String PASSWORD = "admin";

    @Mock UsuarioRepository repository;
    @Mock PasswordEncoder encoder;

    private AdminInicialInitializer inicializador(boolean habilitado,
                                                  String correo,
                                                  String password,
                                                  String run) {
        return new AdminInicialInitializer(
                repository,
                encoder,
                habilitado,
                correo,
                password,
                "Administrador",
                "Sonido Vivo",
                run,
                "1990-01-01",
                "Sin direccion registrada");
    }

    private AdminInicialInitializer inicializador() {
        return inicializador(true, CORREO, PASSWORD, "11111111-1");
    }

    @BeforeEach
    void preparar() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("crea el administrador con rol ADMIN y la contrasena cifrada")
    void creaAdministradorCuandoNoExiste() {
        when(encoder.encode(PASSWORD)).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        inicializador().run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());

        Usuario creado = captor.getValue();
        assertEquals(CORREO, creado.getCorreo());
        assertEquals(Rol.ADMIN, creado.getRol());
        assertEquals("hash-bcrypt", creado.getPassword());
        assertNotEquals(PASSWORD, creado.getPassword());
        assertEquals("11111111-1", creado.getRun());
        assertEquals("Administrador", creado.getNombre());
        verify(encoder).encode(PASSWORD);
    }

    @Test
    @DisplayName("no crea ni sobrescribe nada cuando el administrador ya existe")
    void noCreaNiSobrescribeCuandoYaExiste() {
        when(repository.existsByCorreo(CORREO)).thenReturn(true);

        inicializador().run(null);

        verify(repository, never()).save(any(Usuario.class));
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("es idempotente en ejecuciones sucesivas")
    void esIdempotenteEnEjecucionesSucesivas() {
        AtomicBoolean creado = new AtomicBoolean(false);

        when(repository.existsByCorreo(CORREO)).thenAnswer(inv -> creado.get());
        when(encoder.encode(PASSWORD)).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            creado.set(true);
            return inv.getArgument(0);
        });

        AdminInicialInitializer servicio = inicializador();
        servicio.run(null);
        servicio.run(null);
        servicio.run(null);

        verify(repository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("no crea el administrador cuando la funcionalidad esta deshabilitada")
    void noCreaCuandoEstaDeshabilitado() {
        inicializador(false, CORREO, PASSWORD, "11111111-1").run(null);

        verify(repository, never()).save(any(Usuario.class));
        verify(repository, never()).existsByCorreo(anyString());
    }

    @Test
    @DisplayName("crea el administrador con un RUN alternativo si el configurado ya existe")
    void usaRunAlternativoCuandoElConfiguradoYaExiste() {
        when(repository.existsByCorreo(CORREO)).thenReturn(false);
        when(repository.existsByRun("11111111-1")).thenReturn(true);
        when(encoder.encode(PASSWORD)).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        inicializador().run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertEquals(CORREO, captor.getValue().getCorreo());
        assertNotEquals("11111111-1", captor.getValue().getRun());
        assertEquals(true, captor.getValue().getRun().startsWith("11111111-1-"));
    }

    @Test
    @DisplayName("no crea el administrador si falta correo o contrasena")
    void noCreaCuandoFaltanDatos() {
        inicializador(true, "", PASSWORD, "11111111-1").run(null);
        inicializador(true, CORREO, "", "11111111-1").run(null);

        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("no falla si otra instancia creo el administrador al mismo tiempo")
    void noFallaSiHayConflictoDeIntegridad() {
        when(repository.existsByCorreo(CORREO)).thenReturn(false);
        when(encoder.encode(PASSWORD)).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertDoesNotThrow(() -> inicializador().run(null));
    }
}
