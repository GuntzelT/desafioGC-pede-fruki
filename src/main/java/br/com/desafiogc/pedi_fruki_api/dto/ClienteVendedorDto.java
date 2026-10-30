package br.com.desafiogc.pedi_fruki_api.dto;

public record ClienteVendedorDto(
        Integer idCliente,
        String nome,
        String segmento,
        String subsegmento
) {}
