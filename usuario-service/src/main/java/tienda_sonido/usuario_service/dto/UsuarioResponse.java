package tienda_sonido.usuario_service.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tienda_sonido.usuario_service.model.Rol;

@Getter
@AllArgsConstructor
public class UsuarioResponse {
    
// ATRIBUTOS ESPECIFICOS DE USUARIO

    private Long id;

    private Rol rol;

// ATRIBUTOS DE FORMULARIO DE REGISTRO

    private String nombre;

    private String apellido;

    private String correo;

    private String run;

    private LocalDate fechaNacimiento;

    private String direccion;
    
}
