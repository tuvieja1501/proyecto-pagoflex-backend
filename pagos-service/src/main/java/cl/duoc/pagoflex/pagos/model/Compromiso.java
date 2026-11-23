package cl.duoc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "compromiso", indexes = {
        @Index(name = "idx_compromiso_usuario", columnList = "id_usuario_final"),
        @Index(name = "idx_compromiso_empresa", columnList = "id_empresa")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compromiso")
    private Integer idCompromiso;

    @Column(name = "folio", nullable = false, unique = true, length = 20)
    private String folio;

    @Column(name = "id_usuario_final", nullable = false)
    private Integer idUsuarioFinal;

    @Column(name = "id_empresa", nullable = false)
    private Integer idEmpresa;

    @Column(name = "concepto", nullable = false, length = 120)
    private String concepto;

    @Column(name = "numero_cuota")
    private Integer numeroCuota;

    @Column(name = "total_cuotas")
    private Integer totalCuotas;

    @Column(name = "monto", nullable = false)
    private Integer monto;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "id_estado_compromiso", nullable = false)
    private Integer idEstadoCompromiso;

    @Column(name = "recargo_aplicado", nullable = false)
    private Integer recargoAplicado = 0;

    @Column(name = "fecha_cambio_estado", nullable = false)
    private LocalDateTime fechaCambioEstado;

    @Column(name = "id_ejecutivo_registro")
    private Integer idEjecutivoRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
