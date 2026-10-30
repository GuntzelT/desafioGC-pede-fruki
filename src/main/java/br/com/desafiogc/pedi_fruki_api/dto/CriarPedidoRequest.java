package br.com.desafiogc.pedi_fruki_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarPedidoRequest(
        @NotEmpty(message = "Adicione pelo menos um produto ao pedido.")
        @Size(max = 100, message = "O pedido pode conter no máximo 100 linhas de produtos.")
        List<@NotNull @Valid ItemPedidoRequest> itens
) {}
