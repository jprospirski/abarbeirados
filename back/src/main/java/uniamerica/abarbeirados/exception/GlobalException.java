package uniamerica.abarbeirados.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uniamerica.abarbeirados.dto.error.ApiError;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalException {

    // Validation errors from @Valid (invalid fields in the request body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }

        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, "Erro de validação", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Parameter validation errors (e.g. @RequestParam, @PathVariable with @Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Path/query parameter with an incompatible type (e.g. invalid StatusAgendamento enum value)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Valor inválido para o parâmetro '%s': %s", ex.getName(), ex.getValue());
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Required parameter missing from the request
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Malformed or unreadable JSON in the request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Business rule violations
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiError> handleBusinessException(NegocioException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError apiError = buildError(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    // Violacao de integridade no banco. O caso comum e tentar excluir um cliente
    // ou servico que ainda tem agendamento apontando para ele.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiError apiError = buildError(HttpStatus.CONFLICT,
                "O registro não pode ser alterado ou excluído porque está sendo usado por outro registro");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    // URL que nao casa com nenhum endpoint. Sem este handler a excecao caia no
    // handler generico abaixo e virava 500, escondendo um simples erro de rota.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        ApiError apiError = buildError(HttpStatus.NOT_FOUND, "Rota não encontrada: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    // Any other unhandled exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        ApiError apiError = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    private ApiError buildError(HttpStatus status, String message) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);
    }

    private ApiError buildError(HttpStatus status, String message, Map<String, String> fields) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fields);
    }
}