package cl.duoc.pagoflex.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credencial")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credencial {

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "sal", nullable = false, length = 64)
    private String sal;

    @Column(name = "clave_hash", nullable = false, length = 128)
    private String claveHash;

    @Column(name = "codigo_hash", length = 128)
    private String codigoHash;

    @Column(name = "codigo_expira")
    private LocalDateTime codigoExpira;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;
}
