package io.github.dajoh2062.traveldb.api;

public final class InvalidRequestParameterException extends RuntimeException {

    private final String parameter;

    public InvalidRequestParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
