package br.com.desafiogc.pedi_fruki_api.repository;

import br.com.desafiogc.pedi_fruki_api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HierarquiaRepository extends JpaRepository<Hierarquia, Integer> {

    Optional<Hierarquia> findByCodRepresentante(Integer codRepresentante);
}