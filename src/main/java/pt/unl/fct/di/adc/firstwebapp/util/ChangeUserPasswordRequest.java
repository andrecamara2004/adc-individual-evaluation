package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeUserPasswordRequest {
    private ChangeUserPasswordData input;
    private AuthToken token;

    public ChangeUserPasswordData getInput() {
        return input;
    }

    public void setInput(ChangeUserPasswordData input) {
        this.input = input;
    }

    public AuthToken getToken() {
        return token;
    }

    public void setToken(AuthToken token) {
        this.token = token;
    }
}