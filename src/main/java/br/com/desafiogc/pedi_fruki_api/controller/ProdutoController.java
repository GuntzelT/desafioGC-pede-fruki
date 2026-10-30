package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {
    private final ProdutoRepository produtos;

    @GetMapping
    public List<ProdutoDto> listarAtivos() {
        List<br.com.desafiogc.pedi_fruki_api.model.Produto> encontrados = produtos.findByAtivoTrueOrderByNomeAsc();
        List<ProdutoDto> resposta = new java.util.ArrayList<>();

        // Converte cada entidade de produto em um dto que será enviado ao frontend.

        for (int i = 0; i < encontrados.size(); i++) {
            br.com.desafiogc.pedi_fruki_api.model.Produto produto = encontrados.get(i);
            resposta.add(new ProdutoDto(produto.getProdutoId(), produto.getNome(), produto.getCodFamilia(),
                    produto.getPrecoBase(), produto.getPrecoMinimo(), produto.getAtivo()));
        }
        return resposta;
    }
}
