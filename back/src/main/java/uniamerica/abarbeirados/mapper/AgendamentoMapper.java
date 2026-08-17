package uniamerica.abarbeirados.mapper;

import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.entity.Agendamento;
import uniamerica.abarbeirados.entity.StatusAgendamento;

public class AgendamentoMapper {

    private AgendamentoMapper() {
    }

    public static Agendamento toEntity(AgendamentoRequest request) {
        Agendamento agendamento = new Agendamento();
        agendamento.setNome(request.getNome());
        agendamento.setEmail(request.getEmail());
        agendamento.setTelefone(request.getTelefone());
        agendamento.setServico(request.getServico());
        agendamento.setDataHora(request.getDataHora());
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        return agendamento;
    }

    public static void updateEntity(Agendamento existing, AgendamentoRequest request) {
        existing.setNome(request.getNome());
        existing.setEmail(request.getEmail());
        existing.setTelefone(request.getTelefone());
        existing.setServico(request.getServico());
        existing.setDataHora(request.getDataHora());
    }

    public static AgendamentoResponse toResponse(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getNome(),
                agendamento.getEmail(),
                agendamento.getTelefone(),
                agendamento.getDataHora(),
                agendamento.getServico(),
                agendamento.getStatus()
        );
    }
}