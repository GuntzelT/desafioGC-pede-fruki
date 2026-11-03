package br.com.desafiogc.pedi_fruki_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Consulta feriados públicos nacionais na Nager.Date, alinhada ao projeto de referência. */
@Service
@RequiredArgsConstructor
public class CalendarioService {
    private final RestClient.Builder builder;

    public List<?> proximosFeriados() {
        LocalDate hoje = LocalDate.now();
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
                    "motivo", "Não foi possível consultar feriados nacionais na Nager.Date agora"));
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
    public Optional<FeriadoProximo> proximoFeriado() {
        LocalDate hoje = LocalDate.now();
        List<?> feriados = proximosFeriados();
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
            String nome = "Feriado nacional";
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
