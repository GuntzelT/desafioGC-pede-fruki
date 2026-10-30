package br.com.desafiogc.pedi_fruki_api.dto;

import java.math.BigDecimal;

public record ProdutoDto(
        Integer idItem,
        String nome,
        String codFamilia,
        BigDecimal precoBase,
        BigDecimal precoMinimo,
        Boolean ativo
) {}
