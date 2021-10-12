package de.novareto.keycloak.external;

import lombok.Data;
import lombok.SneakyThrows;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Data
public class VirtualUserCredential {
    private String value;
    private String salt;
    private String algorithm = PasswordPolicy.HASH_ALGORITHM_DEFAULT;
    private Integer iterations = PasswordPolicy.HASH_ITERATIONS_DEFAULT;
    private String type = PasswordCredentialModel.TYPE;
    private MultivaluedHashMap<String, String> attributes;

    public static VirtualUserCredential fromPasswordCredentialModel(PasswordCredentialModel pcm) {
        VirtualUserCredential vuc = new VirtualUserCredential();
        vuc.setValue(pcm.getPasswordSecretData().getValue());
        vuc.setSalt(Base64.encodeBytes(pcm.getPasswordSecretData().getSalt()));
        vuc.setAlgorithm(pcm.getPasswordCredentialData().getAlgorithm());
        vuc.setIterations(pcm.getPasswordCredentialData().getHashIterations());
        return vuc;
    }

    @SneakyThrows
    public PasswordCredentialModel toPasswordCredentialModel() {
        return PasswordCredentialModel.createFromValues(
                this.getAlgorithm(), Base64.decode(this.getSalt()), this.getIterations(), this.getValue());
    }
}
