package cl.duoc.pagoflex.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comuna")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comuna {

    @Id
    @Column(name = "id_comuna")
    private Integer idComuna;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;
}
