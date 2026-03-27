package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeUserRoleRequest {
    private ChangeUserRoleData input;
    private AuthToken token;

    public ChangeUserRoleData getInput() {
        return input;
    }

    public void setInput(ChangeUserRoleData input) {
        this.input = input;
    }

    public AuthToken getToken() {
        return token;
    }

    public void setToken(AuthToken token) {
        this.token = token;
    }
}
