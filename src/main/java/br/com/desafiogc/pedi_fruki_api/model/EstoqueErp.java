package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "estoque_erp", schema = "public")
public class EstoqueErp {
    @Id
    @Column(name = "produto_id")
    private Integer produtoId;

    @Column(name = "quantidade_disponivel", nullable = false)
    private Integer quantidadeDisponivel;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;
}
