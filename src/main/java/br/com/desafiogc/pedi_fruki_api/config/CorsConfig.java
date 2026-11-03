package br.com.desafiogc.pedi_fruki_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origensPermitidas;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String origensCsv) {
        String[] origens = origensCsv.split(",");
        List<String> validas = new ArrayList<>();
        // Percorre o array de URLs autorizadas com índice e length.
        for (int indice = 0; indice < origens.length; indice++) {
            String origem = origens[indice];
            String valor = origem.trim();
            if (!valor.isBlank()) {
                validas.add(valor);
            }
        }
        this.origensPermitidas = validas.toArray(new String[0]);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
