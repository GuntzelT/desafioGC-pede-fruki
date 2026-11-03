package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MapeamentoSchemaTest {
    @Test
    void hierarquiaUsaATabelaDoSqlNovo() {
        Table tabela = Hierarquia.class.getAnnotation(Table.class);
        assertEquals("hierarquia", tabela.name());
    }

    @Test
    void produtoEpedidoUsamNovosNomesDeColuna() throws Exception {
        Field produtoId = Produto.class.getDeclaredField("produtoId");
        Field pedidoId = Pedido.class.getDeclaredField("pedidoId");
        assertEquals("produto_id", produtoId.getAnnotation(Column.class).name());
        assertEquals("pedido_id", pedidoId.getAnnotation(Column.class).name());
    }

    @Test
    void itemPedidoPossuiIdentificadorComposto() {
        IdClass idClass = ItemPedido.class.getAnnotation(IdClass.class);
        assertNotNull(idClass);
        assertEquals(ItemPedidoId.class, idClass.value());
    }
}
