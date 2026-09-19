package uniamerica.abarbeirados.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uniamerica.abarbeirados.dto.agendamento.AgendaDoDiaResponse;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.AgendamentoMapper;
import uniamerica.abarbeirados.model.Agendamento;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.model.StatusAgendamento;
import uniamerica.abarbeirados.repository.AgendamentoRepository;
import uniamerica.abarbeirados.repository.BarbeiroRepository;
import uniamerica.abarbeirados.repository.ClienteRepository;
import uniamerica.abarbeirados.repository.ServicoRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final AgendamentoMapper agendamentoMapper;

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        Servico servico = buscarServico(request.servicoId());
        Barbeiro barbeiro = buscarBarbeiro(request.barbeiroId());

        validarConflito(barbeiro, request.dataHora(), servico.getDuracaoMinutos(), null);

        Agendamento agendamento = agendamentoRepository.save(
                agendamentoMapper.forEntity(request, cliente, servico, barbeiro));

        log.info("Agendamento {} criado para cliente {} com barbeiro {} em {}",
                agendamento.getId(), cliente.getNome(), barbeiro.getNome(), request.dataHora());

        return agendamentoMapper.forResponse(agendamento);
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
        Barbeiro barbeiro = buscarBarbeiro(request.barbeiroId());

        validarConflito(barbeiro, request.dataHora(), servico.getDuracaoMinutos(), id);

        agendamentoMapper.updateEntity(request, agendamento, cliente, servico, barbeiro);
        agendamentoRepository.save(agendamento);

        log.info("Agendamento {} atualizado para cliente {} com barbeiro {} em {}",
                agendamento.getId(), cliente.getNome(), barbeiro.getNome(), request.dataHora());

        return agendamentoMapper.forResponse(agendamento);
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

        log.info("Agendamento {} excluído (cliente {}, barbeiro {})",
                id, agendamento.getCliente().getNome(), agendamento.getBarbeiro().getNome());
    }

    /**
     * Um barbeiro não pode ter dois atendimentos que se sobreponham.
     *
     * A comparação é entre intervalos [início, início + duração), e não entre
     * horários de início: um corte de 40 min marcado às 15:00 bloqueia as 15:20,
     * mas libera as 15:40. Cancelado não ocupa vaga, e na edição o próprio
     * agendamento sai da checagem para não bater contra si mesmo.
     */
    private void validarConflito(Barbeiro barbeiro, LocalDateTime dataHora, Integer duracaoMinutos, Long ignorarId) {
        LocalDateTime inicioNovo = dataHora;
        LocalDateTime fimNovo = dataHora.plusMinutes(duracaoMinutos);

        boolean conflita = agendamentoRepository.findAll().stream()
                .filter(agendamento -> agendamento.getBarbeiro().getId().equals(barbeiro.getId()))
                .filter(agendamento -> agendamento.getStatus() != StatusAgendamento.CANCELADO)
                // - com ignorarId nulo nenhum id bate, então criar() checa a agenda inteira
                .filter(agendamento -> !agendamento.getId().equals(ignorarId))
                .anyMatch(agendamento -> {
                    LocalDateTime inicioExistente = agendamento.getDataHora();
                    LocalDateTime fimExistente = inicioExistente.plusMinutes(agendamento.getDuracaoMinutos());
                    return inicioNovo.isBefore(fimExistente) && inicioExistente.isBefore(fimNovo);
                });

        if (conflita) {
            throw new NegocioException("Já existe um agendamento para esse barbeiro nesse horário.");
        }
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

    private Barbeiro buscarBarbeiro(Long id) {
        return barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com id " + id));
    }
}
