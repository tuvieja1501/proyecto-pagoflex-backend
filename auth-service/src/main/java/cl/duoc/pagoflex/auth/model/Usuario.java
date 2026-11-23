package cl.duoc.pagoflex.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_rol", nullable = false)
    private Integer idRol;

    @Column(name = "rut", nullable = false, unique = true, length = 12)
    private String rut;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "id_comuna")
    private Integer idComuna;

    @Column(name = "fecha_incorporacion")
    private LocalDate fechaIncorporacion;

    @Column(name = "avisos_activos")
    private Boolean avisosActivos;

    @Column(name = "dias_anticipacion")
    private Integer diasAnticipacion;

    @Column(name = "id_area")
    private Integer idArea;

    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "cargo", length = 80)
    private String cargo;
}
