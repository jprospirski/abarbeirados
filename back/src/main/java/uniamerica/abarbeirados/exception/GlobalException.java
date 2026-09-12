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

    // campo inválido no corpo da requisição (@Valid). monta um mapa campo -> mensagem
    // pra frontend saber exatamente o que corrigir, em vez de um erro genérico.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }

        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, "Erro de validação", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // mesma ideia acima, mas pra @RequestParam/@PathVariable validados com @Validated
    // (não passa pelo @Valid do corpo, então cai nesse handler separado).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // parâmetro de tipo errado na url, tipo mandar um status de agendamento que não existe.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Valor inválido para o parâmetro '%s': %s", ex.getName(), ex.getValue());
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // faltou um parâmetro obrigatório na requisição.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // json malformado ou corpo vazio quando era esperado um corpo.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // regra de negócio quebrada (ex: tentar marcar em horário já ocupado).
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiError> handleBusinessException(NegocioException ex) {
        ApiError apiError = buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // id que não existe no banco (cliente, serviço ou agendamento).
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError apiError = buildError(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    // violação de integridade no banco o caso comum é tentar excluir um cliente
    // ou serviço que ainda está sendo usado por algum agendamento.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiError apiError = buildError(HttpStatus.CONFLICT,
                "O registro não pode ser alterado ou excluído porque está sendo usado por outro registro");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    // url que não bate com nenhum endpoint, sem esse handler ela caía no genérico
    // abaixo e virava erro 500, escondendo o que era só uma rota errada.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        ApiError apiError = buildError(HttpStatus.NOT_FOUND, "Rota não encontrada: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    // qualquer outro erro não previsto cai aqui como rede de segurança.
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