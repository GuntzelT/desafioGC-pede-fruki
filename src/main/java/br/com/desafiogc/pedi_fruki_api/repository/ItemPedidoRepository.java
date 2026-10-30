package br.com.desafiogc.pedi_fruki_api.repository;

import br.com.desafiogc.pedi_fruki_api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

//itempedido possui PK (pedido_id, produto_id) no bacno. As consultas de histórico ligam item_pedido a pedido por pedido_id.

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, ItemPedidoId> {

    List<ItemPedido> findAllByPedidoId(Integer pedidoId);

    //Soma a quantidade comprada de cada produto pelo cliente selecionado.

    @Query(value = """
            SELECT ip.produto_id, SUM(ip.quantidade) AS quantidade_total
            FROM public.item_pedido ip
            JOIN public.pedido p ON p.pedido_id = ip.pedido_id
            WHERE p.id_cliente = :clienteId
            GROUP BY ip.produto_id
            """, nativeQuery = true)
    List<Object[]> resumirComprasDoCliente(@Param("clienteId") Integer clienteId);

    //Soma compras de outros clientes do mesmo segmento e subsegmento.

    @Query(value = """
            SELECT ip.produto_id, SUM(ip.quantidade) AS quantidade_total
            FROM public.item_pedido ip
            JOIN public.pedido p ON p.pedido_id = ip.pedido_id
            JOIN public.cliente c ON c.id_cliente = p.id_cliente
            WHERE p.id_cliente <> :clienteId
              AND LOWER(TRIM(COALESCE(c.segmento, ''))) = LOWER(TRIM(COALESCE(:segmento, '')))
              AND LOWER(TRIM(COALESCE(c.subsegmento, ''))) = LOWER(TRIM(COALESCE(:subsegmento, '')))
              AND TRIM(COALESCE(:segmento, '')) <> ''
              AND TRIM(COALESCE(:subsegmento, '')) <> ''
            GROUP BY ip.produto_id
            """, nativeQuery = true)
    List<Object[]> resumirComprasDoPerfil(
            @Param("clienteId") Integer clienteId,
            @Param("segmento") String segmento,
            @Param("subsegmento") String subsegmento
    );
}
