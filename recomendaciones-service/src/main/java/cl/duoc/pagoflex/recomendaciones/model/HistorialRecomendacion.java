package cl.duoc.pagoflex.recomendaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "historial_recomendacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialRecomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @Column(name = "id_recomendacion", nullable = false)
    private Integer idRecomendacion;

    @Column(name = "id_estado_recomendacion", nullable = false)
    private Integer idEstadoRecomendacion;

    @Column(name = "id_agente", nullable = false)
    private Integer idAgente;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", length = 255)
    private String motivo;
}
