package uniamerica.abarbeirados.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import uniamerica.abarbeirados.security.JwtService;

@Service
public class LoginService {

    @Autowired
    private LoginRepository repository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;

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

}
