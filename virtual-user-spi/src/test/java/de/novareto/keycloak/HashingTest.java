package de.novareto.keycloak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.io.IOException;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class HashingTest {

    @ParameterizedTest
    @CsvSource({
            "lZtleWv1DeEcNIr8MTzdeg==,/bVkHA4G/VOskjaCgNYyTz1BhgD+9X8bxCNEPGGpnDCJbWHDXIW0G8IlSBhDdJsXTjrXt/7yQguBwJ3sxSmSKA==",
            "dTnm2F75dNhNRT0yW9UcNw==,ldP3HIF5mf39q/lVSNyXPh/pcScKhEQTbPmnEjQcVVwMScX/nUWYh8gZ3zOXFdrB3Bvv50r51JDYhT48ymxz6A=="
    })
    public void testHashing(String salt, String encodedPassword) throws IOException {
        Pbkdf2Sha256PasswordHashProviderFactory factory = new Pbkdf2Sha256PasswordHashProviderFactory();
        PasswordHashProvider hashProvider = factory.create(null);

        PasswordCredentialModel credentialModel = PasswordCredentialModel.createFromValues(
                PasswordPolicy.HASH_ALGORITHM_DEFAULT,
                Base64.decode(salt),
                PasswordPolicy.HASH_ITERATIONS_DEFAULT,
                encodedPassword
        );

        boolean verify = hashProvider.verify("test", credentialModel);
        Assertions.assertTrue(verify);
    }

}
