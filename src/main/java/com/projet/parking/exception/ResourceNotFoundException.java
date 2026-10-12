package com.projet.parking.exception;

public class ResourceNotFoundException extends RuntimeException {
    private String entity;

    public ResourceNotFoundException(String entity, String message) {
        super(entity + " " + message);
        this.entity = entity;
    }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
}