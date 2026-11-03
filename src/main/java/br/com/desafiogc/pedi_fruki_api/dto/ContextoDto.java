package br.com.desafiogc.pedi_fruki_api.dto;

import java.util.List;

public record ContextoDto(
        Integer clienteId,
        String cidade,
        String uf,
        Object clima,
        List<?> feriados,
        String observacao
) {}
