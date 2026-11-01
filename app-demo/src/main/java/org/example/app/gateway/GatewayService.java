package org.example.app.gateway;


import org.example.app.model.OrderRequest;
import org.example.app.model.OrderResponse;
import org.example.app.model.TaxCalcRequest;
import org.example.app.model.TaxCalcResponse;
import org.example.middleware.annotations.controller.Body;
import org.example.middleware.annotations.controller.MethodMapping;
import org.example.middleware.annotations.controller.RequestMapping;
import org.example.middleware.annotations.lifecycle.StaticInstance;
import org.example.middleware.client.Requestor;
import org.example.middleware.identification.AbsoluteObjectReference;
import org.example.middleware.identification.HeartbeatSignal;
import org.example.middleware.identification.Lookup;
import org.example.middleware.transport.http.MethodHTTP;

@RequestMapping(path = "/gateway")
@StaticInstance
public class GatewayService {

    private final Lookup registry = new Lookup();
    private final Requestor requestor = new Requestor();

    @MethodMapping(path = "/heartbeat", method = MethodHTTP.POST)
    public String receiveHeartbeat(@Body HeartbeatSignal signal) {
        if (signal != null && signal.serviceName() != null) {
            registry.registerReference(signal.host(), signal.port(), signal.serviceName());
            return "{\"status\":\"ACK\"}";
        }
        return "{\"status\":\"REJECTED\"}";
    }

    @MethodMapping(path = "/order", method = MethodHTTP.POST)
    public OrderResponse routeOrder(@Body OrderRequest request) throws Exception {
        AbsoluteObjectReference targetNode = registry.lookup("order");
        if (targetNode == null) {
            throw new IllegalStateException("No OrderService instances available.");
        }

        System.out.printf("[Gateway] Redirecting order to %s:%d%n", targetNode.host(), targetNode.port());
        
        return requestor.invoke(
                "TCP",
                targetNode.host(), 
                targetNode.port(), 
                "/order/process", 
                MethodHTTP.POST, 
                request, 
                OrderResponse.class
        );
    }

    @MethodMapping(path = "/taxcalc", method = MethodHTTP.POST)
    public TaxCalcResponse routeTax(@Body TaxCalcRequest request) throws Exception {
        AbsoluteObjectReference targetNode = registry.lookup("tax");
        if (targetNode == null) {
            throw new IllegalStateException("No TaxService instances available.");
        }

        System.out.printf("[Gateway] Redirecting tax calculation to %s:%d%n", targetNode.host(), targetNode.port());

        return requestor.invoke(
                "TCP",
                targetNode.host(), 
                targetNode.port(), 
                "/tax/calculate", 
                MethodHTTP.POST, 
                request, 
                TaxCalcResponse.class
        );
    }
}