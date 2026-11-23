package cl.duoc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario_reconocimiento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario_final", "id_reconocimiento"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioReconocimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario_final", nullable = false)
    private Integer idUsuarioFinal;

    @Column(name = "id_reconocimiento", nullable = false)
    private Integer idReconocimiento;

    @Column(name = "fecha_obtencion", nullable = false)
    private LocalDateTime fechaObtencion;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;
}
