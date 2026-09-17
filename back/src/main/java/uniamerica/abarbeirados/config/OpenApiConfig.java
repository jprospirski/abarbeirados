package uniamerica.abarbeirados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Cabeçalho da documentação em /swagger-ui/index.html.
 *
 * Os endpoints o springdoc descobre sozinho a partir dos controllers; aqui só
 * fica o título, para a página não abrir como "OpenAPI definition v1.0".
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI abarbeiradosOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Abarbeirados API")
                .description("API de gestão para barbearia — clientes, serviços, barbeiros e agendamentos.")
                .version("1.0"));
    }
}
