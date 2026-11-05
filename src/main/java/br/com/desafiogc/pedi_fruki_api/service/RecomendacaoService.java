package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.RecomendacaoDto;
import br.com.desafiogc.pedi_fruki_api.model.Cliente;
import br.com.desafiogc.pedi_fruki_api.model.EstoqueErp;
import br.com.desafiogc.pedi_fruki_api.model.Produto;
import br.com.desafiogc.pedi_fruki_api.repository.ClienteRepository;
import br.com.desafiogc.pedi_fruki_api.repository.EstoqueRepository;
import br.com.desafiogc.pedi_fruki_api.repository.ItemPedidoRepository;
import br.com.desafiogc.pedi_fruki_api.repository.ProdutoRepository;
import br.valueobject.PerfilComercial;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecomendacaoService {
    private final ClienteRepository clientes;
    private final ProdutoRepository produtos;
    private final ItemPedidoRepository itens;
    private final EstoqueRepository estoques;
    private final ScoreCalculator scoreCalculator;
    private final ContextoPontuacaoService contextoService;

    public List<RecomendacaoDto> recomendar(Integer clienteId) {
        Cliente cliente = clientes.findById(clienteId).orElse(null);
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado: " + clienteId);
        }

        Map<Integer, Long> historico = somarPorProduto(itens.resumirComprasDoCliente(clienteId));
        PerfilComercial perfilComercial = cliente.getPerfilComercial();
        Map<Integer, Long> perfil = new HashMap<>();
        // Sem segmento/subsegmento não há grupo comparável: dispensamos essa consulta SQL.
        if (perfilComercial.temSegmentacaoCompleta()) {
            perfil = somarPorProduto(itens.resumirComprasDoPerfil(
                    clienteId, cliente.getSegmento(), cliente.getSubsegmento()));
        }
        long maiorHistorico = maiorValor(historico);
        long maiorPerfil = maiorValor(perfil);
        ContextoPontuacaoService.ContextoComercial contexto = contextoService.obter(cliente);

        Map<Integer, EstoqueErp> estoquePorProduto = new HashMap<>();
        List<EstoqueErp> todosEstoques = estoques.findAll();
        // Cria um mapa por ID para localizar o estoque de cada produto rapidamente.
        for (int indice = 0; indice < todosEstoques.size(); indice++) {
            EstoqueErp estoque = todosEstoques.get(indice);
            estoquePorProduto.put(estoque.getProdutoId(), estoque);
        }

        List<RecomendacaoDto> resultado = new ArrayList<>();
        List<Produto> catalogo = produtos.findByAtivoTrueOrderByNomeAsc();
        // Preço e perfil comercial são conceitos de negócio, não consultas SQL.

        // Analisa cada produto do catálogo usando for tradicional.
        for (int indice = 0; indice < catalogo.size(); indice++) {
            Produto produto = catalogo.get(indice);
            Integer produtoId = produto.getProdutoId();
            EstoqueErp estoque = estoquePorProduto.get(produtoId);
            if (estoque != null && estoque.getQuantidadeDisponivel() != null
                    && estoque.getQuantidadeDisponivel() <= 0) {
                continue;
            }

            // A entidade Produto usa o PerfilComercial para escolher o mesmo preço do pedido.
            BigDecimal preco = produto.precoPara(perfilComercial);
            if (preco == null || preco.signum() < 0) {
                continue;
            }

            long comprasCliente = valorOuZero(historico.get(produtoId));
            long comprasPerfil = valorOuZero(perfil.get(produtoId));
            int pontosClima = contextoService.pontosClima(produto, contexto);
            int pontosCalendario = contextoService.pontosCalendario(produto, contexto);
            ScoreCalculator.Pontuacao pontuacao = scoreCalculator.calcular(
                    comprasCliente, maiorHistorico, comprasPerfil, maiorPerfil,
                    pontosClima, pontosCalendario);

            List<String> motivos = new ArrayList<>();
            if (comprasCliente > 0) {
                motivos.add("já comprado por este cliente");
            }
            if (comprasPerfil > 0) {
                motivos.add("comprado por clientes do mesmo segmento e subsegmento");
            }
            String motivoClima = contextoService.motivoClima(produto, pontosClima, contexto);
            if (motivoClima != null) {
                motivos.add(motivoClima);
            }
            String motivoCalendario = contextoService.motivoCalendario(pontosCalendario, contexto);
            if (motivoCalendario != null) {
                motivos.add(motivoCalendario);
            }
            if (motivos.isEmpty()) {
                motivos.add("produto ativo para ampliar o mix; sem sinal positivo no histórico ou contexto consultado");
            }

            Integer quantidadeDisponivel = null;
            if (estoque != null) {
                quantidadeDisponivel = estoque.getQuantidadeDisponivel();
            }
            resultado.add(new RecomendacaoDto(
                    produtoId,
                    produto.getNome(),
                    produto.getCodFamilia(),
                    pontuacao.total(),
                    String.join("; ", motivos),
                    fatorPrincipal(pontuacao),
                    preco.setScale(2, java.math.RoundingMode.HALF_UP),
                    pontuacao.historico(),
                    pontuacao.perfil(),
                    pontuacao.clima(),
                    pontuacao.calendario(),
                    quantidadeDisponivel
            ));
        }

        Collections.sort(resultado, new Comparator<RecomendacaoDto>() {
            @Override
            public int compare(RecomendacaoDto primeiro, RecomendacaoDto segundo) {
                int pelaPontuacao = Integer.compare(segundo.score(), primeiro.score());
                if (pelaPontuacao != 0) {
                    return pelaPontuacao;
                }
                String nomePrimeiro = "";
                if (primeiro.nomeProduto() != null) {
                    nomePrimeiro = primeiro.nomeProduto();
                }
                String nomeSegundo = "";
                if (segundo.nomeProduto() != null) {
                    nomeSegundo = segundo.nomeProduto();
                }
                return String.CASE_INSENSITIVE_ORDER.compare(nomePrimeiro, nomeSegundo);
            }
        });

        if (resultado.size() > 10) {
            return new ArrayList<>(resultado.subList(0, 10));
        }
        return resultado;
    }

    private long maiorValor(Map<Integer, Long> valores) {
        long maior = 0L;
        // Map não possui get(índice): Iterator permite percorrer seus valores com while.
        Iterator<Long> iteradorValores = valores.values().iterator();
        while (iteradorValores.hasNext()) {
            Long valor = iteradorValores.next();
            if (valor != null && valor > maior) {
                maior = valor;
            }
        }
        return maior;
    }

    private long valorOuZero(Long valor) {
        if (valor == null) {
            return 0L;
        }
        return valor;
    }

    private Map<Integer, Long> somarPorProduto(List<Object[]> linhas) {
        Map<Integer, Long> resultado = new HashMap<>();
        // Cada posição da lista é uma linha do resultado da consulta SQL.
        for (int indice = 0; indice < linhas.size(); indice++) {
            Object[] linha = linhas.get(indice);
            if (linha != null && linha.length >= 2
                    && linha[0] instanceof Number && linha[1] instanceof Number) {
                Number produtoId = (Number) linha[0];
                Number quantidade = (Number) linha[1];
                resultado.put(produtoId.intValue(), quantidade.longValue());
            }
        }
        return resultado;
    }

    private String fatorPrincipal(ScoreCalculator.Pontuacao pontuacao) {
        String fator = "Exploração do catálogo";
        int maiorPontuacao = 0;
        if (pontuacao.historico() > maiorPontuacao) {
            maiorPontuacao = pontuacao.historico();
            fator = "Histórico de compras";
        }
        if (pontuacao.perfil() > maiorPontuacao) {
            maiorPontuacao = pontuacao.perfil();
            fator = "Segmento e subsegmento";
        }
        if (pontuacao.clima() > maiorPontuacao) {
            maiorPontuacao = pontuacao.clima();
            fator = "Clima";
        }
        if (pontuacao.calendario() > maiorPontuacao) {
            fator = "Calendário";
        }
        return fator;
    }
}
