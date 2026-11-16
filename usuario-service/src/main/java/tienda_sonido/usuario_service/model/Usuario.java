package tienda_sonido.usuario_service.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "usuarios")
@Getter
@Setter
@NoArgsConstructor 
public class Usuario {

// ATRIBUTOS ESPECIFICOS DE USUARIO
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private String password;

    @Column (nullable = false)
    @Enumerated (EnumType.STRING)
    private Rol rol;

// ATRIBUTOS DE FORMULARIO DE REGISTRO

    @Column (nullable = false)
    private String nombre;

    @Column (nullable = false)
    private String apellido;

    @Column (nullable = false, unique = true)
    private String correo;

    @Column (nullable = false, unique = true)
    private String run;

    @Column (nullable = false)
    private LocalDate fechaNacimiento;

    @Column (nullable = false)
    private String direccion;
    
}
