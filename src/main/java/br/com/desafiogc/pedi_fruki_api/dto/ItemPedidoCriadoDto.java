package br.com.desafiogc.pedi_fruki_api.dto;

import java.math.BigDecimal;

/** Resumo de um item do pedido usando o preço cadastrado no produto, sem desconto promocional. */
public record ItemPedidoCriadoDto(
        Integer produtoId,
        String nomeProduto,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal subtotal
) {}
