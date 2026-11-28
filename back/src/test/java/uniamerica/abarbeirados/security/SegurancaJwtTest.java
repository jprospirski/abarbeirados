package uniamerica.abarbeirados.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uniamerica.abarbeirados.auth.LoginController;
import uniamerica.abarbeirados.auth.LoginRepository;
import uniamerica.abarbeirados.auth.LoginService;
import uniamerica.abarbeirados.auth.RegistroController;
import uniamerica.abarbeirados.auth.Usuario;
import uniamerica.abarbeirados.controller.ServicoController;
import uniamerica.abarbeirados.service.ServicoService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de autenticação de ponta a ponta na camada web, com a cadeia de
 * segurança real (filtro JWT, SecurityConfig, BCrypt). Só o banco é simulado.
 */
@WebMvcTest(controllers = {LoginController.class, RegistroController.class, ServicoController.class})
@Import({SecurityConfig.class, Maneger.class, JwtService.class, LoginService.class})
class SegurancaJwtTest {

    private static final String ROTA_PROTEGIDA = "/api/servicos";
    private static final String CREDENCIAIS = """
            {"username": "ana", "password": "segredo123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private LoginRepository loginRepository;

    @MockitoBean
    private ServicoService servicoService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("ana");
        usuario.setPassword(passwordEncoder.encode("segredo123"));
        usuario.setRole("USER");

        when(loginRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(servicoService.listar(any(), any())).thenReturn(List.of());
    }

    @Test
    void rotaProtegida_quandoSemToken_retorna401() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA)).andExpect(status().isUnauthorized());

        verify(servicoService, never()).listar(any(), any());
    }

    @Test
    void rotaProtegida_quandoTokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer nao.e.um.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegida_quandoTokenDeUsuarioInexistente_retorna401() throws Exception {
        Usuario removido = new Usuario();
        removido.setId(2L);
        removido.setUsername("fantasma");
        removido.setRole("USER");
        String token = jwtService.generateToken(removido);

        mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_quandoCredenciaisValidas_retornaTokenQueAbreRotaProtegida() throws Exception {
        String token = mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(CREDENCIAIS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(jwtService.extractUsername(token)).isEqualTo("ana");
        assertThat(jwtService.isTokenValid(token, usuario)).isTrue();

        mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void login_quandoSenhaErrada_retornaApiError401() throws Exception {
        String corpo = """
                {"username": "ana", "password": "errada"}
                """;

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuário ou senha inválidos"));
    }

    @Test
    void registrar_quandoDadosValidos_gravaSenhaEmBCrypt() throws Exception {
        String corpo = """
                {"username": "bruno", "password": "segredo123"}
                """;
        when(loginRepository.existsByUsername("bruno")).thenReturn(false);

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(loginRepository).save(salvo.capture());
        assertThat(salvo.getValue().getUsername()).isEqualTo("bruno");
        assertThat(salvo.getValue().getRole()).isEqualTo(LoginService.ROLE_PADRAO);
        assertThat(salvo.getValue().getPassword()).isNotEqualTo("segredo123");
        assertThat(passwordEncoder.matches("segredo123", salvo.getValue().getPassword())).isTrue();
    }

    @Test
    void registrar_quandoUsuarioJaExiste_retorna400SemSalvar() throws Exception {
        when(loginRepository.existsByUsername("ana")).thenReturn(true);

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(CREDENCIAIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Já existe um usuário com esse nome."));

        verify(loginRepository, never()).save(any());
    }

    @Test
    void registrar_quandoSenhaCurta_retornaErroDeValidacao() throws Exception {
        String corpo = """
                {"username": "bruno", "password": "123"}
                """;

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());

        verify(loginRepository, never()).save(any());
    }

    @Test
    void cors_quandoOrigemPermitida_liberaPreflight() throws Exception {
        mockMvc.perform(options(ROTA_PROTEGIDA)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void cors_quandoOrigemDesconhecida_bloqueiaPreflight() throws Exception {
        mockMvc.perform(options(ROTA_PROTEGIDA)
                        .header(HttpHeaders.ORIGIN, "http://site-estranho.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
