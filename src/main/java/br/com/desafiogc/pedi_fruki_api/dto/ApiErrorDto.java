package br.com.desafiogc.pedi_fruki_api.dto;

import java.time.OffsetDateTime;

public record ApiErrorDto(
        OffsetDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho
) {}
