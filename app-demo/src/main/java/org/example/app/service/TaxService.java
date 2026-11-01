package org.example.app.service;

import org.example.app.model.TaxCalcRequest;
import org.example.app.model.TaxCalcResponse;
import org.example.middleware.annotations.controller.Body;
import org.example.middleware.annotations.controller.MethodMapping;
import org.example.middleware.annotations.controller.RequestMapping;
import org.example.middleware.annotations.lifecycle.StaticInstance;
import org.example.middleware.transport.http.MethodHTTP;

@RequestMapping(path = "/tax")
@StaticInstance(lazy = true)
public class TaxService {

    @MethodMapping(path = "/calculate", method = MethodHTTP.POST)
    public TaxCalcResponse calculate(@Body TaxCalcRequest request) {
        if (request == null || request.grossAmount() <= 0) {
            throw new IllegalArgumentException("Invalid tax calculation request data.");
        }

        double gross = request.grossAmount();
        String state = request.stateCode() != null ? request.stateCode().toUpperCase() : "SP";

        double taxRate = switch (state) {
            case "SP", "RJ", "MG" -> 0.18;
            case "PR", "SC", "RS" -> 0.12;
            default -> 0.07;
        };

        // shipping fee by region
        double shipping = switch (state) {
            case "SP" -> 15.00;
            case "RJ", "MG", "PR", "SC", "RS" -> 25.00;
            default -> 45.00;
        };

        double tax = gross * taxRate;
        double total = gross + tax + shipping;

        System.out.printf("[TaxService] Processed -> State: %s | Gross: $%.2f | Tax: $%.2f | Shipping: $%.2f%n",
                state, gross, tax, shipping);

        return new TaxCalcResponse(gross, tax, shipping, total);
    }
}