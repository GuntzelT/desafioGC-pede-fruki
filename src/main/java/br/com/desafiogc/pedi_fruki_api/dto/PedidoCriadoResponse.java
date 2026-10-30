package br.com.desafiogc.pedi_fruki_api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoCriadoResponse(
        Integer idPedido,
        OffsetDateTime dataPedido,
        ClienteResumoDto cliente,
        List<ItemPedidoCriadoDto> itens,
        Integer quantidadeTotal,
        BigDecimal valorTotal,
        String mensagem,
        Boolean eventoN8nGerado,
        Boolean estoqueAtualizado,
        List<String> observacoes
) {}
