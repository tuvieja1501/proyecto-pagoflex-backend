package cl.duoc.pagoflex.recomendaciones.exception;

import cl.duoc.pagoflex.recomendaciones.dto.ErrorDto;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorDto> negocio(NegocioException e) {
        return ResponseEntity.status(e.getEstado()).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorDto> servicioExterno(FeignException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorDto("El servicio de cuentas no está disponible. Intenta de nuevo en un momento."));
    }
}
