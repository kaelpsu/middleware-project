package org.example.middleware.identification;

public record AbsoluteObjectReference (
    String protocol,
    String host,
    int port,
    ObjectID objectID
) {

    public String getURI() {
        return String.format("%s://%s:%d/%s", protocol, host, port, objectID.value());
    }

    @Override
    public String toString() {
        return getURI();
    }
}
