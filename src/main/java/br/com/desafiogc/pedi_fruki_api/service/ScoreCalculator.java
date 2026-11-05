package br.com.desafiogc.pedi_fruki_api.service;

import org.springframework.stereotype.Component;

/**
 * Motor determinístico de pontuação. A soma máxima é 100 pontos:
 * histórico (40), perfil comercial (40), clima (10) e calendário (10).
 * Promoções não fazem parte das regras deste projeto.
 */
@Component
public class ScoreCalculator {
    public static final int MAX_HISTORICO = 40;
    public static final int MAX_PERFIL = 40;
    public static final int MAX_CLIMA = 10;
    public static final int MAX_CALENDARIO = 10;

    public Pontuacao calcular(long comprasCliente, long maiorHistorico,
                              long comprasPerfil, long maiorPerfil,
                              int pontosClima, int pontosCalendario) {
        int historico = normalizar(comprasCliente, maiorHistorico, MAX_HISTORICO);
        int perfil = normalizar(comprasPerfil, maiorPerfil, MAX_PERFIL);
        int clima = limitar(pontosClima, MAX_CLIMA);
        int calendario = limitar(pontosCalendario, MAX_CALENDARIO);
        return new Pontuacao(historico, perfil, clima, calendario);
    }

    private int normalizar(long quantidade, long maiorQuantidade, int pesoMaximo) {
        if (quantidade <= 0 || maiorQuantidade <= 0) {
            return 0;
        }
        double proporcao = Math.min(1.0, (double) quantidade / maiorQuantidade);
        return (int) Math.round(pesoMaximo * proporcao);
    }

    private int limitar(int pontos, int maximo) {
        return Math.max(0, Math.min(maximo, pontos));
    }

    public record Pontuacao(int historico, int perfil, int clima, int calendario) {
        public int total() {
            return historico + perfil + clima + calendario;
        }
    }
}
