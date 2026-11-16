package tienda_sonido.usuario_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tienda_sonido.usuario_service.model.Rol;

@Getter 
@Setter
@AllArgsConstructor
@NoArgsConstructor 
public class UsuarioUpdateRequest {

    @NotBlank (message = "Debe ingresar un RUN")
    private String run;

    @NotBlank (message = "Debe ingresar un nombre")
    private String nombre;

    @NotBlank (message = "Debe ingresar un apellido")
    private String apellido;

    @NotBlank (message = "Debe ingresar un correo")
    @Email 
    private String correo;

    @NotNull (message = "Debe ingresar una fecha")
    private LocalDate fechaNacimiento;

    @NotNull (message = "Debe asignar un rol")
    private Rol rol;

    @NotBlank (message = "Debe ingresar direccion")
    private String direccion;
    
}
