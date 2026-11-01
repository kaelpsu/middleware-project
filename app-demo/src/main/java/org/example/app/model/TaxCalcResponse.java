package org.example.app.model;

public record TaxCalcResponse(
        double grossAmount, 
        double taxAmount, 
        double shippingFee, 
        double totalAmount
) {}