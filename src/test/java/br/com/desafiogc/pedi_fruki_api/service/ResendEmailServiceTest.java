package br.com.desafiogc.pedi_fruki_api.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendEmailServiceTest {
    @Test
    void naoEnviaQuandoDesativado() {
        ResendEmailService service = new ResendEmailService(
                RestClient.builder(), false, "", "", "", "https://api.resend.com/emails");
        assertFalse(service.configurado());
        // Não existe evento e não há chamada externa neste teste.
    }

    @Test
    void exigeTodasAsConfiguracoes() {
        ResendEmailService incompleto = new ResendEmailService(
                RestClient.builder(), true, "chave", "", "destino@exemplo.com",
                "https://api.resend.com/emails");
        assertFalse(incompleto.configurado());

        ResendEmailService completo = new ResendEmailService(
                RestClient.builder(), true, "chave", "origem@exemplo.com",
                "destino@exemplo.com", "https://api.resend.com/emails");
        assertTrue(completo.configurado());
    }
}
