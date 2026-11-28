package uniamerica.abarbeirados.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

// - cadastro de novos usuários; a rota já estava liberada no SecurityConfig
@RestController
@RequestMapping("/api/register")
public class RegistroController {

    @Autowired
    private LoginService loginService;

    @PostMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody Registro registro) {
        loginService.registrar(registro);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
