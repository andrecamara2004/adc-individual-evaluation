package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.util.CreateAccountRequest;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorCodes;
import pt.unl.fct.di.adc.firstwebapp.util.ResponseBuilder;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;

@Path("/createaccount")
public class CreateAccountResource {

	private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public CreateAccountResource() {
	} // Default constructor, nothing to do

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createAccount(CreateAccountRequest request) {
		RegisterData data = request.input;
		LOG.fine("Attempt to create account with username: " + data.username);

		if (!data.validRegistration())
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG))).build();

		try {
			Transaction txn = datastore.newTransaction();
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT)
						.entity(g.toJson(
								new ResponseBuilder(ErrorCodes.USER_ALREADY_EXISTS, ErrorCodes.USER_ALREADY_EXISTS_MSG)))
						.build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_phone", data.phone)
						.set("user_address", data.address)
						.set("user_role", data.role)
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.put(user);
				txn.commit();
				LOG.info("User registered " + data.username);

                Map<String, String> responseData = new HashMap<>();
                responseData.put("username", data.username);
                responseData.put("role", data.role);

				return Response.ok(g.toJson(new ResponseBuilder("success", responseData))).build();
			}
		} catch (Exception e) {
			LOG.severe("Error registering user: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error registering user.").build();
		} finally {
			// No need to rollback here, as we only have one transaction and it will be
			// automatically rolled back if not committed.
		}
	}
}