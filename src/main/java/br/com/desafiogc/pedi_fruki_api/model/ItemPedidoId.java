package br.com.desafiogc.pedi_fruki_api.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

// Representa a chave primária composta de public.item_pedido.

public class ItemPedidoId implements Serializable {
    private Integer pedidoId;
    private Integer produtoId;

    private static final long serialVersionUID = 1L;
}
