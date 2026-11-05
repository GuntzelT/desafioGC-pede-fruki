package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.ClienteVendedorDto;
import br.com.desafiogc.pedi_fruki_api.dto.ClienteResumoDto;
import br.com.desafiogc.pedi_fruki_api.dto.PedidoResumoVendedorDto;
import br.com.desafiogc.pedi_fruki_api.dto.RecomendacaoDto;
import br.com.desafiogc.pedi_fruki_api.model.Cliente;
import br.com.desafiogc.pedi_fruki_api.repository.ClienteRepository;
import br.com.desafiogc.pedi_fruki_api.service.ClienteService;
import br.com.desafiogc.pedi_fruki_api.repository.PedidoRepository;
import br.com.desafiogc.pedi_fruki_api.model.Pedido;
import br.com.desafiogc.pedi_fruki_api.service.RecomendacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Endpoints de demonstração da tela do app vendedor. Em produção, proteger por login/perfil
 * de vendedor e validar a relação cliente-vendedor antes de disponibilizar estes dados.
 */
@RestController
@RequestMapping("/api/app-vendedor")
@RequiredArgsConstructor
public class AppVendedorController {
    private final ClienteRepository clientes;
    private final ClienteService clienteService;
    private final RecomendacaoService recomendacoes;
    private final PedidoRepository pedidos;

    @GetMapping("/clientes")
    public List<ClienteVendedorDto> listarClientes() {
        List<Cliente> encontrados = clientes.findTop250ByOrderByNomeAsc();
        List<ClienteVendedorDto> resposta = new ArrayList<>();
        // Monta uma lista de DTOs sem expor todos os dados de cada cliente.
        for (int indice = 0; indice < encontrados.size(); indice++) {
            Cliente cliente = encontrados.get(indice);
            resposta.add(new ClienteVendedorDto(cliente.getIdCliente(), cliente.getNome(),
                    cliente.getSegmento(), cliente.getSubsegmento()));
        }
        return resposta;
    }

    @GetMapping("/clientes/{id}")
    public ClienteResumoDto buscarCliente(@PathVariable Integer id) {
        return clienteService.buscarResumo(id);
    }

    @GetMapping("/clientes/{id}/recomendacoes")
    public List<RecomendacaoDto> recomendar(@PathVariable Integer id) {
        return recomendacoes.recomendar(id);
    }

    /** Histórico de pedidos (não é histórico de entrega de notificações). */
    @GetMapping("/clientes/{id}/pedidos-recentes")
    public List<PedidoResumoVendedorDto> pedidosRecentes(@PathVariable Integer id) {
        if (!clientes.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Cliente não encontrado");
        }
        List<PedidoResumoVendedorDto> resposta = new ArrayList<>();
        List<Pedido> encontrados = pedidos.findByIdClienteOrderByDataPedidoDesc(id);
        // Mantém o limite de 20 pedidos sem usar for-each nem Streams.
        for (int indice = 0; indice < encontrados.size(); indice++) {
            Pedido pedido = encontrados.get(indice);
            if (resposta.size() >= 20) break;
            resposta.add(new PedidoResumoVendedorDto(pedido.getPedidoId(), pedido.getDataPedido()));
        }
        return resposta;
    }
}
