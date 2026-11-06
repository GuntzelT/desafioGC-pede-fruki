package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.model.Cidade;
import br.com.desafiogc.pedi_fruki_api.model.Cliente;
import br.com.desafiogc.pedi_fruki_api.model.Produto;
import br.com.desafiogc.pedi_fruki_api.repository.CidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Converte clima, feriados e sazonalidade em sinais pontuáveis POR CATEGORIA de bebida.
 *
 * Fontes:
 *  - Clima: API Open-Meteo (temperatura e chuva da cidade do cliente).
 *  - Feriados nacionais: API Nager.Date, como regra geral para feriados que NÃO estão na tabela de eventos.
 *  - Eventos sazonais (Carnaval, verão, Natal etc.): tabela local em EventosSazonais, sem API.
 *
 * As regras são heurísticas comerciais editáveis, não previsões garantidas de venda.
 */
@Service
@RequiredArgsConstructor
public class ContextoPontuacaoService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final CidadeRepository cidades;
    private final ClimaService clima;
    private final CalendarioService calendario;

    // Confira estes termos com o catálogo real (SELECT nome, cod_familia FROM produto).
    @Value("${app.recomendacoes.palavras-alcoolicas:cerveja,pilsen,bellavista,chope,chopp,vinho,cachaca,aguardente,vodka,whisky,espumante}")
    private String palavrasAlcoolicas;

    @Value("${app.recomendacoes.palavras-refrigerantes:refri,refrigerante,guarana,citrus,frukito}")
    private String palavrasRefrigerantes;

    @Value("${app.recomendacoes.palavras-aguas:agua}")
    private String palavrasAguas;

    @Value("${app.recomendacoes.contexto-cache-segundos:1800}")
    private long cacheSegundos;

    private final Map<Integer, CacheContexto> cache = new ConcurrentHashMap<>();

    public ContextoComercial obter(Cliente cliente) {
        Instant agora = Instant.now();
        CacheContexto existente = cache.get(cliente.getIdCliente());
        if (existente != null && existente.expiraEm().isAfter(agora)) return existente.contexto();

        Map<?, ?> previsao = Map.of("disponivel", false, "motivo", "Cidade não informada");
        String uf = null;
        if (cliente.getIdCidade() != null) {
            Cidade cidade = cidades.findById(cliente.getIdCidade()).orElse(null);
            if (cidade != null && cidade.getNome() != null && cidade.getUf() != null) {
                uf = cidade.getUf();
                previsao = clima.previsao(cidade.getNome(), cidade.getUf());
            }
        }
        var proximoFeriado = calendario.proximoFeriado().orElse(null);
        Integer diasFeriado = null;
        String nomeFeriado = null;
        if (proximoFeriado != null) {
            diasFeriado = proximoFeriado.dias();
            nomeFeriado = proximoFeriado.nome();
        }
        ContextoComercial contexto = new ContextoComercial(previsao, diasFeriado, nomeFeriado, uf);
        long ttl = Math.max(60, cacheSegundos);
        cache.put(cliente.getIdCliente(), new CacheContexto(contexto, agora.plusSeconds(ttl)));
        return contexto;
    }

    // ---------------------------------------------------------------- CLIMA (máx. 10) - Open-Meteo

    public int pontosClima(Produto produto, ContextoComercial contexto) {
        int base = pontosBaseDeCalor(contexto);
        if (base == 0) return 0;
        // Calor puxa as bebidas geladas; as demais (suco, chá, energético) recebem metade.
        if (classificar(produto) == CategoriaBebidaService.OUTRAS) {
            return Math.round(base * 0.5f);
        }
        return base;
    }

    private int pontosBaseDeCalor(ContextoComercial contexto) {
        Object valorAtual = contexto.clima().get("current");
        if (!(valorAtual instanceof Map<?, ?>)) {
            return 0;
        }
        Map<?, ?> atual = (Map<?, ?>) valorAtual;
        Double temperatura = numero(atual.get("temperature_2m"));
        Double precipitacao = numero(atual.get("precipitation"));
        Double codigoTempo = numero(atual.get("weather_code"));
        if (temperatura == null) return 0;
        boolean chovendo = (precipitacao != null && precipitacao >= 0.5)
                || (codigoTempo != null && codigoTempo >= 51);
        if (chovendo) return 0;
        if (temperatura >= 32) return 10;
        if (temperatura >= 28) return 7;
        if (temperatura >= 24) return 4;
        return 0;
    }

    public String motivoClima(Produto produto, int pontos, ContextoComercial contexto) {
        if (pontos <= 0) return null;
        Object temperatura = null;
        Object valorAtual = contexto.clima().get("current");
        if (valorAtual instanceof Map<?, ?>) {
            Map<?, ?> atual = (Map<?, ?>) valorAtual;
            temperatura = atual.get("temperature_2m");
        }
        return "temperatura de " + temperatura + " °C favorece " + classificar(produto).rotulo();
    }

    // ---------------------------------------------------------------- CALENDÁRIO (máx. 10)
    // Vale o MAIOR entre a tabela de eventos e o feriado nacional genérico (não soma).

    public int pontosCalendario(Produto produto, ContextoComercial contexto) {
        return sinalCalendario(produto, contexto).pontos();
    }

    public String motivoCalendario(Produto produto, int pontos, ContextoComercial contexto) {
        if (pontos <= 0) return null;
        return sinalCalendario(produto, contexto).motivo();
    }

    private Sinal sinalCalendario(Produto produto, ContextoComercial contexto) {
        CategoriaBebidaService categoria = classificar(produto);
        LocalDate hoje = LocalDate.now(FUSO);

        // 1) Eventos da tabela (Carnaval, verão, Natal, Dia dos Pais etc.), calculados localmente.
        EventosSazonaisService.Resultado evento = EventosSazonaisService.melhorPara(categoria, hoje, contexto.uf());
        Sinal melhor = new Sinal(evento.pontos(), evento.motivo());

        // 2) Feriado nacional vindo da Nager.Date: cobre os que não estão na tabela
        //    (Corpus Christi, Finados, Proclamação da República, Consciência Negra...).
        if (contexto.diasAteFeriado() != null) {
            int dias = contexto.diasAteFeriado();
            double fator = 0;
            if (dias <= 3) fator = 1.0;
            else if (dias <= 7) fator = 0.6;
            if (fator > 0) {
                String nome = contexto.nomeFeriado() != null ? contexto.nomeFeriado() : "feriado nacional";
                int pontos = (int) Math.round(pontosFeriadoGenerico(categoria) * fator);
                if (pontos > melhor.pontos()) {
                    melhor = new Sinal(pontos, "proximidade de " + nome + " (em " + dias + " dia(s))");
                }
            }
        }
        return melhor;
    }

    private int pontosFeriadoGenerico(CategoriaBebidaService categoria) {
        switch (categoria) {
            case ALCOOLICA: return 8;
            case REFRIGERANTE: return 6;
            case AGUA: return 6;
            default: return 3;
        }
    }

    // ---------------------------------------------------------------- CLASSIFICAÇÃO

    public CategoriaBebidaService classificar(Produto produto) {
        String texto = normalizarTexto(produto.getNome() + " " + produto.getCodFamilia());
        // Álcool primeiro: "aguardente" começa com "agua" e não pode virar água.
        if (contemTermo(texto, palavrasAlcoolicas)) return CategoriaBebidaService.ALCOOLICA;
        if (contemTermo(texto, palavrasAguas)) return CategoriaBebidaService.AGUA;
        if (contemTermo(texto, palavrasRefrigerantes)) return CategoriaBebidaService.REFRIGERANTE;
        return CategoriaBebidaService.OUTRAS;
    }

    /** O termo precisa estar no INÍCIO de uma palavra (evita "cha" casar com "salsicha"). */
    private boolean contemTermo(String texto, String listaCsv) {
        String[] termos = listaCsv.split(",");
        for (int indice = 0; indice < termos.length; indice++) {
            String termo = normalizarTexto(termos[indice]);
            if (termo.isBlank()) continue;
            Pattern padrao = Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(termo));
            if (padrao.matcher(texto).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private Double numero(Object valor) {
        if (valor instanceof Number) {
            Number numero = (Number) valor;
            return numero.doubleValue();
        }
        return null;
    }

    private record Sinal(int pontos, String motivo) {}
    private record CacheContexto(ContextoComercial contexto, Instant expiraEm) {}
    public record ContextoComercial(Map<?, ?> clima, Integer diasAteFeriado, String nomeFeriado, String uf) {}
}