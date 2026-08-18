package uniamerica.abarbeirados.dto.error;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de erro da API, montado pelo GlobalException.
 *
 * `fields` só vem preenchido em erro de validação, com o motivo campo a campo.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fields
) {

    public ApiError(LocalDateTime timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, null);
    }
}
