package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	//15minutes
	public static final long EXPIRATION_TIME = 15 * 60 * 1000;
	
	public String tokenID;
	public String username;
	public String role;
	public long issuedAt;
	public long expiresAt;
	
	public AuthToken() { }
	
	public AuthToken(String username, String role) {
		this.tokenID = UUID.randomUUID().toString();
		this.username = username;
		this.role     = role;
		this.issuedAt = System.currentTimeMillis();
		this.expiresAt = this.issuedAt + EXPIRATION_TIME;
	}
	
}