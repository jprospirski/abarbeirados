package uniamerica.abarbeirados.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.agendamento.AgendaDoDiaResponse;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.AgendamentoMapper;
import uniamerica.abarbeirados.model.Agendamento;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.repository.AgendamentoRepository;
import uniamerica.abarbeirados.repository.ClienteRepository;
import uniamerica.abarbeirados.repository.ServicoRepository;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoMapper agendamentoMapper;

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        Servico servico = buscarServico(request.servicoId());

        Agendamento agendamento = agendamentoMapper.forEntity(request, cliente, servico);
        return agendamentoMapper.forResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(String busca, LocalDate data) {
        return agendamentoRepository.findAll().stream()
                .filter(agendamento -> combinaComBusca(agendamento, busca))
                .filter(agendamento -> data == null || agendamento.getDataHora().toLocalDate().equals(data))
                .sorted(Comparator.comparing(Agendamento::getDataHora))
                .map(agendamentoMapper::forResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscarPorId(Long id) {
        return agendamentoMapper.forResponse(buscarEntidadePorId(id));
    }

    /** A agenda inteira agrupada por dia, em ordem cronológica. */
    @Transactional(readOnly = true)
    public List<AgendaDoDiaResponse> agendaAgrupadaPorDia() {
        Map<LocalDate, List<AgendamentoResponse>> agrupado = new LinkedHashMap<>();

        for (AgendamentoResponse agendamento : listar(null, null)) {
            agrupado.computeIfAbsent(agendamento.dataHora().toLocalDate(), dia -> new ArrayList<>())
                    .add(agendamento);
        }

        return agrupado.entrySet().stream()
                .map(entrada -> new AgendaDoDiaResponse(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparing(AgendaDoDiaResponse::dia))
                .toList();
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = buscarEntidadePorId(id);
        Cliente cliente = buscarCliente(request.clienteId());
        Servico servico = buscarServico(request.servicoId());

        agendamentoMapper.updateEntity(request, agendamento, cliente, servico);
        return agendamentoMapper.forResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse atualizarStatus(Long id, AtualizarStatusRequest request) {
        Agendamento agendamento = buscarEntidadePorId(id);

        agendamento.setStatus(request.status());
        return agendamentoMapper.forResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public void excluir(Long id) {
        Agendamento agendamento = buscarEntidadePorId(id);
        agendamentoRepository.delete(agendamento);
    }

    /** Busca livre pelo nome do cliente ou do serviço. */
    private boolean combinaComBusca(Agendamento agendamento, String busca) {
        if (busca == null || busca.isBlank()) {
            return true;
        }

        String termo = busca.toLowerCase();

        return agendamento.getCliente().getNome().toLowerCase().contains(termo)
                || agendamento.getServico().getNome().toLowerCase().contains(termo);
    }

    private Agendamento buscarEntidadePorId(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com id " + id));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id " + id));
    }

    private Servico buscarServico(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com id " + id));
    }
}
