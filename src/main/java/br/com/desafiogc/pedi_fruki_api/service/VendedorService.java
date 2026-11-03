package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.ClienteVendedorDto;
import br.com.desafiogc.pedi_fruki_api.dto.VendedorDto;
import br.com.desafiogc.pedi_fruki_api.model.Cliente;
import br.com.desafiogc.pedi_fruki_api.model.Hierarquia;
import br.com.desafiogc.pedi_fruki_api.repository.ClienteRepository;
import br.com.desafiogc.pedi_fruki_api.repository.HierarquiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VendedorService {
    private final HierarquiaRepository hierarquias;
    private final ClienteRepository clientes;

    public VendedorDto buscarVendedor(Integer idHierarquia) {
        // Sem lambda: procura o vendedor e verifica o Optional explicitamente.
        Hierarquia vendedor = hierarquias.findById(idHierarquia).orElse(null);
        if (vendedor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Vendedor não encontrado: " + idHierarquia);
        }
        return new VendedorDto(vendedor.getIdHierarquia(),
                vendedor.getCodRepresentante(), vendedor.getNomeVendedor());
    }

    public List<ClienteVendedorDto> listarCarteira(Integer idHierarquia) {
        buscarVendedor(idHierarquia);
        List<Cliente> carteira = clientes.findByIdHierarquiaOrderByNomeAsc(idHierarquia);
        List<ClienteVendedorDto> resposta = new ArrayList<>();
        // Usa apenas for tradicional, sem Stream/map/toList.
        for (int indice = 0; indice < carteira.size(); indice++) {
            Cliente cliente = carteira.get(indice);
            resposta.add(new ClienteVendedorDto(cliente.getIdCliente(),
                    cliente.getNome(), cliente.getSegmento(), cliente.getSubsegmento()));
        }
        return resposta;
    }
}
