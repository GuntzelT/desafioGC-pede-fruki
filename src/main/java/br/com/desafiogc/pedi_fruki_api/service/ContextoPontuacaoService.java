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
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converte os dados externos de clima/calendário em sinais pontuáveis.
 * As regras são heurísticas comerciais editáveis, não previsões garantidas de venda.
 */
@Service
@RequiredArgsConstructor
public class ContextoPontuacaoService {
    private final CidadeRepository cidades;
    private final ClimaService clima;
    private final CalendarioService calendario;

    @Value("${app.recomendacoes.palavras-bebidas:agua,água,refrigerante,refri,suco,bebida,fruki,frukito,água da pedra,agua da pedra,cerveja,pilsen,bellavista,energético,energetico,isotônico,isotonico,cha,chá,limonada}")
    private String palavrasBebidas;

    @Value("${app.recomendacoes.contexto-cache-segundos:1800}")
    private long cacheSegundos;

    private final Map<Integer, CacheContexto> cache = new ConcurrentHashMap<>();

    public ContextoComercial obter(Cliente cliente) {
        Instant agora = Instant.now();
        CacheContexto existente = cache.get(cliente.getIdCliente());
        if (existente != null && existente.expiraEm().isAfter(agora)) return existente.contexto();

        Map<?, ?> previsao = Map.of("disponivel", false, "motivo", "Cidade não informada");
        if (cliente.getIdCidade() != null) {
            Cidade cidade = cidades.findById(cliente.getIdCidade()).orElse(null);
            if (cidade != null && cidade.getNome() != null && cidade.getUf() != null) {
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
        ContextoComercial contexto = new ContextoComercial(previsao, diasFeriado, nomeFeriado);
        long ttl = Math.max(60, cacheSegundos);
        cache.put(cliente.getIdCliente(), new CacheContexto(contexto, agora.plusSeconds(ttl)));
        return contexto;
    }

    public int pontosClima(Produto produto, ContextoComercial contexto) {
        if (!produtoElegivel(produto)) return 0;
        Map<?, ?> raiz = contexto.clima();
        Object valorAtual = raiz.get("current");
        if (!(valorAtual instanceof Map<?, ?>)) {
            return 0;
        }
        Map<?, ?> atual = (Map<?, ?>) valorAtual;
        Double temperatura = numero(atual.get("temperature_2m"));
        Double precipitacao = numero(atual.get("precipitation"));
        Double codigoTempo = numero(atual.get("weather_code"));
        if (temperatura == null) return 0;
        // Regra inicial: o impulso por calor só vale quando a API não indica precipitação/tempo chuvoso.
        boolean chovendo = (precipitacao != null && precipitacao >= 0.5)
                || (codigoTempo != null && codigoTempo >= 51);
        if (chovendo) return 0;
        if (temperatura >= 32) return 10;
        if (temperatura >= 28) return 7;
        if (temperatura >= 24) return 4;
        return 0;
    }

    public int pontosCalendario(Produto produto, ContextoComercial contexto) {
        if (!produtoElegivel(produto) || contexto.diasAteFeriado() == null) return 0;
        int dias = contexto.diasAteFeriado();
        if (dias <= 3) return 10;
        if (dias <= 7) return 6;
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
        return "temperatura de " + temperatura + " °C favorece a regra inicial de bebidas";
    }

    public String motivoCalendario(int pontos, ContextoComercial contexto) {
        if (pontos <= 0) return null;
        String nome = "feriado nacional";
        if (contexto.nomeFeriado() != null) {
            nome = contexto.nomeFeriado();
        }
        return "proximidade de " + nome + " (em " + contexto.diasAteFeriado() + " dia(s))";
    }

    private boolean produtoElegivel(Produto produto) {
        String texto = normalizarTexto(
                String.valueOf(produto.getNome()) + " " + String.valueOf(produto.getCodFamilia()));
        String[] palavras = palavrasBebidas.split(",");
        // Array usa length, diferente de List, que usa size().
        for (int indice = 0; indice < palavras.length; indice++) {
            String termo = normalizarTexto(palavras[indice]);
            if (!termo.isBlank() && texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    /** Ignora diferenças de acentuação e letras maiúsculas na regra contextual. */
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

    private record CacheContexto(ContextoComercial contexto, Instant expiraEm) {}
    public record ContextoComercial(Map<?, ?> clima, Integer diasAteFeriado, String nomeFeriado) {}
}
