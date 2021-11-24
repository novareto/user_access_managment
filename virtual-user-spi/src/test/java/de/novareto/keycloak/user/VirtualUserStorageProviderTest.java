package de.novareto.keycloak.user;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class VirtualUserStorageProviderTest {

    @ParameterizedTest
    @CsvSource({
            "username@localhost,true",
            "username@domain.com,true",
            "user.name@domain.com,true",
            "user-name@domain.com,true",
            "username@domain.co.in,true",
            "user_name@domain.com,true",
            "username#domain.com,false",
            "username@domain'.com,false",
            "username@domain|.com,false"
    })
    public void testEmailAddresses(String email, boolean isValid) {
        boolean result = VirtualUserStorageProvider.isEmailAddressRFC5322(email);
        assertEquals(isValid, result);
    }
}
