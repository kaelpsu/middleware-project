package org.example.app.model;

public record OrderResponse(
        String orderId, 
        String item, 
        int quantity, 
        double grossAmount, 
        double taxAmount, 
        double shippingFee, 
        double totalAmount, 
        String status
) {}