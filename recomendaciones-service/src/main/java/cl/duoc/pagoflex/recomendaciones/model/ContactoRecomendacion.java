package cl.duoc.pagoflex.recomendaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contacto_recomendacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactoRecomendacion {

    @Id
    @Column(name = "id_recomendacion")
    private Integer idRecomendacion;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "autoriza_contacto", nullable = false)
    private Boolean autorizaContacto;
}
