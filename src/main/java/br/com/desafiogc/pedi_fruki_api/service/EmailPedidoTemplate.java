package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.ItemPedidoCriadoDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Monta o conteúdo do e-mail de confirmação (HTML e texto simples).
 * HTML com estilos inline e tabelas, que é o formato mais compatível com Gmail e Outlook.
 * Todo texto vindo do banco passa por escaparHtml(), para nunca virar HTML.
 */
final class EmailPedidoTemplate {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final String COR_PRINCIPAL = "#026138";

    private EmailPedidoTemplate() {
    }

    static String assunto(PedidoConfirmadoEvent evento) {
        return "Pede Fruki - pedido #" + evento.pedidoId() + " confirmado";
    }

    static String html(PedidoConfirmadoEvent evento) {
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"background:#f4f4f4;padding:24px 12px;font-family:Arial,Helvetica,sans-serif;color:#222222\">");
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"max-width:620px;margin:0 auto;background:#ffffff;border-radius:8px\">");

        // Cabeçalho
        h.append("<tr><td style=\"background:").append(COR_PRINCIPAL)
                .append(";color:#ffffff;padding:22px 24px;border-radius:8px 8px 0 0\">");
        h.append("<div style=\"font-size:12px;letter-spacing:1px;text-transform:uppercase\">Pede Fruki</div>");
        h.append("<div style=\"font-size:24px;font-weight:bold;margin-top:4px\">Pedido #")
                .append(evento.pedidoId()).append(" confirmado</div>");
        h.append("</td></tr>");

        // Dados do pedido
        h.append("<tr><td style=\"padding:20px 24px 4px 24px;font-size:14px;line-height:22px\">");
        h.append("<div><strong>Cliente:</strong> ").append(escaparHtml(nomeCliente(evento))).append("</div>");
        if (evento.dataPedido() != null) {
            h.append("<div><strong>Data:</strong> ")
                    .append(evento.dataPedido().atZoneSameInstant(FUSO).format(DATA_HORA)).append("</div>");
        }
        h.append("</td></tr>");

        // Itens
        h.append("<tr><td style=\"padding:12px 24px\">");
        List<ItemPedidoCriadoDto> itens = evento.itens();
        if (itens != null && !itens.isEmpty()) {
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px\">");
            h.append("<tr style=\"background:#fafafa;color:#666666;font-size:12px;text-transform:uppercase\">")
                    .append("<td style=\"padding:8px;border-bottom:2px solid ").append(COR_PRINCIPAL).append("\">Produto</td>")
                    .append("<td align=\"right\" style=\"padding:8px;border-bottom:2px solid ").append(COR_PRINCIPAL).append("\">Fardos</td>")
                    .append("<td align=\"right\" style=\"padding:8px;border-bottom:2px solid ").append(COR_PRINCIPAL).append("\">Preço unit.</td>")
                    .append("<td align=\"right\" style=\"padding:8px;border-bottom:2px solid ").append(COR_PRINCIPAL).append("\">Subtotal</td>")
                    .append("</tr>");
            for (int i = 0; i < itens.size(); i++) {
                ItemPedidoCriadoDto item = itens.get(i);
                h.append("<tr>")
                        .append("<td style=\"padding:10px 8px;border-bottom:1px solid #eeeeee\">")
                        .append(escaparHtml(nomeProduto(item))).append("</td>")
                        .append("<td align=\"right\" style=\"padding:10px 8px;border-bottom:1px solid #eeeeee\">")
                        .append(item.quantidade()).append("</td>")
                        .append("<td align=\"right\" style=\"padding:10px 8px;border-bottom:1px solid #eeeeee\">")
                        .append(moeda(item.valorUnitario())).append("</td>")
                        .append("<td align=\"right\" style=\"padding:10px 8px;border-bottom:1px solid #eeeeee\">")
                        .append(moeda(item.subtotal())).append("</td>")
                        .append("</tr>");
            }
            h.append("</table>");
        }
        h.append("</td></tr>");

        // Totais
        h.append("<tr><td style=\"padding:4px 24px 20px 24px\">");
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"background:#fafafa;border-radius:6px\"><tr>");
        h.append("<td style=\"padding:14px 16px;font-size:14px\"><strong>")
                .append(evento.quantidadeFardos()).append(" fardos</strong> no pedido</td>");
        h.append("<td align=\"right\" style=\"padding:14px 16px;font-size:20px;font-weight:bold;color:")
                .append(COR_PRINCIPAL).append("\">").append(moeda(evento.valorTotal())).append("</td>");
        h.append("</tr></table></td></tr>");

        // Rodapé
        h.append("<tr><td style=\"padding:14px 24px 22px 24px;font-size:12px;color:#888888;border-top:1px solid #eeeeee\">");
        h.append("Pedido registrado no sistema Pede Fruki. Os valores seguem a tabela de preços do perfil do cliente. ");
        h.append("Esta mensagem é um e-mail de demonstração.");
        h.append("</td></tr>");

        h.append("</table></div>");
        return h.toString();
    }

    static String texto(PedidoConfirmadoEvent evento) {
        StringBuilder t = new StringBuilder();
        t.append("Pede Fruki - Pedido #").append(evento.pedidoId()).append(" confirmado\n\n");
        t.append("Cliente: ").append(nomeCliente(evento)).append("\n");
        if (evento.dataPedido() != null) {
            t.append("Data: ").append(evento.dataPedido().atZoneSameInstant(FUSO).format(DATA_HORA)).append("\n");
        }
        List<ItemPedidoCriadoDto> itens = evento.itens();
        if (itens != null && !itens.isEmpty()) {
            t.append("\nItens:\n");
            for (int i = 0; i < itens.size(); i++) {
                ItemPedidoCriadoDto item = itens.get(i);
                t.append("- ").append(nomeProduto(item)).append(": ")
                        .append(item.quantidade()).append(" fardos x ").append(moedaTexto(item.valorUnitario()))
                        .append(" = ").append(moedaTexto(item.subtotal())).append("\n");
            }
        }
        t.append("\nTotal: ").append(evento.quantidadeFardos()).append(" fardos - ")
                .append(moedaTexto(evento.valorTotal())).append("\n\n");
        t.append("Esta mensagem é um e-mail de demonstração.\n");
        return t.toString();
    }

    private static String nomeCliente(PedidoConfirmadoEvent evento) {
        return evento.clienteNome() == null ? "Cliente não informado" : evento.clienteNome();
    }

    private static String nomeProduto(ItemPedidoCriadoDto item) {
        return item.nomeProduto() == null ? "Produto " + item.produtoId() : item.nomeProduto();
    }

    private static String moeda(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        return formato.format(valor.setScale(2, RoundingMode.HALF_UP));
    }

    private static String moedaTexto(BigDecimal valor) {
        return moeda(valor).replace('\u00a0', ' ');
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}