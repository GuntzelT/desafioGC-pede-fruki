package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.ProdutoDto;
import br.com.desafiogc.pedi_fruki_api.model.Produto;
import br.com.desafiogc.pedi_fruki_api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {
    private final ProdutoRepository produtos;

    @GetMapping
    public List<ProdutoDto> listarAtivos() {
        List<Produto> encontrados = produtos.findByAtivoTrueOrderByNomeAsc();
        List<ProdutoDto> resposta = new ArrayList<>();
        // Mantém o nome produtoId para refletir public.produto.produto_id.
        for (int indice = 0; indice < encontrados.size(); indice++) {
            Produto produto = encontrados.get(indice);
            resposta.add(new ProdutoDto(produto.getProdutoId(), produto.getNome(),
                    produto.getCodFamilia(), produto.getPrecoBase(),
                    produto.getPrecoMinimo(), produto.getAtivo()));
        }
        return resposta;
    }
}
