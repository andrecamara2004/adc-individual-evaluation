package pt.unl.fct.di.adc.firstwebapp.util;

public class LoginData {
	
	public String username;
	public String password;
	public String role;
	
	public LoginData() { }
	
	public LoginData(String username, String password) {
		this.username = username;
		this.password = password;
	}


	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validLogin() {
		return nonEmptyOrBlankField(username) &&
				nonEmptyOrBlankField(password) &&
				username.contains("@");
	}
}