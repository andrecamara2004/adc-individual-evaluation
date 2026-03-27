package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.datastore.*;

import com.google.gson.Gson;

@Path("/deleteaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DeleteAccountResource {

    private static final Logger LOG = Logger.getLogger(DeleteAccountResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public DeleteAccountResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAccount(DeleteAccountData data) {
        LOG.fine("Op4: deleteAccount");

        // Validate token
        AuthToken token = data.token;
        if (token == null || token.tokenID == null || token.username == null) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        Key sessionKey = datastore.newKeyFactory().setKind("Token").newKey(token.tokenID);
        Entity session = datastore.get(sessionKey);

        if (session == null) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        if (!session.getString("username").equals(token.username)) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        if (System.currentTimeMillis() > session.getLong("expiresAt")) {
            datastore.delete(sessionKey);
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.TOKEN_EXPIRED, ErrorCodes.TOKEN_EXPIRED_MSG)))
                    .build();
        }

        // check role
        String callerRole = session.getString("role");
        if (!callerRole.equals("ADMIN")) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();
        }

        DeleteAccountInput input = data.input;

        // Validate input
        if (input == null || input.username == null || input.username.isBlank()) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();
        }

        // Delete user and associated tokens
        String targetUsername = input.username;

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
        Entity user = datastore.get(userKey);
        if (user == null) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.USER_NOT_FOUND, ErrorCodes.USER_NOT_FOUND_MSG)))
                    .build();
        }

        datastore.delete(userKey);

        Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", targetUsername))
                .build();
        QueryResults<Entity> tokens = datastore.run(tokenQuery);

        List<Key> keysToDelete = new ArrayList<>();
        while (tokens.hasNext()) {
            keysToDelete.add(tokens.next().getKey());
        }
        if (!keysToDelete.isEmpty()) {
            datastore.delete(keysToDelete.toArray(new Key[0]));
        }

        LOG.info("Account deleted: " + targetUsername);

        Map<String, String> responseData = new LinkedHashMap<>();
        responseData.put("message", "Account deleted successfully");

        // return success
        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseData)))
                .build();
    }
}
