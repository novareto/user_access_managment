package de.novareto.keycloak.forms;

import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.beans.MessageType;

import javax.ws.rs.core.Response;
import java.util.Objects;

import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PASSWORD;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class VirtualUserLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    public VirtualUserLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }

    @Override
    public Response createResponse(UserModel.RequiredAction action) {
        if (!action.equals(UPDATE_PASSWORD)) {
            return super.createResponse(action);
        }

        String actionMessage;
        boolean isRequestedByAdmin = user.getRequiredActionsStream().filter(Objects::nonNull).anyMatch(UPDATE_PASSWORD.toString()::contains);
        if (isRequestedByAdmin) {
            String passwordUpdateMessage = user.getFirstAttribute("passwordUpdateMessage");
            actionMessage = (passwordUpdateMessage != null && !passwordUpdateMessage.isBlank())
                    ? passwordUpdateMessage : Messages.UPDATE_PASSWORD;
        } else {
            actionMessage = Messages.RESET_PASSWORD;
        }

        if (messages == null) {
            setMessage(MessageType.WARNING, actionMessage);
        }

        return createResponse(LoginFormsPages.LOGIN_UPDATE_PASSWORD);
    }

}
