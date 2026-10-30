
package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "item_pedido", schema = "public")
@IdClass(ItemPedidoId.class)

public class ItemPedido {

    @Id
    @Column(name = "pedido_id")
    private Integer pedidoId;

    
    @Id
    @Column(name = "produto_id")
    private Integer produtoId;

   
    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    
    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

  
    public ItemPedido(Integer pedidoId, Integer produtoId, Integer quantidade, BigDecimal valorUnitario) {
        this.pedidoId = pedidoId;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
    }
}
