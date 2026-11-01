package org.example.app.model;

public record OrderRequest(
        String item, 
        int quantity, 
        double unitPrice, 
        String stateCode
) {}