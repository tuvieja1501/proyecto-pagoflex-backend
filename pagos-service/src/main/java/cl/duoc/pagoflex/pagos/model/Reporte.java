package cl.duoc.pagoflex.pagos.model;

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
@Table(name = "reporte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Column(name = "id_compromiso", nullable = false)
    private Integer idCompromiso;

    @Column(name = "id_motivo_reporte", nullable = false)
    private Integer idMotivoReporte;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "id_estado_reporte", nullable = false)
    private Integer idEstadoReporte;

    @Column(name = "id_agente")
    private Integer idAgente;

    @Column(name = "fecha_toma")
    private LocalDateTime fechaToma;

    @Column(name = "respuesta", length = 500)
    private String respuesta;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
