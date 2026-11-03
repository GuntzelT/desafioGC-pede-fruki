package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hierarquia", schema = "public")
public class Hierarquia {
    @Id
    @Column(name = "id_hierarquia")
    private Integer idHierarquia;

    @Column(name = "cod_representante")
    private Integer codRepresentante;

    @Column(name = "nome_vendedor", nullable = false)
    private String nomeVendedor;
}
