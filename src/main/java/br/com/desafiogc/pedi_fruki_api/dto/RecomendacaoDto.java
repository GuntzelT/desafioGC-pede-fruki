package br.com.desafiogc.pedi_fruki_api.dto;

import java.math.BigDecimal;

/** Resultado do motor de recomendações usado tanto pelo Pede Fruki quanto pelo app do vendedor. */
public record RecomendacaoDto(
        Integer produtoId,
        String nomeProduto,
        String categoria,
        int score,
        String motivo,
        String fatorPrincipal,
        BigDecimal preco,
        int scoreHistorico,
        int scorePerfil,
        int scoreClima,
        int scoreCalendario,
        Integer estoqueDisponivel
) {}
