package com.projet.parking.exception;

public class OperationNonAutoriseeException extends RuntimeException {
    public OperationNonAutoriseeException(String message) {
        super(message);
    }
}