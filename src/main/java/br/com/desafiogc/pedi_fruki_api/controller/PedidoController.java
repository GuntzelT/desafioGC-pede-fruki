package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.CriarPedidoRequest;
import br.com.desafiogc.pedi_fruki_api.dto.PedidoCriadoResponse;
import br.com.desafiogc.pedi_fruki_api.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoCriadoResponse finalizar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CriarPedidoRequest request
    ) {
        return service.finalizar(authorization, request);
    }

    @GetMapping("/{idPedido}")
    public PedidoCriadoResponse buscar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Integer idPedido
    ) {
        return service.buscarPedido(authorization, idPedido);
    }
}
