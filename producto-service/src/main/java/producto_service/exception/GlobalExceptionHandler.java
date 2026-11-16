package producto_service.exception;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import producto_service.dto.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage(), request, null);
    }

    @ExceptionHandler({ BusinessException.class, IllegalArgumentException.class })
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.BAD_REQUEST, "Solicitud invalida", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.CONFLICT, "Conflicto", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        logger.warn("Conflicto de integridad de datos en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return construir(
                HttpStatus.CONFLICT,
                "Conflicto",
                "La operacion viola una restriccion de unicidad o integridad de datos",
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return construir(HttpStatus.BAD_REQUEST, "Datos invalidos", "Revise los campos enviados", request, errores);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleMalformedRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        return construir(
                HttpStatus.BAD_REQUEST,
                "Solicitud invalida",
                "El formato de la solicitud no es valido",
                request,
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Metodo no permitido", ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.NOT_FOUND, "No encontrado", "Recurso no encontrado", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.error("Error interno en {}", request.getRequestURI(), ex);
        return construir(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno",
                "Ocurrio un error interno al procesar la solicitud",
                request,
                null
        );
    }

    private ResponseEntity<ErrorResponseDTO> construir(
            HttpStatus estado,
            String error,
            String mensaje,
            HttpServletRequest request,
            List<String> errores
    ) {
        ErrorResponseDTO respuesta = new ErrorResponseDTO(
                LocalDateTime.now(ZoneOffset.UTC),
                estado.value(),
                error,
                mensaje,
                request.getRequestURI(),
                errores
        );
        return ResponseEntity.status(estado).body(respuesta);
    }
}
