package br.com.desafiogc.pedi_fruki_api.repository;

import br.com.desafiogc.pedi_fruki_api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    List<Cliente> findTop250ByOrderByNomeAsc();

    /** Busca por CNPJ ignorando pontos, barras, hífens e espaços no valor armazenado. */
    @Query(value = """
            SELECT c.*
            FROM public.cliente c
            WHERE regexp_replace(COALESCE(c.cnpj::text, ''), '[^0-9]', '', 'g') = :cnpj
            LIMIT 1
            """, nativeQuery = true)
    Optional<Cliente> findPorCnpjNormalizado(@Param("cnpj") String cnpj);
}
