package cl.duoc.pagoflex.recomendaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class RecomendacionesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecomendacionesServiceApplication.class, args);
    }
}
