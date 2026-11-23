package cl.duoc.pagoflex.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresa_cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCliente {

    @Id
    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "razon_social", nullable = false, length = 120)
    private String razonSocial;

    @Column(name = "nombre_fantasia", nullable = false, length = 80)
    private String nombreFantasia;

    @Column(name = "rut", nullable = false, unique = true, length = 12)
    private String rut;

    @Column(name = "id_rubro", nullable = false)
    private Integer idRubro;

    @Column(name = "correo_atencion", nullable = false, length = 120)
    private String correoAtencion;

    @Column(name = "telefono_atencion", nullable = false, length = 20)
    private String telefonoAtencion;

    @Column(name = "recargo_atraso", nullable = false)
    private Integer recargoAtraso;

    @Column(name = "activa", nullable = false)
    private Boolean activa;
}
