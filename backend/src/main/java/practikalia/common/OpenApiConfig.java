package practikalia.common;

import practikalia.usuario.jwt.JwtService;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_COOKIE = "cookieAuth";

    @Bean
    public OpenAPI practikaliaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Practikalia API")
                        .description("API de Practikalia, plataforma para gestionar empresas de prácticas, su histórico y la orientación del alumnado.")
                        .version("0.0.1-SNAPSHOT"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(JwtService.COOKIE_NAME)));
    }
}
