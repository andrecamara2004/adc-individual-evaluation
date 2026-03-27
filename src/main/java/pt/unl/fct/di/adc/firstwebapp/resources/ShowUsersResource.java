package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;

import com.google.gson.Gson;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUsersResource {
    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ShowUsersResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(ShowUsersRequest data) {
        LOG.fine("Op3: showUsers");

        // Validate token
        AuthToken token = data.token;
        if (token == null || token.tokenID == null || token.username == null) {
            return Response.status(Status.BAD_REQUEST)
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
        if (!callerRole.equals("ADMIN") && !callerRole.equals("BOFFICER")) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();
        }

        // query users
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .build();
        QueryResults<Entity> results = datastore.run(query);

        List<Map<String, String>> usersList = new ArrayList<>();
        while (results.hasNext()) {
            Entity user = results.next();
            Map<String, String> userMap = new LinkedHashMap<>();
            userMap.put("username", user.getKey().getName());
            userMap.put("role", user.getString("user_role"));
            usersList.add(userMap);
        }

        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("users", usersList);

        // return result
        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseData)))
                .build();
    }
}
