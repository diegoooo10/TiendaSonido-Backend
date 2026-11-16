package tienda_sonido.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor 
public class LoginRequest {

    @NotBlank (message = "Debe ingresar un email")
    @Email (message = "Debe ingresar un correo válido")
    private String correo;

    @NotBlank (message = "Debe ingresar una contraseña")
    private String password;
    
}
