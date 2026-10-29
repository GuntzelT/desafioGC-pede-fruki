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
@Table(name = "cidade", schema = "public")
public class Cidade {
    @Id
    @Column(name = "id_cidade")
    private Integer idCidade;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;
}
