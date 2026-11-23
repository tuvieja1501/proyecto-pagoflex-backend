package cl.duoc.pagoflex.recomendaciones.model;

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
@Table(name = "recomendacion", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero"}),
        @UniqueConstraint(columnNames = {"id_usuario_final", "nombre_organizacion"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recomendacion")
    private Integer idRecomendacion;

    @Column(name = "numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "id_usuario_final", nullable = false)
    private Integer idUsuarioFinal;

    @Column(name = "nombre_organizacion", nullable = false, length = 80)
    private String nombreOrganizacion;

    @Column(name = "rut_organizacion", length = 12)
    private String rutOrganizacion;

    @Column(name = "id_rubro", nullable = false)
    private Integer idRubro;

    @Column(name = "id_comuna", nullable = false)
    private Integer idComuna;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "id_estado_recomendacion", nullable = false)
    private Integer idEstadoRecomendacion;
}
