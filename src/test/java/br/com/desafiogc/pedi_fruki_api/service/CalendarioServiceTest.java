package br.com.desafiogc.pedi_fruki_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarioServiceTest {

    private CalendarioService calendario;

    @BeforeEach
    void configurar() {
        calendario = new CalendarioService(RestClient.builder());
    }

    @Test
    void deveAceitarFeriadoNacionalParaQualquerUf() {
        Map<String, Object> feriado = Map.of(
                "global", true,
                "types", List.of("Public")
        );

        assertTrue(calendario.aplicaAoEstadoOuPaisTodo(feriado, "RS"));
        assertTrue(calendario.aplicaAoEstadoOuPaisTodo(feriado, "SC"));
    }

    @Test
    void deveAceitarFeriadoEstadualSomenteParaUfCorrespondente() {
        Map<String, Object> feriadoRioGrandeDoSul = Map.of(
                "global", false,
                "counties", List.of("BR-RS"),
                "types", List.of("Public")
        );

        assertTrue(calendario.aplicaAoEstadoOuPaisTodo(feriadoRioGrandeDoSul, "RS"));
        assertFalse(calendario.aplicaAoEstadoOuPaisTodo(feriadoRioGrandeDoSul, "SC"));
    }

    @Test
    void semUfDeveConsiderarSomenteFeriadosNacionais() {
        Map<String, Object> nacional = Map.of("global", true);
        Map<String, Object> estadual = Map.of(
                "global", false,
                "counties", List.of("BR-RS")
        );

        assertTrue(calendario.aplicaAoEstadoOuPaisTodo(nacional, null));
        assertFalse(calendario.aplicaAoEstadoOuPaisTodo(estadual, null));
    }

    @Test
    void deveAceitarSomenteTipoPublicQuandoTiposVieremInformados() {
        Map<String, Object> feriadoPublico = Map.of("types", List.of("Public"));
        Map<String, Object> pontoFacultativo = Map.of("types", List.of("Optional"));

        assertTrue(calendario.ehFeriadoPublico(feriadoPublico));
        assertFalse(calendario.ehFeriadoPublico(pontoFacultativo));
    }
}
    