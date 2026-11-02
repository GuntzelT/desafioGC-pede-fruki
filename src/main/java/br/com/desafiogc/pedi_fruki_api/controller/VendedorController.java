package br.com.desafiogc.pedi_fruki_api.controller;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendedor")
@RequiredArgsConstructor
public class VendedorController {
    private final VendedorService service;

    @GetMapping("/{idHierarquia}")
    public VendedorDto buscar(@PathVariable Integer idHierarquia) {
        return service.buscarVendedor(idHierarquia);
    }

    @GetMapping("/{idHierarquia}/clientes")
    public List<ClienteVendedorDto> carteira(@PathVariable Integer idHierarquia) {
        return service.listarCarteira(idHierarquia);
    }
}