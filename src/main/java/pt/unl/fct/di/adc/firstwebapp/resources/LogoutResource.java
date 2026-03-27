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

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {
    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(LogoutRequest request) {
        LOG.fine("Op10: logout");

        AuthToken token = request.getToken();
        String targetUserId = request.getInput().getUserID();

        // Validate input
        if (targetUserId == null || targetUserId.isBlank())
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();

        // Validate token
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token.tokenID);
        Entity tokenEntity = datastore.get(tokenKey);
        if (tokenEntity == null) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        if (!tokenEntity.getString("username").equals(token.username)) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        if (System.currentTimeMillis() > tokenEntity.getLong("expiresAt")) {
            datastore.delete(tokenKey);
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.TOKEN_EXPIRED, ErrorCodes.TOKEN_EXPIRED_MSG)))
                    .build();
        }

        // Check access control:
        String callerRole = tokenEntity.getString("role");
        String callerUsername = tokenEntity.getString("username");

        if(!callerRole.equals("ADMIN") && !callerUsername.equals(targetUserId)) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();
        }

        // Delete token
        datastore.delete(tokenKey);

        // Return success response
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "Logout successful");

        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseMap)))
                .build();
    }
}
