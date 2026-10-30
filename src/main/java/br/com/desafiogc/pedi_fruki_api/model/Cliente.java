package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import br.valueobject.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "cliente", schema = "public")
public class Cliente {
    @Id
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "segmento")
    private String segmento;

    @Column(name = "subsegmento")
    private String subsegmento;

    @Column(name = "numero_celular")
    private String numeroCelular;

    @Column(name = "cep")
    private String cep;

    @Column(name = "ultima_compra")
    private OffsetDateTime ultimaCompra;

    @Column(name = "id_hierarquia")
    private Integer idHierarquia;

    @Column(name = "id_cidade")
    private Integer idCidade;

    //Converte o CNPJ do banco para um valor validado
    public Cnpj getCnpjValor() {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        return new Cnpj(cnpj);
    }

    //Constrói o perfil de recomendação sem criar colunas adicionais no banco
    public PerfilComercial getPerfilComercial() {
        return new PerfilComercial(segmento, subsegmento);
    }
}
