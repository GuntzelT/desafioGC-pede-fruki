package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "produto", schema = "public")
public class Produto {
    @Id
    @Column(name = "produto_id")
    private Integer produtoId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "cod_familia")
    private String codFamilia;

    @Column(name = "preco_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "preco_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoMinimo;

    @Column(name = "ativo")
    private Boolean ativo;
}
