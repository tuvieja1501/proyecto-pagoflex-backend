package cl.duoc.pagoflex.auth.exception;

import cl.duoc.pagoflex.auth.dto.ErrorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorDto> negocio(NegocioException e) {
        return ResponseEntity.status(e.getEstado()).body(new ErrorDto(e.getMessage()));
    }
}
