package br.com.desafiogc.pedi_fruki_api.repository;

import br.com.desafiogc.pedi_fruki_api.model.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CidadeRepository extends JpaRepository<Cidade, Long> {

}
