package de.novareto.keycloak.external;

import lombok.Value;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Value
public class VirtualUserCredential {
    String userId;
    String value;
    String type = PasswordCredentialModel.TYPE;
    Map<String, Object> attributes;

    public VirtualUserCredential(String userId, String value) {
        this.userId = userId;
        this.value = value;
        this.attributes = null;
    }

    public VirtualUserCredential(String userId, String value, Map<String, Object> attributes) {
        this.userId = userId;
        this.value = value;
        this.attributes = attributes;
    }
}
