package cl.duoc.pagoflex.recomendaciones.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("PagoFlex - recomendaciones-service")
                .version("1.0.0")
                .description("Recomendaciones de organizaciones, contactos y seguimiento de estados."));
    }
}
