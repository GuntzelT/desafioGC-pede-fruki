package br.com.desafiogc.pedi_fruki_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Consulta Open-Meteo. Falhas da API externa são tratadas como contexto indisponível. */
@Service
@RequiredArgsConstructor
public class ClimaService {
    private final RestClient.Builder builder;

    public Map<?, ?> previsao(String cidade, String uf) {
        try {
            RestClient http = builder.build();
            Map<?, ?> geo = http.get().uri(UriComponentsBuilder
                    .fromUriString("https://geocoding-api.open-meteo.com/v1/search")
                    .queryParam("name", cidade).queryParam("count", 10)
                    .queryParam("language", "pt").queryParam("format", "json").build().toUri())
                    .retrieve().body(Map.class);
            if (geo == null || !(geo.get("results") instanceof List<?> results) || results.isEmpty()) {
                return indisponivel("Cidade não encontrada na API de geocodificação");
            }

            Map<?, ?> local = escolherLocal(results, uf);
            if (local == null) return indisponivel("Não foi possível identificar a cidade e UF na API meteorológica");

            Object latitude = local.get("latitude");
            Object longitude = local.get("longitude");
            Map<?, ?> previsao = http.get().uri(UriComponentsBuilder
                    .fromUriString("https://api.open-meteo.com/v1/forecast")
                    .queryParam("latitude", latitude).queryParam("longitude", longitude)
                    .queryParam("current", "temperature_2m,precipitation,weather_code")
                    .queryParam("daily", "temperature_2m_max,precipitation_sum")
                    .queryParam("timezone", "America/Sao_Paulo").build().toUri())
                    .retrieve().body(Map.class);
            if (previsao == null) {
                return indisponivel("A API meteorológica não retornou dados");
            }
            return previsao;
        } catch (Exception e) {
            return indisponivel("Não foi possível consultar o clima agora");
        }
    }

    private Map<?, ?> escolherLocal(List<?> resultados, String uf) {
        String estadoEsperado = nomeEstado(uf);
        // Procura uma cidade brasileira com a UF esperada, usando índice numérico.
        for (int indice = 0; indice < resultados.size(); indice++) {
            Object item = resultados.get(indice);
            if (!(item instanceof Map<?, ?> local)) continue;
            Object paisObj = local.get("country_code");
            Object estadoObj = local.get("admin1");
            String pais = "";
            if (paisObj != null) {
                pais = paisObj.toString();
            }
            String estado = "";
            if (estadoObj != null) {
                estado = estadoObj.toString();
            }
            if (!"BR".equalsIgnoreCase(pais)) continue;
            // Compara nomes de estado ignorando diferenças de acentuação e caixa.
            if (estadoEsperado.isBlank() || normalizarEstado(estado).equals(normalizarEstado(estadoEsperado))) {
                return local;
            }
        }
        return null;
    }

    /** Normalização simples de nomes, sem criar um terceiro Value Object. */
    private String normalizarEstado(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> indisponivel(String motivo) {
        return Map.of("disponivel", false, "motivo", motivo);
    }

    private String nomeEstado(String uf) {
        if (uf == null) return "";
        String sigla = uf.trim().toUpperCase(Locale.ROOT);
        switch (sigla) {
            case "AC": return "Acre";
            case "AL": return "Alagoas";
            case "AP": return "Amapá";
            case "AM": return "Amazonas";
            case "BA": return "Bahia";
            case "CE": return "Ceará";
            case "DF": return "Distrito Federal";
            case "ES": return "Espírito Santo";
            case "GO": return "Goiás";
            case "MA": return "Maranhão";
            case "MT": return "Mato Grosso";
            case "MS": return "Mato Grosso do Sul";
            case "MG": return "Minas Gerais";
            case "PA": return "Pará";
            case "PB": return "Paraíba";
            case "PR": return "Paraná";
            case "PE": return "Pernambuco";
            case "PI": return "Piauí";
            case "RJ": return "Rio de Janeiro";
            case "RN": return "Rio Grande do Norte";
            case "RS": return "Rio Grande do Sul";
            case "RO": return "Rondônia";
            case "RR": return "Roraima";
            case "SC": return "Santa Catarina";
            case "SP": return "São Paulo";
            case "SE": return "Sergipe";
            case "TO": return "Tocantins";
            default: return uf;
        }
    }

}
