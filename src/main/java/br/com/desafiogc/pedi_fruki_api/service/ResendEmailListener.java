package br.com.desafiogc.pedi_fruki_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Só dispara o e-mail se a criação do pedido foi confirmada no PostgreSQL.
 * Sem lambda e sem salvar uma tabela de histórico de notificações.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResendEmailListener {
    private final ResendEmailService resend;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposPedidoConfirmado(PedidoConfirmadoEvent evento) {
        try {
            resend.enviarConfirmacaoPedido(evento);
        } catch (Exception erro) {
            // A transação JÁ foi confirmada: o pedido permanece salvo mesmo com falha de e-mail.
            // Sem fila/outbox persistente, não há reenvio automático garantido.
            log.error("Não foi possível solicitar o e-mail Resend do pedido {}: {}",
                    evento.pedidoId(), erro.getMessage());
        }
    }
}
