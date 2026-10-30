
package br.com.desafiogc.pedi_fruki_api.repository;

import br.com.desafiogc.pedi_fruki_api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    
    List<Pedido> findByIdClienteOrderByDataPedidoDesc(Integer idCliente);

    Optional<Pedido> findByPedidoIdAndIdCliente(Integer pedidoId, Integer idCliente);
}
