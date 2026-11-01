package org.example.app.model;

public record TaxCalcRequest (
    double grossAmount,
    String stateCode
) {}
