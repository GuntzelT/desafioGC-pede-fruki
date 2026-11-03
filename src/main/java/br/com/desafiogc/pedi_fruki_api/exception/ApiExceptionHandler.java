package br.com.desafiogc.pedi_fruki_api.exception;

import br.com.desafiogc.pedi_fruki_api.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDto> tratarStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String mensagem;
        if (ex.getReason() == null) {
            mensagem = status.getReasonPhrase();
        } else {
            mensagem = ex.getReason();
        }
        return ResponseEntity.status(status).body(erro(status, mensagem, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String mensagem = "Dados informados inválidos";
        if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
            var erroCampo = ex.getBindingResult().getFieldErrors().get(0);
            mensagem = erroCampo.getField() + ": " + erroCampo.getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(erro(status, mensagem, request));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorDto> tratarBanco(DataAccessException ex, HttpServletRequest request) {
        log.error("Falha ao consultar o banco PostgreSQL/Supabase", ex);
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(erro(status,
                "Não foi possível consultar o banco de dados. Confira conexão, credenciais e esquema.", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado na API", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(erro(status,
                "Ocorreu um erro interno. Consulte os logs do backend.", request));
    }

    private ApiErrorDto erro(HttpStatus status, String mensagem, HttpServletRequest request) {
        return new ApiErrorDto(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, request.getRequestURI());
    }
}
