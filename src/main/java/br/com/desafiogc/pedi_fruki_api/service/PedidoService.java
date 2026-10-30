package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.model.*;
import br.com.desafiogc.pedi_fruki_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PedidoService {
    @Value("${app.pedidos.quantidade-minima:200}")
    private int quantidadeMinimaPedido;

    @Value("${app.estoque.baixar-no-pedido:true}")
    private boolean baixarEstoqueNoPedido;

    @Value("${app.estoque.exigir-registro:true}")
    private boolean exigirRegistroEstoque;

    private final SimulacaoSessaoService sessoes;
    private final ClienteRepository clientes;
    private final ClienteService clienteService;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;
    private final PedidoRepository pedidos;
    private final ItemPedidoRepository itensPedido;
    private final NotificacaoEventoService notificacaoService;
    private final JdbcTemplate jdbcTemplate;

    //Grava pedido, itens, estoque conhecido e ultima compra numa transação. Se alguma etapa falhar, o banco reverte todas as alterações realizadas
    @Transactional
    public PedidoCriadoResponse finalizar(String authorization, CriarPedidoRequest request) {
        Integer idCliente = sessoes.exigirCliente(authorization);
        Cliente cliente = clientes.findById(idCliente).orElse(null);
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente da sessão não existe mais.");
        }
        List<ItemPedidoRequest> itens = request.itens();

        Set<Integer> idsProdutos = new HashSet<>();

        // Percorre os itens por índice para identificar produtos repetidos.
        
        for (int i = 0; i < itens.size(); i++) {
            ItemPedidoRequest item = itens.get(i);
            if (!idsProdutos.add(item.produtoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada produto deve aparecer apenas uma vez no pedido. Some as quantidades repetidas.");
            }
        }
        long quantidadeTotalLong = 0L;
        
        // Soma a quantidade de fardos, elemento por elemento.
        
        for (int i = 0; i < itens.size(); i++) {
            ItemPedidoRequest item = itens.get(i);
            quantidadeTotalLong += item.quantidade();
        }
        if (quantidadeTotalLong < quantidadeMinimaPedido) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O pedido mínimo é de " + quantidadeMinimaPedido + " fardos. Quantidade atual: " + quantidadeTotalLong + ".");
        }
        if (quantidadeTotalLong > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade total do pedido é muito grande.");
        }

        Map<Integer, Produto> produtosPorId = new HashMap<>();
        List<Produto> encontrados = produtos.findAllById(idsProdutos);
        
        // Busca os produtos retornados pela consulta, um de cada vez.
        
        for (int i = 0; i < encontrados.size(); i++) {
            Produto produto = encontrados.get(i);
            produtosPorId.put(produto.getProdutoId(), produto);
        }
        if (produtosPorId.size() != idsProdutos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais produtos não existem no catálogo.");
        }
        
        // Usa p iterador para percorrer pq Map nao tem indice

        Iterator<Produto> iteradorProdutos = produtosPorId.values().iterator();
        while (iteradorProdutos.hasNext()) {
            Produto produto = iteradorProdutos.next();
            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O produto '" + produto.getNome() + "' não está mais ativo para venda.");
            }
        }

        // O PerfilComercial concentra a regra de identificação de atacado.

        boolean atacado = cliente.getPerfilComercial().ehAtacado();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        Map<Integer, BigDecimal> precosFinais = new HashMap<>();
        BigDecimal valorTotal = BigDecimal.ZERO;
        List<ItemPedidoCriadoDto> itensResposta = new ArrayList<>();

        // Calcula preço e subtotal de cada item do pedido.
        
        for (int i = 0; i < itens.size(); i++) {
            ItemPedidoRequest item = itens.get(i);
            Produto produto = produtosPorId.get(item.produtoId());
            BigDecimal preco;
            if (atacado) {
                preco = produto.getPrecoMinimo();
            } else {
                preco = produto.getPrecoBase();
            }
            if (preco == null || preco.signum() < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O produto '" + produto.getNome() + "' não possui preço válido cadastrado.");
            }
            preco = preco.setScale(2, RoundingMode.HALF_UP);
            precosFinais.put(produto.getProdutoId(), preco);
            BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(item.quantidade()))
                    .setScale(2, RoundingMode.HALF_UP);
            itensResposta.add(new ItemPedidoCriadoDto(
                    produto.getProdutoId(), produto.getNome(), item.quantidade(), preco, subtotal));
            valorTotal = valorTotal.add(subtotal);
        }
        valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);

        List<String> observacoes = new ArrayList<>();
        boolean estoqueAtualizado = true;
        if (baixarEstoqueNoPedido) {
            Map<Integer, EstoqueErp> estoquePorProduto = new HashMap<>();
            List<EstoqueErp> registrosEstoque = estoques.findAllById(idsProdutos);
        
            // Transforma a lista do banco em um map por produto_id.
        
            for (int i = 0; i < registrosEstoque.size(); i++) {
                EstoqueErp registro = registrosEstoque.get(i);
                estoquePorProduto.put(registro.getProdutoId(), registro);
            }

            // Confere o estoque disponível para cada produto do pedido.
            
            for (int i = 0; i < itens.size(); i++) {
                ItemPedidoRequest item = itens.get(i);
                Produto produto = produtosPorId.get(item.produtoId());
                EstoqueErp estoque = estoquePorProduto.get(item.produtoId());
                if (estoque == null) {
                    if (exigirRegistroEstoque) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Não há registro de estoque para '" + produto.getNome()
                                        + "'. Sincronize estoque_erp antes de aceitar este pedido.");
                    }
            
                    //quantidade desconhecida não pode ser decrementada.
            
                    estoqueAtualizado = false;
                    observacoes.add("Não foi possível baixar o estoque de '" + produto.getNome()
                            + "' porque não existe registro em estoque_erp.");
                    continue;
                }
                if (estoque.getQuantidadeDisponivel() == null || estoque.getQuantidadeDisponivel() < item.quantidade()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Estoque insuficiente para '" + produto.getNome() + "'. Disponível: "
                                    + estoque.getQuantidadeDisponivel() + "; solicitado: " + item.quantidade() + ".");
                }
                int linhasAtualizadas = jdbcTemplate.update("""
                        UPDATE public.estoque_erp
                        SET quantidade_disponivel = quantidade_disponivel - ?,
                            ultima_atualizacao = ?
                        WHERE produto_id = ? AND quantidade_disponivel >= ?
                        """, item.quantidade(), Timestamp.from(agora.toInstant()), item.produtoId(), item.quantidade());
                if (linhasAtualizadas != 1) {
                    
                    //bloqueia venda quando dois pedidos disputam o mesmo saldo.
                    
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "O estoque de '" + produto.getNome() + "' mudou durante a finalização. Atualize o catálogo e tente novamente.");
                }
            }
        } else {
            estoqueAtualizado = false;
            observacoes.add("A baixa de estoque está desativada pela configuração do backend.");
        }
        if (!estoqueAtualizado && observacoes.isEmpty()) {
            observacoes.add("O pedido foi salvo, mas nem todos os itens tiveram estoque atualizado.");
        }

        Integer idPedido = inserirCabecalhoComIdSeguro(idCliente, agora);
        
        for (int i = 0; i < itens.size(); i++) {
            ItemPedidoRequest item = itens.get(i);
            jdbcTemplate.update("""
                    INSERT INTO public.item_pedido (pedido_id, produto_id, quantidade, valor_unitario)
                    VALUES (?, ?, ?, ?)
                    """, idPedido, item.produtoId(), item.quantidade(), precosFinais.get(item.produtoId()));
        }
        jdbcTemplate.update("UPDATE public.cliente SET ultima_compra = ? WHERE id_cliente = ?",
                Timestamp.from(agora.toInstant()), idCliente);

        notificacaoService.publicarConfirmacaoPedido(cliente, idPedido,
                (int) quantidadeTotalLong, valorTotal, agora);
        ClienteResumoDto resumoAtualizado = clienteService.buscarResumo(idCliente);
        ClienteResumoDto resumo = new ClienteResumoDto(
                resumoAtualizado.idCliente(), resumoAtualizado.nome(), resumoAtualizado.segmento(),
                resumoAtualizado.subsegmento(), resumoAtualizado.cidade(), resumoAtualizado.uf(), agora);
        String mensagem;
        if (estoqueAtualizado) {
            mensagem = "Pedido salvo e estoque atualizado. Evento n8n será processado após a transação, se habilitado.";
        } else {
            mensagem = "Pedido salvo. Confira as observações de estoque; n8n é opcional.";
        }
        return new PedidoCriadoResponse(idPedido, agora, resumo, List.copyOf(itensResposta),
                (int) quantidadeTotalLong, valorTotal, mensagem, true, estoqueAtualizado,
                List.copyOf(observacoes));
    }

    //O token da sessão só pode consultar pedidos pertencentes ao próprio cliente associado.

    @Transactional(readOnly = true)
    public PedidoCriadoResponse buscarPedido(String authorization, Integer pedidoId) {
        Integer idCliente = sessoes.exigirCliente(authorization);
        Pedido pedido = pedidos.findByPedidoIdAndIdCliente(pedidoId, idCliente).orElse(null);
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Pedido não encontrado para o cliente desta conversa.");
        }
        ClienteResumoDto resumo = clienteService.buscarResumo(idCliente);
        List<ItemPedido> itens = itensPedido.findAllByPedidoId(pedidoId);
        Map<Integer, Produto> produtosPorId = new HashMap<>();
        Set<Integer> idsProdutos = new HashSet<>();

        // Coleta os IDs de produtos associados ao pedido consultado.
        
        for (int i = 0; i < itens.size(); i++) {
            ItemPedido item = itens.get(i);
            idsProdutos.add(item.getProdutoId());
        }
        List<Produto> produtosEncontrados = produtos.findAllById(idsProdutos);

        // Monta o mapa de nomes e atributos dos produtos do pedido consultado.
        
        for (int i = 0; i < produtosEncontrados.size(); i++) {
            Produto produto = produtosEncontrados.get(i);
            produtosPorId.put(produto.getProdutoId(), produto);
        }
        List<ItemPedidoCriadoDto> responseItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int quantidadeTotal = 0;

        // Recompõe o DTO de resposta com os itens que vieram do banco.
        
        for (int i = 0; i < itens.size(); i++) {
            ItemPedido item = itens.get(i);
            Produto produto = produtosPorId.get(item.getProdutoId());
            String nome;
            if (produto == null) {
                nome = "Produto " + item.getProdutoId();
            } else {
                nome = produto.getNome();
            }
            BigDecimal subtotal = item.getValorUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))
                    .setScale(2, RoundingMode.HALF_UP);
            responseItems.add(new ItemPedidoCriadoDto(
                    item.getProdutoId(), nome, item.getQuantidade(), item.getValorUnitario(), subtotal));
            total = total.add(subtotal);
            quantidadeTotal += item.getQuantidade();
        }
        return new PedidoCriadoResponse(pedido.getPedidoId(), pedido.getDataPedido(), resumo,
                List.copyOf(responseItems), quantidadeTotal, total.setScale(2, RoundingMode.HALF_UP),
                "Pedido encontrado no banco de dados.", null, null, List.of());
    }

    
    private Integer inserirCabecalhoComIdSeguro(Integer idCliente, OffsetDateTime dataPedido) {
        jdbcTemplate.execute("LOCK TABLE public.pedido IN EXCLUSIVE MODE");
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(pedido_id), 0) FROM public.pedido", Long.class);
        if (maxId == null) maxId = 0L;
        String sequencia = jdbcTemplate.queryForObject(
                "SELECT pg_get_serial_sequence('public.pedido', 'pedido_id')", String.class);
        if (sequencia != null && !sequencia.isBlank()) {
            String semAspas = sequencia.replace("\"", "");
            int ultimoPonto = semAspas.lastIndexOf('.');
            String esquemaSequencia;
            String nomeSequencia;
            if (ultimoPonto >= 0) {
                esquemaSequencia = semAspas.substring(0, ultimoPonto);
                nomeSequencia = semAspas.substring(ultimoPonto + 1);
            } else {
                esquemaSequencia = "public";
                nomeSequencia = semAspas;
            }
            Long valorAtualSequencia = jdbcTemplate.query(
                    "SELECT last_value FROM pg_sequences WHERE schemaname = ? AND sequencename = ?",
                    new ResultSetExtractor<Long>() {
                        @Override
                        public Long extractData(ResultSet resultado) throws SQLException {
                            if (resultado.next()) {
                                return resultado.getObject("last_value", Long.class);
                            }
                            return null;
                        }
                    }, esquemaSequencia, nomeSequencia);
            if (maxId > 0 && (valorAtualSequencia == null || valorAtualSequencia <= maxId)) {
                jdbcTemplate.queryForObject("SELECT setval(CAST(? AS regclass), ?, true)", Long.class, sequencia, maxId);
            }
            return jdbcTemplate.queryForObject("""
                    INSERT INTO public.pedido (data_pedido, id_cliente)
                    VALUES (?, ?)
                    RETURNING pedido_id
                    """, Integer.class, Timestamp.from(dataPedido.toInstant()), idCliente);
        }
        long proximo = maxId + 1L;
        if (proximo > Integer.MAX_VALUE) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O campo pedido_id excedeu a capacidade de um número inteiro.");
        int id = (int) proximo;
        jdbcTemplate.update("INSERT INTO public.pedido (pedido_id, data_pedido, id_cliente) VALUES (?, ?, ?)",
                id, Timestamp.from(dataPedido.toInstant()), idCliente);
        return id;
    }
}
