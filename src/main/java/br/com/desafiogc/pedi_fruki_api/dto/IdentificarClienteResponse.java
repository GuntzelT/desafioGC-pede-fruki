package br.com.desafiogc.pedi_fruki_api.dto;

public record IdentificarClienteResponse(
        String tokenSessao,
        ClienteResumoDto cliente,
        String mensagem
) {}
