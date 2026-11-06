package br.com.desafiogc.pedi_fruki_api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabela de eventos sazonais. Para incluir ou ajustar um evento, edite SOMENTE a lista em montarTabela().
 * Os pontos são PREMISSAS COMERCIAIS (base fictícia): devem ser calibrados com pedidos reais.
 * Datas calculadas localmente, sem API externa. Em sobreposição vale o MAIOR valor (não soma).
 */
public final class EventosSazonaisService {

    private enum Tipo { JANELA_FIXA, DATA_FIXA, CARNAVAL, PASCOA, DIA_DOS_PAIS }

    private static final class Evento {
        final String nome;
        final Tipo tipo;
        final int mes, dia, mesFim, diaFim; // JANELA_FIXA usa início e fim; DATA_FIXA usa mes/dia
        final int antecedenciaDias;         // eventos de data: pontua nos N dias anteriores
        final String uf;                    // null = vale para todos os estados
        final int[] pontos;                 // ordem: ALCOOLICA, REFRIGERANTE, AGUA, OUTRAS

        Evento(String nome, Tipo tipo, int mes, int dia, int mesFim, int diaFim,
               int antecedenciaDias, String uf, int[] pontos) {
            this.nome = nome;
            this.tipo = tipo;
            this.mes = mes;
            this.dia = dia;
            this.mesFim = mesFim;
            this.diaFim = diaFim;
            this.antecedenciaDias = antecedenciaDias;
            this.uf = uf;
            this.pontos = pontos;
        }
    }

    public record Resultado(int pontos, String motivo) {}

    private static final List<Evento> TABELA = montarTabela();

    private EventosSazonaisService() {
    }

    private static List<Evento> montarTabela() {
        List<Evento> t = new ArrayList<>();
        //                      nome                    tipo               mes dia mFim dFim antec uf      álc refri água outras
        t.add(new Evento("Carnaval",            Tipo.CARNAVAL,      0,  0,  0,  0,  14, null, new int[]{10, 5, 5, 2}));
        t.add(new Evento("Verão e praia",       Tipo.JANELA_FIXA,  12, 21,  3, 20,   0, null, new int[]{ 8, 8, 10, 4}));
        t.add(new Evento("Natal e Réveillon",   Tipo.JANELA_FIXA,  12, 15, 12, 31,   0, null, new int[]{10, 8, 6, 4}));
        t.add(new Evento("Tiradentes",          Tipo.DATA_FIXA,     4, 21,  0,  0,   7, null, new int[]{ 8, 7, 5, 3}));
        t.add(new Evento("Dia do Trabalho",     Tipo.DATA_FIXA,     5,  1,  0,  0,   7, null, new int[]{ 8, 7, 5, 3}));
        t.add(new Evento("Independência",       Tipo.DATA_FIXA,     9,  7,  0,  0,   7, null, new int[]{ 8, 7, 5, 3}));
        t.add(new Evento("Semana Farroupilha",  Tipo.JANELA_FIXA,   9, 14,  9, 20,   0, "RS", new int[]{ 8, 7, 4, 2}));
        t.add(new Evento("Dia das Crianças",    Tipo.DATA_FIXA,    10, 12,  0,  0,   7, null, new int[]{ 2, 8, 3, 5}));
        t.add(new Evento("Páscoa",              Tipo.PASCOA,        0,  0,  0,  0,   7, null, new int[]{ 6, 6, 6, 3}));
        t.add(new Evento("Dia dos Pais",        Tipo.DIA_DOS_PAIS,  0,  0,  0,  0,   7, null, new int[]{ 8, 5, 3, 2}));
        t.add(new Evento("Inverno",             Tipo.JANELA_FIXA,   6, 21,  9, 22,   0, null, new int[]{ 4, 3, 2, 6}));
        return t;
    }

    /** Melhor sinal sazonal para a categoria em "hoje". uf pode ser null (eventos regionais não valem). */
    public static Resultado melhorPara(CategoriaBebidaService categoria, LocalDate hoje, String uf) {
        int melhorPontos = 0;
        String melhorMotivo = null;
        for (int i = 0; i < TABELA.size(); i++) {
            Evento evento = TABELA.get(i);
            if (evento.uf != null && !evento.uf.equalsIgnoreCase(uf == null ? "" : uf.trim())) {
                continue;
            }
            int pontos = evento.pontos[categoria.ordinal()];
            if (pontos <= melhorPontos) {
                continue;
            }
            if (evento.tipo == Tipo.JANELA_FIXA) {
                if (dentroDaJanela(evento, hoje)) {
                    melhorPontos = pontos;
                    melhorMotivo = "período de " + evento.nome + ": favorece " + categoria.rotulo();
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

    private static boolean dentroDaJanela(Evento e, LocalDate hoje) {
        int ano = hoje.getYear();
        LocalDate inicio = LocalDate.of(ano, e.mes, e.dia);
        LocalDate fim = LocalDate.of(ano, e.mesFim, e.diaFim);
        if (fim.isBefore(inicio)) { // janela que cruza o ano (ex.: verão)
            return !hoje.isBefore(inicio) || !hoje.isAfter(fim);
        }
        return !hoje.isBefore(inicio) && !hoje.isAfter(fim);
    }

    private static long diasAteProximaData(Evento e, LocalDate hoje) {
        LocalDate data = dataDoEvento(e, hoje.getYear());
        if (data.isBefore(hoje)) {
            data = dataDoEvento(e, hoje.getYear() + 1);
        }
        return ChronoUnit.DAYS.between(hoje, data);
    }

    private static LocalDate dataDoEvento(Evento e, int ano) {
        switch (e.tipo) {
            case CARNAVAL:
                return CalendarioComercialService.tercaDeCarnaval(ano);
            case PASCOA:
                return CalendarioComercialService.pascoa(ano);
            case DIA_DOS_PAIS: // 2º domingo de agosto
                return LocalDate.of(ano, 8, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
            default:
                return LocalDate.of(ano, e.mes, e.dia);
        }
    }
}