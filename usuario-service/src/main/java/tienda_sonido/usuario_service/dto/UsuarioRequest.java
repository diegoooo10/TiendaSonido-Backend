package tienda_sonido.usuario_service.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor 
public class UsuarioRequest {

// ATRIBUTOS ESPECIFICOS DE USUARIO

    @NotBlank (message = "Debe ingresar una contraseña")
    private String password;

// ATRIBUTOS DE FORMULARIO DE REGISTRO

    @NotBlank (message = "Debe ingresar un nombre")
    private String nombre;

    @NotBlank (message = "Debe ingresar un apellido")
    private String apellido;

    @NotBlank (message = "Debe ingresar un correo")
    @Email (message = "Debe ingresar un correo válido")
    private String correo;

    @NotBlank (message = "Debe ingresar un run")
    private String run;

    @NotNull (message = "Debe ingresar una fecha")
    private LocalDate fechaNacimiento;

    @NotBlank (message = "Debe ingresar una direccion")
    private String direccion;
    
}
