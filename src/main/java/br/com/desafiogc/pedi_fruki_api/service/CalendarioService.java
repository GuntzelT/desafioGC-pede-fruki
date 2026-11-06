package br.com.desafiogc.pedi_fruki_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

/**
 * Consulta feriados públicos nacionais e estaduais do Brasil na Nager.Date API v3.
 * Na v3, o campo "global" identifica feriados nacionais e "counties" informa
 * os códigos ISO 3166-2 das subdivisões estaduais (por exemplo, BR-RS).
 */
@Service
@RequiredArgsConstructor
public class CalendarioService {
    private final RestClient.Builder builder;

    public List<?> proximosFeriados(String uf) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        List<Map<?, ?>> todos = new ArrayList<>();
        int consultasBemSucedidas = 0;

        // Primeiro consulta o ano atual; na segunda volta, consulta o ano seguinte.
        int anoInicial = hoje.getYear();
        for (int ano = anoInicial; ano <= anoInicial + 1; ano++) {
            try {
                var uri = UriComponentsBuilder.fromUriString("https://date.nager.at/api/v3/PublicHolidays/" + ano + "/BR")
                        .build().toUri();
                Object resposta = builder.build().get().uri(uri).retrieve().body(Object.class);
                if (resposta instanceof List<?>) {
                    consultasBemSucedidas++;
                    List<?> lista = (List<?>) resposta;
                    // Uma lista possui índices de 0 até size() - 1.
                    for (int indice = 0; indice < lista.size(); indice++) {
                        Object item = lista.get(indice);
                        if (item instanceof Map<?, ?>) {
                            todos.add((Map<?, ?>) item);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Se um ano falhar, ainda tentamos preservar os feriados retornados pelo outro.
            }
        }

        if (consultasBemSucedidas == 0) {
            List<Map<String, Object>> indisponivel = new ArrayList<>();
            indisponivel.add(Map.of("disponivel", false,
                    "motivo", "Não foi possível consultar feriados nacionais e estaduais na Nager.Date agora"));
            return indisponivel;
        }

        List<Map<?, ?>> validos = new ArrayList<>();
        // Percorre a lista de feriados por índice, sem for-each.
        for (int indice = 0; indice < todos.size(); indice++) {
            Map<?, ?> feriado = todos.get(indice);
            Object valorData = feriado.get("date");
            if (!(valorData instanceof String)) {
                continue;
            }
            String data = (String) valorData;
            if (!dataValidaENaoPassada(data, hoje)) {
                continue;
            }
            if (!ehFeriadoPublico(feriado) || !aplicaAoEstadoOuPaisTodo(feriado, uf)) {
                continue;
            }
            if (!validos.contains(feriado)) {
                validos.add(feriado);
            }
        }

        Collections.sort(validos, new Comparator<Map<?, ?>>() {
            @Override
            public int compare(Map<?, ?> primeiro, Map<?, ?> segundo) {
                LocalDate dataPrimeiro = parseData(String.valueOf(primeiro.get("date")));
                LocalDate dataSegundo = parseData(String.valueOf(segundo.get("date")));
                if (dataPrimeiro == null && dataSegundo == null) return 0;
                if (dataPrimeiro == null) return 1;
                if (dataSegundo == null) return -1;
                return dataPrimeiro.compareTo(dataSegundo);
            }
        });

        if (validos.size() > 5) {
            return new ArrayList<>(validos.subList(0, 5));
        }
        return validos;
    }

    /** Busca o primeiro feriado futuro usando a lista filtrada, sem repetir as chamadas externas. */
    public Optional<FeriadoProximo> proximoFeriado(String uf) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        List<?> feriados = proximosFeriados(uf);
        FeriadoProximo melhor = null;
        // Lê um feriado de cada vez, preservando as validações abaixo.
        for (int indice = 0; indice < feriados.size(); indice++) {
            Object objeto = feriados.get(indice);
            if (!(objeto instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> mapa = (Map<?, ?>) objeto;
            Object valorData = mapa.get("date");
            if (!(valorData instanceof String)) {
                continue;
            }
            LocalDate data = parseData((String) valorData);
            if (data == null) {
                continue;
            }
            long diasLong = ChronoUnit.DAYS.between(hoje, data);
            if (diasLong < 0 || diasLong > Integer.MAX_VALUE) {
                continue;
            }
            Object nomeLocal = mapa.get("localName");
            Object nomeInternacional = mapa.get("name");
            String nome = "Feriado nacional ou estadual";
            if (nomeLocal != null) {
                nome = String.valueOf(nomeLocal);
            } else if (nomeInternacional != null) {
                nome = String.valueOf(nomeInternacional);
            }
            FeriadoProximo candidato = new FeriadoProximo((int) diasLong, nome, data);
            if (melhor == null || candidato.data().isBefore(melhor.data())) {
                melhor = candidato;
            }
        }
        return Optional.ofNullable(melhor);
    }

    public record FeriadoProximo(int dias, String nome, LocalDate data) {}

    /**
     * A API v3 usa "global=true" para feriados nacionais e "counties" para
     * feriados aplicáveis somente a determinadas subdivisões.
     * Quando a UF não estiver disponível, retornamos apenas os nacionais.
     */
    boolean aplicaAoEstadoOuPaisTodo(Map<?, ?> feriado, String uf) {
        if (Boolean.TRUE.equals(feriado.get("global"))) {
            return true;
        }

        if (uf == null || uf.isBlank()) {
            return false;
        }

        Object valorCounties = feriado.get("counties");
        if (!(valorCounties instanceof List<?>)) {
            return false;
        }

        String codigoEstado = "BR-" + uf.trim().toUpperCase(Locale.ROOT);
        List<?> counties = (List<?>) valorCounties;
        for (int indice = 0; indice < counties.size(); indice++) {
            Object county = counties.get(indice);
            if (county != null && codigoEstado.equalsIgnoreCase(county.toString().trim())) {
                return true;
            }
        }
        return false;
    }

    /** Mantém apenas feriados classificados pela API como tipo Public. */
    boolean ehFeriadoPublico(Map<?, ?> feriado) {
        Object valorTipos = feriado.get("types");
        if (!(valorTipos instanceof List<?>)) {
            // Algumas respostas antigas podem não trazer o campo; nesse caso,
            // preservamos o resultado do endpoint PublicHolidays.
            return true;
        }

        List<?> tipos = (List<?>) valorTipos;
        for (int indice = 0; indice < tipos.size(); indice++) {
            Object tipo = tipos.get(indice);
            if (tipo != null && "Public".equalsIgnoreCase(tipo.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean dataValidaENaoPassada(String texto, LocalDate hoje) {
        LocalDate data = parseData(texto);
        return data != null && !data.isBefore(hoje);
    }

    private LocalDate parseData(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (Exception ignored) {
            return null;
        }
    }
}
