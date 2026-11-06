package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.ItemPedidoCriadoDto;
import br.com.desafiogc.pedi_fruki_api.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

//Publica evento em memória durante a transação; o listener do Resend é chamado somente após o commit.
@Service
@RequiredArgsConstructor
public class NotificacaoEventoService {
    private final ApplicationEventPublisher eventos;

    public void publicarConfirmacaoPedido(Cliente cliente, Integer pedidoId, int quantidade,
                                          BigDecimal total, OffsetDateTime dataPedido,
                                          List<ItemPedidoCriadoDto> itens) {
        String mensagem = "Pedido #" + pedidoId + " recebido com " + quantidade
                + " fardos. Total: R$ " + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + ".";
        eventos.publishEvent(new PedidoConfirmadoEvent(
                pedidoId, cliente.getIdCliente(), cliente.getNome(), cliente.getIdHierarquia(),
                cliente.getNumeroCelular(), mensagem, quantidade, total, dataPedido,
                List.copyOf(itens)));
    }
}