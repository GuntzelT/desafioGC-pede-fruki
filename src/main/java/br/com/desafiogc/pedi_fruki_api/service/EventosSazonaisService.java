package br.com.desafiogc.pedi_fruki_api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras comerciais sazonais calculadas localmente, sem API externa.
 * Inclui as quatro estações do ano no hemisfério sul e algumas datas comerciais
 * recorrentes. Feriados nacionais e estaduais oficiais são consultados à parte
 * pela Nager.Date. Em sobreposição, vale o maior sinal (não soma os pontos).
 *
 * Os pontos são premissas comerciais editáveis e devem ser calibrados com vendas reais.
 */
public final class EventosSazonaisService {

    private enum Tipo { JANELA_FIXA, CARNAVAL, PASCOA, DIA_DOS_PAIS }

    private static final class Evento {
        final String nome;
        final Tipo tipo;
        final int mes;
        final int dia;
        final int mesFim;
        final int diaFim;
        final int antecedenciaDias;
        final int[] pontos;

        Evento(String nome, Tipo tipo, int mes, int dia, int mesFim, int diaFim,
               int antecedenciaDias, int[] pontos) {
            this.nome = nome;
            this.tipo = tipo;
            this.mes = mes;
            this.dia = dia;
            this.mesFim = mesFim;
            this.diaFim = diaFim;
            this.antecedenciaDias = antecedenciaDias;
            this.pontos = pontos;
        }
    }

    public record Resultado(int pontos, String motivo) {}

    private static final List<Evento> TABELA = montarTabela();

    private EventosSazonaisService() {
    }

    private static List<Evento> montarTabela() {
        List<Evento> eventos = new ArrayList<>();
        // Ordem dos pontos: ALCOOLICA, REFRIGERANTE, AGUA, OUTRAS.
        // Estações aproximadas do calendário astronômico do hemisfério sul.
        eventos.add(new Evento("Verão", Tipo.JANELA_FIXA, 12, 21, 3, 20, 0,
                new int[]{8, 8, 10, 4}));
        eventos.add(new Evento("Outono", Tipo.JANELA_FIXA, 3, 21, 6, 20, 0,
                new int[]{5, 6, 6, 5}));
        eventos.add(new Evento("Inverno", Tipo.JANELA_FIXA, 6, 21, 9, 22, 0,
                new int[]{4, 3, 2, 7}));
        eventos.add(new Evento("Primavera", Tipo.JANELA_FIXA, 9, 23, 12, 20, 0,
                new int[]{6, 7, 7, 5}));

        // Sazonalidades comerciais mantidas do calendário anterior.
        eventos.add(new Evento("Carnaval", Tipo.CARNAVAL, 0, 0, 0, 0, 14,
                new int[]{10, 5, 5, 2}));
        eventos.add(new Evento("Natal e Réveillon", Tipo.JANELA_FIXA, 12, 15, 12, 31, 0,
                new int[]{10, 8, 6, 4}));
        eventos.add(new Evento("Páscoa", Tipo.PASCOA, 0, 0, 0, 0, 7,
                new int[]{6, 6, 6, 3}));
        eventos.add(new Evento("Dia dos Pais", Tipo.DIA_DOS_PAIS, 0, 0, 0, 0, 7,
                new int[]{8, 5, 3, 2}));
        return eventos;
    }

    /** Retorna o melhor sinal sazonal para a categoria na data informada. */
    public static Resultado melhorPara(CategoriaBebidaService categoria, LocalDate hoje) {
        int melhorPontos = 0;
        String melhorMotivo = null;

        for (int indice = 0; indice < TABELA.size(); indice++) {
            Evento evento = TABELA.get(indice);
            int pontos = evento.pontos[categoria.ordinal()];
            if (pontos <= melhorPontos) {
                continue;
            }

            if (evento.tipo == Tipo.JANELA_FIXA) {
                if (dentroDaJanela(evento, hoje)) {
                    melhorPontos = pontos;
                    String tipoPeriodo = evento.nome.equals("Verão")
                            || evento.nome.equals("Outono")
                            || evento.nome.equals("Inverno")
                            || evento.nome.equals("Primavera")
                            ? "estação de " : "período de ";
                    melhorMotivo = tipoPeriodo + evento.nome + ": favorece " + categoria.rotulo();
                }
            } else {
                long dias = diasAteProximaData(evento, hoje);
                if (dias <= evento.antecedenciaDias) {
                    melhorPontos = pontos;
                    String quando = dias == 0 ? "é hoje" : "em " + dias + " dia(s)";
                    melhorMotivo = evento.nome + " (" + quando + "): favorece " + categoria.rotulo();
                }
            }
        }

        return new Resultado(melhorPontos, melhorMotivo);
    }

    private static boolean dentroDaJanela(Evento evento, LocalDate hoje) {
        int ano = hoje.getYear();
        LocalDate inicio = LocalDate.of(ano, evento.mes, evento.dia);
        LocalDate fim = LocalDate.of(ano, evento.mesFim, evento.diaFim);

        if (fim.isBefore(inicio)) {
            // Janela que cruza o ano: por exemplo, verão de 21/12 a 20/03.
            return !hoje.isBefore(inicio) || !hoje.isAfter(fim);
        }
        return !hoje.isBefore(inicio) && !hoje.isAfter(fim);
    }

    private static long diasAteProximaData(Evento evento, LocalDate hoje) {
        LocalDate data = dataDoEvento(evento, hoje.getYear());
        if (data.isBefore(hoje)) {
            data = dataDoEvento(evento, hoje.getYear() + 1);
        }
        return ChronoUnit.DAYS.between(hoje, data);
    }

    private static LocalDate dataDoEvento(Evento evento, int ano) {
        switch (evento.tipo) {
            case CARNAVAL:
                return CalendarioComercialService.tercaDeCarnaval(ano);
            case PASCOA:
                return CalendarioComercialService.pascoa(ano);
            case DIA_DOS_PAIS:
                return LocalDate.of(ano, 8, 1)
                        .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
            default:
                return LocalDate.of(ano, evento.mes, evento.dia);
        }
    }
}
