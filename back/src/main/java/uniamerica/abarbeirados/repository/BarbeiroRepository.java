package uniamerica.abarbeirados.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import uniamerica.abarbeirados.model.Barbeiro;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    List<Barbeiro> findByNomeContainingIgnoreCase(String nome);
    List<Barbeiro> findByAtivo(Boolean ativo);
}
