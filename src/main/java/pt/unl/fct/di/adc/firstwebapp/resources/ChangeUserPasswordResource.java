package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.datastore.*;

import com.google.gson.Gson;

@Path("/changeuserpwd")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeUserPasswordResource {
    private static final Logger LOG = Logger.getLogger(ChangeUserPasswordResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeUserPassword(ChangeUserPasswordRequest request) {
        LOG.fine("Op9: changeUserPassword");

        AuthToken token = request.getToken();
        String targetUserId = request.getInput().getUsername();
        String oldPassword = request.getInput().getOldPassword();
        String newPassword = request.getInput().getNewPassword();

        // Validate input
        if (targetUserId == null || targetUserId.isBlank())
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();

        if (oldPassword == null || oldPassword.isBlank())
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();

        if (newPassword == null || newPassword.isBlank())
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

        if (!tokenEntity.getString("username").equals(token.username))
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();

        if (System.currentTimeMillis() > tokenEntity.getLong("expiresAt")) {
            datastore.delete(tokenKey);
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.TOKEN_EXPIRED, ErrorCodes.TOKEN_EXPIRED_MSG)))
                    .build();
        }

        // Check access control:
        // Nota: só deve ser possível a um utilizador (independentemente do seu role),
        // mudar a password da sua própria conta
        String callerUsername = tokenEntity.getString("username");
        if (!callerUsername.equals(targetUserId))
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

        // Check old password
        String storedPassword = userEntity.getString("user_pwd");
        String oldPasswordHash = DigestUtils.sha512Hex(oldPassword);
        if (!storedPassword.equals(oldPasswordHash))
            return Response.ok()
                    .entity(g.toJson(new ResponseBuilder(ErrorCodes.INVALID_CREDENTIALS, ErrorCodes.INVALID_CREDENTIALS_MSG)))
                    .build();

        // Update password
        String newPasswordHash = DigestUtils.sha512Hex(newPassword);
        Entity updatedUser = Entity.newBuilder(userEntity)
                .set("user_pwd", newPasswordHash)
                .build();
        datastore.update(updatedUser);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "Password changed successfully");
        return Response.ok()
                .entity(g.toJson(new ResponseBuilder("success", responseData)))
                .build();
    }
}
