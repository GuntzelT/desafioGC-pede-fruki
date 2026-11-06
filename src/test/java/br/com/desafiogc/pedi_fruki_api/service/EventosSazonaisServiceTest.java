package br.com.desafiogc.pedi_fruki_api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventosSazonaisServiceTest {

    @Test
    void deveReconhecerVeraoNoInicioDoAno() {
        EventosSazonaisService.Resultado resultado = EventosSazonaisService.melhorPara(
                CategoriaBebidaService.AGUA, LocalDate.of(2027, 1, 15));

        assertEquals(10, resultado.pontos());
        assertTrue(resultado.motivo().contains("Verão"));
    }

    @Test
    void deveReconhecerOutono() {
        EventosSazonaisService.Resultado resultado = EventosSazonaisService.melhorPara(
                CategoriaBebidaService.OUTRAS, LocalDate.of(2027, 4, 15));

        assertEquals(5, resultado.pontos());
        assertTrue(resultado.motivo().contains("Outono"));
    }

    @Test
    void deveReconhecerInverno() {
        EventosSazonaisService.Resultado resultado = EventosSazonaisService.melhorPara(
                CategoriaBebidaService.OUTRAS, LocalDate.of(2027, 7, 15));

        assertEquals(7, resultado.pontos());
        assertTrue(resultado.motivo().contains("Inverno"));
    }

    @Test
    void deveReconhecerPrimavera() {
        EventosSazonaisService.Resultado resultado = EventosSazonaisService.melhorPara(
                CategoriaBebidaService.REFRIGERANTE, LocalDate.of(2027, 10, 15));

        assertEquals(7, resultado.pontos());
        assertTrue(resultado.motivo().contains("Primavera"));
    }

    @Test
    void deveReconhecerVeraoTambemEmDezembro() {
        EventosSazonaisService.Resultado resultado = EventosSazonaisService.melhorPara(
                CategoriaBebidaService.AGUA, LocalDate.of(2027, 12, 25));

        assertTrue(resultado.pontos() >= 8);
        assertTrue(resultado.motivo().contains("Verão")
                || resultado.motivo().contains("Natal e Réveillon"));
    }
}
