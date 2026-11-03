package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import br.valueobject.PerfilComercial;
import java.math.RoundingMode;

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

    @Column(name = "preco_base", precision = 12, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "preco_minimo", precision = 12, scale = 2)
    private BigDecimal precoMinimo;

    @Column(name = "ativo")
    private Boolean ativo;
    /**
     * Escolhe o preço do catálogo de acordo com o segmento do cliente.
     * A regra fica na entidade Produto e utiliza somente o Value Object PerfilComercial.
     * Não há desconto nem promoção: atacado = preco_minimo; demais = preco_base.
     * Se não houver preço válido, devolve null para o Service decidir como responder.
     */
    public BigDecimal precoPara(PerfilComercial perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("O perfil comercial é obrigatório.");
        }

        BigDecimal precoEscolhido;
        if (perfil.ehAtacado()) {
            precoEscolhido = precoMinimo;
        } else {
            precoEscolhido = precoBase;
        }

        if (precoEscolhido == null) {
            return null;
        }
        if (precoEscolhido.signum() < 0) {
            return null;
        }
        return precoEscolhido.setScale(2, RoundingMode.HALF_UP);
    }
}
