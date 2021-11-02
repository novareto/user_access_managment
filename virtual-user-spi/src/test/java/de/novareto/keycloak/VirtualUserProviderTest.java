package de.novareto.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Testcontainers
public class VirtualUserProviderTest {

    static Network network = Network.newNetwork();

    @Container
    public static KeycloakContainer keycloak = new KeycloakContainer()
            .withRealmImportFile("/novareto-realm.json")
            .withProviderClassesFrom("target/classes")
            .withNetwork(network);

    @Container
    public static GenericContainer<?> apiMock = new GenericContainer<>(DockerImageName.parse("muonsoft/openapi-mock:latest"))
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forHostPath("../api/virtualUserStorage.yaml"), "/tmp/spec.yaml")
            .withEnv(Map.of(
                    "OPENAPI_MOCK_SPECIFICATION_URL", "/tmp/spec.yaml",
                    "OPENAPI_MOCK_USE_EXAMPLES", "if_present"
            ))
            .withNetwork(network)
            .withNetworkAliases("api");


    @ParameterizedTest
    @ValueSource(strings = { "master", "novareto" })
    public void testRealms(String realm) {
        String accountServiceUrl = given().when().get(keycloak.getAuthServerUrl() + "/realms/" + realm)
                .then().statusCode(200).body("realm", equalTo(realm))
                .extract().path("account-service");

        given().when().get(accountServiceUrl).then().statusCode(200);
    }

    @Test
    public void testAccessingUsersAsAdmin() {
        Keycloak kcAdmin = KeycloakBuilder.builder()
                .serverUrl(keycloak.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(keycloak.getAdminUsername())
                .password(keycloak.getAdminPassword())
                .build();

        UsersResource users = kcAdmin.realm("novareto").users();
        assertThat(users.count(), is(123));

        List<UserRepresentation> search = users.search("john.doe@example.com");
        assertThat(search, is(not(empty())));

        String userId = search.get(0).getId();
        UserRepresentation user = users.get(userId).toRepresentation();
        assertThat(user.getEmail(), is("john.doe@example.com"));
        assertThat(user.firstAttribute("foo"), is("bar"));
        assertThat(user.getAttributes().get("foo").get(1), is("baz"));

        boolean matchGroup = users.get(userId).groups().stream().anyMatch(group -> "Editors".equals(group.getName()));
        assertTrue(matchGroup);
    }

}
