package de.novareto.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
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
@Slf4j
@Testcontainers
public class VirtualUserProviderTest {

    static final String REALM = "novareto";

    static Network network = Network.newNetwork();

    @Container
    public static KeycloakContainer keycloak = new KeycloakContainer()
            .withRealmImportFile("/novareto-realm.json")
            .withProviderClassesFrom("target/classes")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withNetwork(network);

    @Container
    public static GenericContainer<?> apiMock = new GenericContainer<>(DockerImageName.parse("muonsoft/openapi-mock:latest"))
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forHostPath("../api/virtualUserStorage.yaml"), "/tmp/spec.yaml")
            .withEnv(Map.of(
                    "OPENAPI_MOCK_SPECIFICATION_URL", "/tmp/spec.yaml",
                    "OPENAPI_MOCK_USE_EXAMPLES", "if_present"
            ))
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withNetwork(network)
            .withNetworkAliases("api");


    @ParameterizedTest
    @ValueSource(strings = { "master", REALM })
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

        UsersResource users = kcAdmin.realm(REALM).users();
        assertThat(users.count(), is(123));

        List<UserRepresentation> search = users.search("john.doe@example.com", 1, 20);
        assertThat(search, is(not(empty())));

        String userId = search.get(0).getId();
        UserRepresentation user = users.get(userId).toRepresentation();
        assertThat(user.getEmail(), is("john.doe@example.com"));
        assertThat(user.firstAttribute("foo"), is("bar"));
        assertThat(user.getAttributes().get("foo").get(1), is("baz"));

        boolean matchGroup = users.get(userId).groups().stream().anyMatch(group -> "Editors".equals(group.getName()));
        assertTrue(matchGroup);
    }

    @ParameterizedTest
    @ValueSource(strings = { "john.doe@example.com", "john.doe" })
    public void testLoginAsUserAndCheckAccessToken(String userIdentifier) throws IOException {
        String tokenEndpoint = getTokenEndpoint();
        String accessTokenString = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", userIdentifier)
                .formParam("password", "test")
                .formParam("grant_type", "password")
                .formParam("client_id", "admin-cli")
                .formParam("scope", "openid")
                .when().post(tokenEndpoint)
                .then().statusCode(200)
                .extract().path("access_token");

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<>() {};

        byte[] tokenPayload = Base64.getDecoder().decode(accessTokenString.split("\\.")[1]);
        Map<String, Object> payload = mapper.readValue(tokenPayload, typeRef);

        assertThat(payload.get("preferred_username"), is("john.doe@example.com"));
        assertThat(payload.get("email"), is("john.doe@example.com"));
        assertThat(payload.get("given_name"), is("John"));
        assertThat(payload.get("family_name"), is("Doe"));
    }

    @Test
    public void testLoginAsUserWithInvalidPassword() {
        String tokenEndpoint = getTokenEndpoint();
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "john.doe@example.com")
                .formParam("password", "invalid")
                .formParam("grant_type", "password")
                .formParam("client_id", "admin-cli")
                .formParam("scope", "openid")
                .when().post(tokenEndpoint)
                .then().statusCode(401);
    }

    private String getTokenEndpoint() {
        return given().when().get(keycloak.getAuthServerUrl() + "/realms/" + REALM + "/.well-known/openid-configuration")
                .then().extract().path("token_endpoint");
    }

}
