package cl.duoc.pagoflex.recomendaciones.controller;

import cl.duoc.pagoflex.recomendaciones.exception.ManejadorErrores;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

final class MvcSupport {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private MvcSupport() {
    }

    static MockMvc mvc(Object controlador) {
        return MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new ManejadorErrores())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(MAPPER))
                .build();
    }
}
