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

@Path("/showauthsessions")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowAuthSessionsResource {

    private static final Logger LOG = Logger.getLogger(ShowAuthSessionsResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public ShowAuthSessionsResource() { }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showAuthSessions(ShowAuthSessionsData data) {
        LOG.fine("Op6: showAuthSessions");

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

        // Check if user is admin
        String callerRole = session.getString("role");
        if (!callerRole.equals("ADMIN")) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();
        }

        // Query all active sessions
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .build();
        QueryResults<Entity> results = datastore.run(query);

        List<Map<String, Object>> sessionsList = new ArrayList<>();
        long now = System.currentTimeMillis();

        while (results.hasNext()) {
            Entity s = results.next();
            long expiresAt = s.getLong("expiresAt");

            if (now > expiresAt) continue;

            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("tokenId", s.getString("tokenID"));
            sm.put("username", s.getString("username"));
            sm.put("role", s.getString("role"));
            sm.put("expiresAt", expiresAt);
            sessionsList.add(sm);
        }

        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("sessions", sessionsList);

        // return result
        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseData)))
                .build();
    }
}

