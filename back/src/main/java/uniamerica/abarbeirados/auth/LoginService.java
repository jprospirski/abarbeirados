package uniamerica.abarbeirados.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.security.JwtService;

@Slf4j
@Service
public class LoginService {

    public static final String ROLE_PADRAO = "USER";

    @Autowired
    private LoginRepository repository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public String logar(Login login) {

        String token = this.gerarToken(login);
        return token;

    }

    public String gerarToken(Login login) {
        // - usuário ou senha errados lançam AuthenticationException, que o GlobalException devolve como 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        login.getUsername(),
                        login.getPassword()
                )
        );
        Usuario usuario = repository.findByUsername(login.getUsername()).get();
        String jwtToken = jwtService.generateToken(usuario);
        return jwtToken;
    }

    // - a senha nunca é gravada em texto puro: vai para o banco já em BCrypt
    @Transactional
    public void registrar(Registro registro) {
        String username = registro.getUsername().trim();

        if (repository.existsByUsername(username)) {
            throw new NegocioException("Já existe um usuário com esse nome.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(registro.getPassword()));
        usuario.setRole(ROLE_PADRAO);
        repository.save(usuario);

        log.info("Usuário {} cadastrado: {}", usuario.getId(), usuario.getUsername());
    }

}
