package uniamerica.abarbeirados.dto.agendamento;

import jakarta.validation.constraints.NotNull;
import uniamerica.abarbeirados.model.StatusAgendamento;

/** Corpo do PATCH de /api/agendamentos/{id}/status. */
public record AtualizarStatusRequest(

        @NotNull(message = "O status é obrigatório")
        StatusAgendamento status
) {
}
