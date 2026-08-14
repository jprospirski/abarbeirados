package uniamerica.abarbeirados.dto.cliente;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDateTime  dataCadastro
) {
}
