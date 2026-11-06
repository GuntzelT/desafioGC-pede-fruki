package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.ContextoDto;
import br.com.desafiogc.pedi_fruki_api.model.Cidade;
import br.com.desafiogc.pedi_fruki_api.model.Cliente;
import br.com.desafiogc.pedi_fruki_api.repository.CidadeRepository;
import br.com.desafiogc.pedi_fruki_api.repository.ClienteRepository;
import br.com.desafiogc.pedi_fruki_api.service.CalendarioService;
import br.com.desafiogc.pedi_fruki_api.service.ClimaService;
import br.com.desafiogc.pedi_fruki_api.service.SimulacaoSessaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ContextoController {
    private final ClienteRepository clientes;
    private final CidadeRepository cidades;
    private final ClimaService clima;
    private final CalendarioService calendario;
    private final SimulacaoSessaoService sessoes;

    @GetMapping("/{id}/contexto")
    public ContextoDto contexto(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        sessoes.exigirClienteDaSessao(authorization, id);
        Cliente cliente = clientes.findById(id).orElse(null);
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado: " + id);
        }
        if (cliente.getIdCidade() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cliente sem cidade cadastrada");
        }
        Cidade cidade = cidades.findById(cliente.getIdCidade()).orElse(null);
        if (cidade == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidade do cliente não encontrada");
        }

        return new ContextoDto(id, cidade.getNome(), cidade.getUf(),
                clima.previsao(cidade.getNome(), cidade.getUf()), calendario.proximosFeriados(cidade.getUf()),
                "Clima, feriados nacionais/estaduais da UF do cliente e sazonalidades comerciais (estações do ano, Carnaval, Páscoa, Dia dos Pais e Natal/Réveillon) podem alterar o ranking por regras heurísticas.");
    }
}
