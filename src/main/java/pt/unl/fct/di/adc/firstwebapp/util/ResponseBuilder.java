package pt.unl.fct.di.adc.firstwebapp.util;

public class ResponseBuilder {
    public String status;
    public Object data;

    public ResponseBuilder() { }

    public ResponseBuilder(String code, Object message) {
        this.status = code;
        this.data = message;
    }    
}
