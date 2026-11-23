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
@Table(name = "pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @Column(name = "numero_comprobante", nullable = false, unique = true, length = 20)
    private String numeroComprobante;

    @Column(name = "id_compromiso", nullable = false, unique = true)
    private Integer idCompromiso;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "monto_pagado", nullable = false)
    private Integer montoPagado;

    @Column(name = "id_canal", nullable = false)
    private Integer idCanal;

    @Column(name = "id_medio_pago", nullable = false)
    private Integer idMedioPago;
}
