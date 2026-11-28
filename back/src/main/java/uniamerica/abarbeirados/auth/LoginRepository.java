package uniamerica.abarbeirados.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String login);

    boolean existsByUsername(String username);

}
