package br.com.desafiogc.pedi_fruki_api.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Confirma apenas que a aplicação subiu. */
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "ok", "aplicacao", "pede-fruki-api");
    }

    /** Verifica de verdade se o backend consegue consultar o PostgreSQL/Supabase. */
    @GetMapping("/database")
    public ResponseEntity<Map<String, String>> database() {
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok", "banco", "conectado"));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "indisponivel", "banco", "nao_conectado"));
        }
    }
}
