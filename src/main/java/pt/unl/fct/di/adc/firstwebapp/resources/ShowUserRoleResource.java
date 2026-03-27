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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import com.google.gson.Gson;

@Path("/showuserrole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUserRoleResource {

    private static final Logger LOG = Logger.getLogger(ShowUserRoleResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUserRole(ShowUserRoleRequest request) {
        LOG.fine("Op7: showUserRole");

        AuthToken token = request.getToken();
        String targetUserId = request.getInput().getUsername();

        // Validate input
        if (targetUserId == null || targetUserId.isBlank())
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();

        // Validade token
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

        // Check if caller is BOFFICER or ADMIN
        String callerRole = tokenEntity.getString("role");
        if (!callerRole.equals("BOFFICER") && !callerRole.equals("ADMIN"))
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();

        // Get target user
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(targetUserId);
        Entity userEntity = datastore.get(userKey);

        if (userEntity == null)
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.USER_NOT_FOUND, ErrorCodes.USER_NOT_FOUND_MSG)))
                    .build();

        // Return user role
        String role = userEntity.getString("user_role");

        Map<String, String> responseData = new HashMap<>();
        responseData.put("userId", targetUserId);
        responseData.put("role", role);

        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseData)))
                .build();
    }
}