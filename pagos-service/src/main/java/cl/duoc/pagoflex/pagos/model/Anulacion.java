package cl.duoc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "anulacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Anulacion {

    @Id
    @Column(name = "id_compromiso")
    private Integer idCompromiso;

    @Column(name = "id_ejecutivo", nullable = false)
    private Integer idEjecutivo;

    @Column(name = "id_motivo_anulacion", nullable = false)
    private Integer idMotivoAnulacion;

    @Column(name = "comentario", length = 255)
    private String comentario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
