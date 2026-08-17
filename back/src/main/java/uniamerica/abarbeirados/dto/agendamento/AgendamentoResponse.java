package uniamerica.abarbeirados.dto.agendamento;

import lombok.NoArgsConstructor;
import uniamerica.abarbeirados.entity.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public record AgendamentoResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDateTime dataHora,
        String servico,
        StatusAgendamento status
){
}