package br.com.desafiogc.pedi_fruki_api.model;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pedido", schema = "public")
public class Pedido {

    private static final Object AccessLevel = ;
    @Id
    private Integer idPedido;

    private OffsetDateTime dataPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;
}