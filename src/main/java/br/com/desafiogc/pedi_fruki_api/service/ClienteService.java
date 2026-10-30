package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.model.*;
import br.com.desafiogc.pedi_fruki_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clientes;
    private final CidadeRepository cidades;

    public ClienteResumoDto buscarResumo(Integer id) {
        Cliente cliente = clientes.findById(id).orElse(null);
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado: " + id);
        }
        Cidade cidade = null;
        if (cliente.getIdCidade() != null) {
            cidade = cidades.findById(cliente.getIdCidade()).orElse(null);
        }
        String nomeCidade = null;
        String ufCidade = null;
        if (cidade != null) {
            nomeCidade = cidade.getNome();
            ufCidade = cidade.getUf();
        }
        return new ClienteResumoDto(
                cliente.getIdCliente(), cliente.getNome(), cliente.getSegmento(), cliente.getSubsegmento(),
                nomeCidade, ufCidade, cliente.getUltimaCompra());
    }
}
