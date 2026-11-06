package br.com.desafiogc.pedi_fruki_api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Datas comerciais calculadas localmente (sem API externa):
 * - Carnaval: terça-feira de Carnaval = Páscoa - 47 dias.
 * - Verão (hemisfério sul): de 21/12 a 20/03.
 */
public final class CalendarioComercialService {

    private CalendarioComercialService() {
    }

    /** Algoritmo de Meeus/Jones/Butcher para a Páscoa gregoriana. */
    public static LocalDate pascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    public static LocalDate tercaDeCarnaval(int ano) {
        return pascoa(ano).minusDays(47);
    }

    /** Dias até a próxima terça de Carnaval (0 se for hoje). Passado o Carnaval, aponta para o do ano seguinte. */
    public static long diasAteCarnaval(LocalDate hoje) {
        LocalDate carnaval = tercaDeCarnaval(hoje.getYear());
        if (carnaval.isBefore(hoje)) {
            carnaval = tercaDeCarnaval(hoje.getYear() + 1);
        }
        return ChronoUnit.DAYS.between(hoje, carnaval);
    }

    public static boolean emVerao(LocalDate hoje) {
        int mes = hoje.getMonthValue();
        int dia = hoje.getDayOfMonth();
        return (mes == 12 && dia >= 21) || mes == 1 || mes == 2 || (mes == 3 && dia <= 20);
    }
}