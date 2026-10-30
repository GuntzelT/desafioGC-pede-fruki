package br.com.desafiogc.pedi_fruki_api.dto;

import java.time.OffsetDateTime;

public record ClienteResumoDto(
        Integer idCliente,
        String nome,
        String segmento,
        String subsegmento,
        String cidade,
        String uf,
        OffsetDateTime ultimaCompra
) {}
