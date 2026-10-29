package br.com.desafiogc.pedi_fruki_api.dto;

import jakarta.validation.constraints.NotBlank;

public record IdentificarClienteRequest(
        @NotBlank(message = "Informe o CNPJ cadastrado ou o código do cliente.")
        String identificador
) {}
