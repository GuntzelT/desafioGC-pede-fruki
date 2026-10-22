package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "produto")
@Getter
@NoArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id_item;

    @Column(nullable = true)
    private String nome;

    @Column(nullable = true)
    private long cod_familia;

    @Column(nullable = true)
    private double preco_base;

    @Column(nullable = true)
    private double preco_minimo;

    @Column(nullable = false)
    private boolean ativo;
}
