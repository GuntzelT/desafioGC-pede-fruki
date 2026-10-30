package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService service;
    private final SimulacaoSessaoService sessoes;

    @GetMapping("/{id}")
    public ClienteResumoDto buscarResumo(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        sessoes.exigirClienteDaSessao(authorization, id);
        return service.buscarResumo(id);
    }
}

