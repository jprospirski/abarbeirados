package uniamerica.abarbeirados.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uniamerica.abarbeirados.entity.Agendamento;
import uniamerica.abarbeirados.entity.StatusAgendamento;
import uniamerica.abarbeirados.repository.AgendamentoRepository;

import java.util.List;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Transactional
    public Agendamento save (Agendamento agendamento) {
        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> findAllByStatus(StatusAgendamento status) {
        return agendamentoRepository.findAllByStatus(status);
    }

    public Agendamento findById(Long id) {
        return agendamentoRepository.findById(id).orElse(null);
    }

    public Agendamento findByIdAndStatus(Long id,StatusAgendamento status) {
        return agendamentoRepository.findByIdAndStatus(id, status).orElse(null);
    }

    @Transactional
    public Agendamento updateAgendamento(Long id,Agendamento agendamento) {
        if (agendamentoRepository.existsById(id)) {
            agendamento.setId(id);
            return agendamentoRepository.save(agendamento);
        }
        return null;
    }

    @Transactional
    public boolean deleteById(Long id) {
        if (agendamentoRepository.existsById(id)) {
            agendamentoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteByIdAndStatus(Long id,StatusAgendamento status) {

        return agendamentoRepository.findByIdAndStatus( id, status)
                .map(a -> {
                    agendamentoRepository.delete(a);
                    return true;
                })
                .orElse(false);
    }

}
