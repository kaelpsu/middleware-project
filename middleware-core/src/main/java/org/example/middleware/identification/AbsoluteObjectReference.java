package org.example.middleware.identification;

public record AbsoluteObjectReference (
    String host,
    int port,
    ObjectID objectID
) {

    public String getBaseUrl() {
        return String.format("http://%s:%d", host, port);
    }

    public String getURI() {
        return String.format("http://%s:%d/%s", host, port, objectID.value());
    }

    @Override
    public String toString() {
        return getURI();
    }
}
