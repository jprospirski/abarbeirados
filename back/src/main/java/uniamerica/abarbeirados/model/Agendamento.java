package uniamerica.abarbeirados.model;

import uniamerica.abarbeirados.entity.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalTime;

public record Agendamento(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDate data,
        LocalTime hora,
        String servico,
        StatusAgendamento status
) {
}
