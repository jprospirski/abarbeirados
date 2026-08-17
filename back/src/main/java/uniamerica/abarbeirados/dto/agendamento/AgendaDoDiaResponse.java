package uniamerica.abarbeirados.dto.agendamento;

import java.time.LocalDate;
import java.util.List;

public class AgendaDoDiaResponse {

    private LocalDate dia;
    private List<AgendamentoResponse> agendamentos;

    public AgendaDoDiaResponse() {
    }

    public AgendaDoDiaResponse(LocalDate dia, List<AgendamentoResponse> agendamentos) {
        this.dia = dia;
        this.agendamentos = agendamentos;
    }

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public List<AgendamentoResponse> getAgendamentos() {
        return agendamentos;
    }

    public void setAgendamentos(List<AgendamentoResponse> agendamentos) {
        this.agendamentos = agendamentos;
    }
}