package br.com.desafiogc.pedi_fruki_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pedido", schema = "public")
public class Pedido {
    @Id
    @Column(name = "pedido_id")
    private Integer pedidoId;

    @Column(name = "data_pedido", nullable = false)
    private OffsetDateTime dataPedido;

    @Column(name = "id_cliente")
    private Integer idCliente;
}
