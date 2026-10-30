package br.com.desafiogc.pedi_fruki_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(
        @NotNull(message = "Informe o ID do produto.")
        @Positive(message = "O ID do produto deve ser positivo.")
        Integer produtoId,

        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "A quantidade precisa ser maior que zero.")
        @Max(value = 1000000, message = "A quantidade máxima por produto é 1.000.000.")
        Integer quantidade
) {}
