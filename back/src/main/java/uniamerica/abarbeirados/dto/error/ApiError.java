package uniamerica.abarbeirados.dto.error;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> fields;

    public ApiError() {
    }

    public ApiError(LocalDateTime timestamp, int status, String error, String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ApiError(LocalDateTime timestamp, int status, String error, String message, Map<String, String> fields) {
        this(timestamp, status, error, message);
        this.fields = fields;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}
