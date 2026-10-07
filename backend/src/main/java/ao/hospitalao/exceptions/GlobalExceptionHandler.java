package ao.hospitalao.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        var problem = problem(
            HttpStatus.BAD_REQUEST,
            "Pedido inválido",
            "Um ou mais campos são inválidos."
        );
        Map<String, String> errors = new TreeMap<>();
        exception.getBindingResult().getFieldErrors()
            .forEach(error -> errors.putIfAbsent(error.getField(), "Valor inválido."));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        var problem = problem(
            HttpStatus.BAD_REQUEST,
            "Pedido inválido",
            "Um ou mais parâmetros são inválidos."
        );
        Map<String, String> errors = new TreeMap<>();
        exception.getConstraintViolations().forEach(violation ->
            errors.putIfAbsent(violation.getPropertyPath().toString(), "Valor inválido.")
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class
    })
    public ProblemDetail handleBadRequest(Exception exception) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Pedido inválido",
            "Os dados enviados não têm um formato válido."
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return problem(
            HttpStatus.NOT_FOUND,
            "Recurso não encontrado",
            "O recurso solicitado não foi encontrado."
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailConflict(EmailAlreadyExistsException exception) {
        return problem(
            HttpStatus.CONFLICT,
            "Conflito",
            "Já existe um registo com os dados informados."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return problem(
            HttpStatus.CONFLICT,
            "Conflito",
            "Os dados informados entram em conflito com um registo existente."
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return problem(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Método não permitido",
            "O método HTTP não é permitido para este recurso."
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException exception) {
        return problem(
            HttpStatus.UNAUTHORIZED,
            "Não autenticado",
            "As credenciais informadas são inválidas."
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFound(UsernameNotFoundException exception) {
        return problem(
            HttpStatus.UNAUTHORIZED,
            "Não autenticado",
            "As credenciais informadas são inválidas."
        );
    }

    @ExceptionHandler({
        ExpiredJwtException.class,
        MalformedJwtException.class,
        SignatureException.class
    })
    public ProblemDetail handleInvalidToken(Exception exception) {
        return problem(
            HttpStatus.UNAUTHORIZED,
            "Não autenticado",
            "O token é inválido ou expirou. Inicie sessão novamente."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        return problem(
            HttpStatus.FORBIDDEN,
            "Acesso proibido",
            "Não tem permissão para aceder a este recurso."
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Erro inesperado ao processar o pedido", exception);
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno",
            "Ocorreu um erro interno. Tente novamente mais tarde."
        );
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
