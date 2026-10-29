package br.com.desafiogc.pedi_fruki_api.service;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import br.com.desafiogc.pedi_fruki_api.model.*;
import br.com.desafiogc.pedi_fruki_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SimulacaoSessaoService {
    private static final Duration DURACAO_SESSAO = Duration.ofHours(2);

    private final ClienteRepository clientes;
    private final ClienteService clienteService;
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public IdentificarClienteResponse identificar(String identificador) {
        String somenteDigitos = identificador == null ? "" : identificador.replaceAll("\\D", "");
        if (somenteDigitos.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um CNPJ com 14 dígitos ou o código numérico do cliente.");
        }
        Cliente cliente;
        if (somenteDigitos.length() == 14) {
            cliente = clientes.findPorCnpjNormalizado(somenteDigitos).orElse(null);
            if (cliente == null) {
                throw clienteNaoEncontrado();
            }
        } else {
            try {
                Integer id = Integer.valueOf(somenteDigitos);
                cliente = clientes.findById(id).orElse(null);
                if (cliente == null) {
                    throw clienteNaoEncontrado();
                }
            } catch (NumberFormatException ex) {
                throw clienteNaoEncontrado();
            }
        }

        expurgarSessoesExpiradas();
        String token = UUID.randomUUID().toString();
        sessoes.put(token, new Sessao(cliente.getIdCliente(), Instant.now().plus(DURACAO_SESSAO)));

        return new IdentificarClienteResponse(
                token,
                clienteService.buscarResumo(cliente.getIdCliente()),
                "Cadastro localizado. Nesta simulação, a conversa está associada a " + cliente.getNome() + "."
        );
    }

    public Integer exigirCliente(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Identifique o cliente na conversa antes de continuar.");
        }
        String token = authorizationHeader.substring(7).trim();
        Sessao sessao = sessoes.get(token);
        if (sessao == null || sessao.expiraEm().isBefore(Instant.now())) {
            if (token != null) sessoes.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "A sessão da simulação expirou. Identifique o cliente novamente.");
        }
        return sessao.idCliente();
    }

    /** Impede que uma sessão consulte o cadastro de outro cliente alterando a URL da API. */
    public Integer exigirClienteDaSessao(String authorizationHeader, Integer idClienteSolicitado) {
        Integer idAssociado = exigirCliente(authorizationHeader);
        if (!idAssociado.equals(idClienteSolicitado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta conversa está vinculada a outro cliente. Reinicie a simulação para trocar de cadastro.");
        }
        return idAssociado;
    }

    private void expurgarSessoesExpiradas() {
        Instant agora = Instant.now();
        List<String> tokensExpirados = new java.util.ArrayList<>();
        for (Map.Entry<String, Sessao> entrada : sessoes.entrySet()) {
            if (entrada.getValue().expiraEm().isBefore(agora)) {
                tokensExpirados.add(entrada.getKey());
            }
        }
        for (String token : tokensExpirados) {
            sessoes.remove(token);
        }
    }

    private ResponseStatusException clienteNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Não encontrei um cliente com esse CNPJ ou código. Confira os dados e tente novamente.");
    }

    private record Sessao(Integer idCliente, Instant expiraEm) {}
}
