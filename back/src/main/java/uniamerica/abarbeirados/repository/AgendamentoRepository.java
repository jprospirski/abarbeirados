package uniamerica.abarbeirados.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uniamerica.abarbeirados.model.Agendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    // - filtro por busca/dia e agrupamento são feitos em memória no service; migrar para @Query se a base crescer
}