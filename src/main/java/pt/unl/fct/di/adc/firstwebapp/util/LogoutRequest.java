package pt.unl.fct.di.adc.firstwebapp.util;

public class LogoutRequest {
    private LogoutData input;
    private AuthToken token;

    public LogoutData getInput() {
        return input;
    }

    public void setInput(LogoutData input) {
        this.input = input;
    }

    public AuthToken getToken() {
        return token;
    }

    public void setToken(AuthToken token) {
        this.token = token;
    }
}
