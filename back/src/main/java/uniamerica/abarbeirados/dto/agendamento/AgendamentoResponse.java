package uniamerica.abarbeirados.dto.agendamento;

import uniamerica.abarbeirados.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Retorno de /api/agendamentos.
 *
 * Traz cliente e servico ja resolvidos para a tela nao precisar de uma segunda
 * requisicao so para descobrir o nome de quem esta agendado.
 */
public record AgendamentoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        String clienteTelefone,
        Long servicoId,
        String servicoNome,
        BigDecimal valor,
        Integer duracaoMinutos,
        LocalDateTime dataHora,
        StatusAgendamento status,
        String observacoes
) {
}
