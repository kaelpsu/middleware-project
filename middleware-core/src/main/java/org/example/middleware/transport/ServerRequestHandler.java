package org.example.middleware.transport;

import org.example.middleware.broker.Invoker;
import org.example.middleware.marshaller.Marshaller;
import org.example.middleware.transport.http.*;

import java.io.ByteArrayInputStream;

public class ServerRequestHandler implements RequestHandler {

    private final Invoker invoker;

    public ServerRequestHandler(Invoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public byte[] handle(byte[] requestBytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBytes);
            HttpRequest request = HttpParser.parse(inputStream);
            
            if (request == null) {
                return HttpResponse.internalServerError("{\"error\":\"Bad/Empty Request\"}").getBytes();
            }

            Object result = invoker.dispatch(request);

            String jsonResponse = Marshaller.marshal(result);

            return HttpResponse.ok(jsonResponse).getBytes();

        } catch (NoSuchMethodException e) {
            return HttpResponse.notFound(e.getMessage()).getBytes();
        } catch (Exception e) {
            System.err.println("[ServerRequestHandler] Server Error: " + e.getMessage());
            return HttpResponse.internalServerError("{\"error\":\"" + e.getMessage() + "\"}").getBytes();
        }
    }
}