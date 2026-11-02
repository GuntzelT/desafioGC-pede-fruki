package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.model.*;
import br.com.desafiogc.pedi_fruki_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VendedorService {
    private final HierarquiaRepository hierarquias;
    private final ClienteRepository clientes;

    public VendedorDto buscarVendedor(Integer idHierarquia) {
        Hierarquia h = hierarquias.findById(idHierarquia)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vendedor não encontrado: " + idHierarquia));
        return new VendedorDto(h.getIdHierarquia(), h.getCodRepresentante(), h.getNomeVendedor());
    }

    public List<ClienteVendedorDto> listarCarteira(Integer idHierarquia) {
        buscarVendedor(idHierarquia); // garante 404 se o vendedor não existir

        return clientes.findByIdHierarquiaOrderByNomeAsc(idHierarquia).stream()
                .map(c -> new ClienteVendedorDto(
                        c.getIdCliente(), c.getNome(), c.getSegmento(), c.getSubsegmento()))
                .toList();
    }
}