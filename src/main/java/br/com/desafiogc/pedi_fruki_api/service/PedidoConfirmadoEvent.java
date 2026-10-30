package br.com.desafiogc.pedi_fruki_api.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

//Evento criado na mesma transação do pedido e processado pelo n8n somente após o commit.
public record PedidoConfirmadoEvent(
        Integer pedidoId,
        Integer clienteId,
        String clienteNome,
        Integer hierarquiaVendedorId,
        String celular,
        String mensagem,
        Integer quantidadeFardos,
        BigDecimal valorTotal,
        OffsetDateTime dataPedido
) {}
