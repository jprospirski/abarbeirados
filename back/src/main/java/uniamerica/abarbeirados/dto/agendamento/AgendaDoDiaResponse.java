package uniamerica.abarbeirados.dto.agendamento;

import java.time.LocalDate;
import java.util.List;

/** Os agendamentos de um dia, agrupados para a agenda. */
public record AgendaDoDiaResponse(
        LocalDate dia,
        List<AgendamentoResponse> agendamentos
) {
}
