package uniamerica.abarbeirados.mapper;

import org.springframework.stereotype.Component;

import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.model.Agendamento;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.model.StatusAgendamento;

@Component
public class AgendamentoMapper {

    /*
     * Cliente e Servico chegam prontos: quem resolve os ids e o service, que e a
     * camada com acesso aos repositorios.
     */
    public Agendamento forEntity(AgendamentoRequest request, Cliente cliente, Servico servico) {
        return Agendamento.builder()
                .cliente(cliente)
                .servico(servico)
                .dataHora(request.dataHora())
                .observacoes(request.observacoes())
                .status(StatusAgendamento.AGENDADO)
                .valor(servico.getValor())
                .duracaoMinutos(servico.getDuracaoMinutos())
                .build();
    }

    public void updateEntity(AgendamentoRequest request, Agendamento agendamento, Cliente cliente, Servico servico) {
        agendamento.setCliente(cliente);
        agendamento.setServico(servico);
        agendamento.setDataHora(request.dataHora());
        agendamento.setObservacoes(request.observacoes());
        agendamento.setValor(servico.getValor());
        agendamento.setDuracaoMinutos(servico.getDuracaoMinutos());
    }

    public AgendamentoResponse forResponse(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        Servico servico = agendamento.getServico();

        return new AgendamentoResponse(
                agendamento.getId(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                servico.getId(),
                servico.getNome(),
                agendamento.getValor(),
                agendamento.getDuracaoMinutos(),
                agendamento.getDataHora(),
                agendamento.getStatus(),
                agendamento.getObservacoes()
        );
    }
}
