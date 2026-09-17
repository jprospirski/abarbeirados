package uniamerica.abarbeirados.dto.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Corpo do POST e do PUT de /api/agendamentos. */
public record AgendamentoRequest(

        @NotNull(message = "O cliente é obrigatório")
        Long clienteId,

        @NotNull(message = "O serviço é obrigatório")
        Long servicoId,

        @NotNull(message = "Data e hora são obrigatórias")
        @Future(message = "A data do agendamento deve ser no futuro")
        LocalDateTime dataHora,

        String observacoes
) {
}
