package uniamerica.abarbeirados.dto.servico;

import java.math.BigDecimal;

public record ServicoResponse(
        Long id,
        String nome,
        BigDecimal valor,
        Integer duracaoMinutos,
        Boolean ativo
) {
}