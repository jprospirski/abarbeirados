package uniamerica.abarbeirados.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uniamerica.abarbeirados.entity.Agendamento;
import uniamerica.abarbeirados.entity.StatusAgendamento;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findAllByStatus(StatusAgendamento status);

    Optional<Agendamento> findByIdAndStatus(Long id, StatusAgendamento status);
}
