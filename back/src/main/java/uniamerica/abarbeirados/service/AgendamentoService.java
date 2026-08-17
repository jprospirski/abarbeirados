package uniamerica.abarbeirados.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uniamerica.abarbeirados.dto.agendamento.AgendaDoDiaResponse;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.entity.Agendamento;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;
import uniamerica.abarbeirados.mapper.AgendamentoMapper;
import uniamerica.abarbeirados.repository.AgendamentoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Transactional
    public Agendamento save(AgendamentoRequest request) {
        Agendamento agendamento = AgendamentoMapper.toEntity(request);
        return agendamentoRepository.save(agendamento);
    }

    public Agendamento findById(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com id: " + id));
    }

    public List<Agendamento> findAll(String busca, LocalDate dia) {
        List<Agendamento> todos = agendamentoRepository.findAll();
        List<Agendamento> resultado = new ArrayList<>();

        for (Agendamento a : todos) {
            boolean passaBusca = busca == null || busca.isBlank()
                    || a.getNome().toLowerCase().contains(busca.toLowerCase())
                    || a.getServico().toLowerCase().contains(busca.toLowerCase());

            boolean passaDia = dia == null || a.getDataHora().toLocalDate().equals(dia);

            if (passaBusca && passaDia) {
                resultado.add(a);
            }
        }

        resultado.sort(Comparator.comparing(Agendamento::getDataHora));
        return resultado;
    }

    public List<AgendaDoDiaResponse> getAgendaAgrupadaPorDia() {
        List<Agendamento> agendamentos = agendamentoRepository.findAll();

        // Agrupa manualmente os agendamentos por dia
        Map<LocalDate, List<AgendamentoResponse>> agrupado = new LinkedHashMap<>();

        for (Agendamento a : agendamentos) {
            LocalDate dia = a.getDataHora().toLocalDate();

            if (!agrupado.containsKey(dia)) {
                agrupado.put(dia, new ArrayList<>());
            }
            agrupado.get(dia).add(AgendamentoMapper.toResponse(a));
        }

        // Monta a lista final de AgendaDoDiaResponse a partir do mapa agrupado
        List<AgendaDoDiaResponse> resultado = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AgendamentoResponse>> entry : agrupado.entrySet()) {
            resultado.add(new AgendaDoDiaResponse(entry.getKey(), entry.getValue()));
        }

        resultado.sort(Comparator.comparing(AgendaDoDiaResponse::getDia));
        return resultado;
    }

    @Transactional
    public Agendamento update(Long id, AgendamentoRequest request) {
        Agendamento existing = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com id: " + id));

        AgendamentoMapper.updateEntity(existing, request);
        return agendamentoRepository.save(existing);
    }

    @Transactional
    public Agendamento updateStatus(Long id, AtualizarStatusRequest request) {
        Agendamento existing = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com id: " + id));

        existing.setStatus(request.getStatus());
        return agendamentoRepository.save(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!agendamentoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Agendamento não encontrado com id: " + id);
        }
        agendamentoRepository.deleteById(id);
    }
}