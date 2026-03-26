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

@Path("/modaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ModAccountResource {
    private static final Logger LOG = Logger.getLogger(ModAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public ModAccountResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modAccount(ModAccountData data) {
        LOG.fine("Op5: modAccount");

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
        ModAccountInput input = data.input;

        if (input == null || input.username == null || input.username.isBlank()
                || input.attributes == null || input.attributes.isEmpty()) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();
        }

        // Username não pode ser alterado (é a primary key)
        if (input.attributes.containsKey("username")) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();
        }
        String targetUsername = input.username;

        Key targetKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
        Entity targetUser = datastore.get(targetKey);

        if (targetUser == null) {
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.USER_NOT_FOUND, ErrorCodes.USER_NOT_FOUND_MSG)))
                    .build();
        }
        String callerRole = session.getString("role");
        String callerUsername = session.getString("username");
        String targetRole = targetUser.getString("user_role");

        if (callerRole.equals("USER")) {
            // USER só pode modificar a sua própria conta
            if (!callerUsername.equals(targetUsername)) {
                return Response.ok()
                        .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                        .build();
            }
        } else if (callerRole.equals("BOFFICER")) {
            // BOFFICER pode modificar a sua conta ou qualquer conta USER
            if (!callerUsername.equals(targetUsername) && !targetRole.equals("USER")) {
                return Response.ok()
                        .entity(g.toJson(new ResponseBuilder(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                        .build();
            }
        }
        Entity.Builder builder = Entity.newBuilder(targetUser);

        for (Map.Entry<String, String> entry : input.attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            switch (key) {
                case "phone":
                    builder.set("user_phone", value);
                    break;
                case "address":
                    builder.set("user_address", value);
                    break;
                default:
                    // Atributo desconhecido — ignorar ou dar erro
                    return Response.ok()
                            .entity(g.toJson(
                                    new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                            .build();
            }
        }

        datastore.put(builder.build());

        LOG.info("Account modified: " + targetUsername + " by " + callerUsername);

        Map<String, String> responseData = new LinkedHashMap<>();
        responseData.put("message", "Updated successfully");

        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseData)))
                .build();
    }
}
