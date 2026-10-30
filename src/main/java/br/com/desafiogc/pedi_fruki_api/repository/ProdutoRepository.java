package br.com.desafiogc.pedi_fruki_api.repository;

import br.com.desafiogc.pedi_fruki_api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {
    List<Produto> findByAtivoTrueOrderByNomeAsc();
}
