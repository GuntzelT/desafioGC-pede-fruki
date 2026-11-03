package br.com.desafiogc.pedi_fruki_api.model;

import br.valueobject.PerfilComercial;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Valida a escolha do preço mantida diretamente na entidade Produto. */
class ProdutoPrecoTest {

    @Test
    void precoEEscolhidoPeloPerfilSemPromocoes() throws Exception {
        Produto produto = new Produto();
        Field campoBase = Produto.class.getDeclaredField("precoBase");
        Field campoMinimo = Produto.class.getDeclaredField("precoMinimo");
        campoBase.setAccessible(true);
        campoMinimo.setAccessible(true);
        campoBase.set(produto, new BigDecimal("10.00"));
        campoMinimo.set(produto, new BigDecimal("8.00"));

        assertEquals(new BigDecimal("8.00"),
                produto.precoPara(new PerfilComercial("Atacado", "Mercado")));
        assertEquals(new BigDecimal("10.00"),
                produto.precoPara(new PerfilComercial("Varejo", "Mercado")));
    }
}
