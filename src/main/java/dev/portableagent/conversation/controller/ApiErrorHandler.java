package dev.portableagent.conversation.controller;

import dev.portableagent.conversation.api.model.Problem;
import dev.portableagent.conversation.client.ActionUnavailable;
import dev.portableagent.conversation.client.AgentUnavailable;
import dev.portableagent.conversation.exception.MessageBusy;
import dev.portableagent.conversation.exception.MessageErased;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler({AgentUnavailable.class, ActionUnavailable.class, MessageBusy.class})
    ResponseEntity<Problem> unavailable() {
        return problem(HttpStatus.BAD_GATEWAY, "Dependency is temporarily unavailable");
    }

    @ExceptionHandler({MessageErased.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    ResponseEntity<Problem> invalid() {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Request cannot be processed");
    }

    private ResponseEntity<Problem> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(new Problem(detail));
    }
}
