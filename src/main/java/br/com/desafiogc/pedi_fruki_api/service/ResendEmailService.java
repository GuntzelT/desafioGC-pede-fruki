package br.com.desafiogc.pedi_fruki_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração direta com a API HTTP do Resend.
 * O banco possui telefone, mas NÃO possui e-mail de clientes/vendedores.
 * Por isso o destinatário é uma caixa de demonstração configurada em RESEND_TO_EMAIL.
 * Sem tabela adicional e sem envio para WhatsApp/push.
 */
@Slf4j
@Service
public class ResendEmailService {
    private final RestClient http;
    private final String apiUrl;
    private final boolean habilitado;
    private final String chaveApi;
    private final String remetente;
    private final String destinatario;

    public ResendEmailService(
            RestClient.Builder builder,
            @Value("${app.email.resend.enabled:false}") boolean habilitado,
            @Value("${app.email.resend.api-key:}") String chaveApi,
            @Value("${app.email.resend.from:}") String remetente,
            @Value("${app.email.resend.to:}") String destinatario,
            @Value("${app.email.resend.api-url:https://api.resend.com/emails}") String apiUrl
    ) {
        this.http = builder.build();
        this.apiUrl = apiUrl;
        this.habilitado = habilitado;
        this.chaveApi = chaveApi;
        this.remetente = remetente;
        this.destinatario = destinatario;
    }

    public boolean habilitado() {
        return habilitado;
    }

    public boolean configurado() {
        if (!habilitado) {
            return false;
        }
        if (chaveApi == null || chaveApi.isBlank()) {
            return false;
        }
        if (remetente == null || remetente.isBlank()) {
            return false;
        }
        if (destinatario == null || destinatario.isBlank()) {
            return false;
        }
        return true;
    }

    public void enviarConfirmacaoPedido(PedidoConfirmadoEvent evento) {
        if (!habilitado) {
            log.info("E-mail Resend desativado; pedido {} permanece salvo.", evento.pedidoId());
            return;
        }
        if (!configurado()) {
            log.warn("Resend habilitado sem chave, remetente ou destinatário configurado; pedido {}.",
                    evento.pedidoId());
            return;
        }

        // O conteúdo (HTML com escape + versão em texto) é montado em EmailPedidoTemplate.
        List<String> para = new ArrayList<>();
        para.add(destinatario.trim());
        Map<String, Object> corpoJson = new HashMap<>();
        corpoJson.put("from", remetente.trim());
        corpoJson.put("to", para);
        corpoJson.put("subject", EmailPedidoTemplate.assunto(evento));
        corpoJson.put("html", EmailPedidoTemplate.html(evento));
        corpoJson.put("text", EmailPedidoTemplate.texto(evento));

        // Resend: POST /emails, token Bearer, payload JSON e idempotência do evento.
        ResponseEntity<String> resposta = http.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + chaveApi.trim())
                .header("Idempotency-Key", "pede-fruki-pedido-" + evento.pedidoId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpoJson)
                .retrieve()
                .toEntity(String.class);
        log.info("Resend aceitou solicitação de e-mail do pedido {} (HTTP {}).",
                evento.pedidoId(), resposta.getStatusCode().value());
        // 2xx significa aceitação pela API; entrega final pode falhar depois.
    }
}