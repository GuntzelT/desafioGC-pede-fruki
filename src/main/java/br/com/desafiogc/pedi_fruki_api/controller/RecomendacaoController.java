package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.RecomendacaoDto;
import br.com.desafiogc.pedi_fruki_api.service.RecomendacaoService;
import br.com.desafiogc.pedi_fruki_api.service.SimulacaoSessaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class RecomendacaoController {
    private final RecomendacaoService service;
    private final SimulacaoSessaoService sessoes;

    @GetMapping("/{id}/recomendacoes")
    public List<RecomendacaoDto> recomendar(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Integer clienteId = sessoes.exigirClienteDaSessao(authorization, id);
        return service.recomendar(clienteId);
    }
}
