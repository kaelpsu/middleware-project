package org.example.app.service;

import org.example.app.model.OrderRequest;
import org.example.app.model.OrderResponse;
import org.example.app.model.TaxCalcRequest;
import org.example.app.model.TaxCalcResponse;
import org.example.middleware.annotations.controller.Body;
import org.example.middleware.annotations.controller.MethodMapping;
import org.example.middleware.annotations.controller.RequestMapping;
import org.example.middleware.annotations.lifecycle.PerRequestInstance;
import org.example.middleware.client.Requestor;
import org.example.middleware.transport.http.MethodHTTP;

import java.util.UUID;

@RequestMapping(path = "/order")
@PerRequestInstance(pooled = true, poolSize = 5)
public class OrderService {

    private final Requestor requestor = new Requestor();

    @MethodMapping(path = "/process", method = MethodHTTP.POST)
    public OrderResponse processOrder(@Body OrderRequest request) throws Exception {
        if (request == null || request.quantity() <= 0 || request.unitPrice() <= 0) {
            throw new IllegalArgumentException("Invalid order data.");
        }

        double grossAmount = request.quantity() * request.unitPrice();

        String gatewayHost = System.getProperty("gateway.host", "127.0.0.1");
        int gatewayPort = Integer.parseInt(System.getProperty("gateway.tcp.port", "8080"));

        TaxCalcRequest taxReq = new TaxCalcRequest(grossAmount, request.stateCode());
        TaxCalcResponse taxResp = requestor.invoke(
                "TCP",
                gatewayHost,
                gatewayPort,
                "/gateway/taxcalc",
                MethodHTTP.POST,
                taxReq,
                TaxCalcResponse.class
        );

        String orderId = UUID.randomUUID().toString().substring(0, 8);

        System.out.printf("[OrderService] Order nº %s processed with success. Total: $%.2f%n",
                orderId, taxResp.totalAmount());

        return new OrderResponse(
                orderId,
                request.item(),
                request.quantity(),
                grossAmount,
                taxResp.taxAmount(),
                taxResp.shippingFee(),
                taxResp.totalAmount(),
                "PROCESSED"
        );
    }
}