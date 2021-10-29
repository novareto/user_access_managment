package de.novareto.keycloak.external;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface VirtualStorageClient {

    @GET
    @Path("/users")
    List<VirtualUser> getUsers(@QueryParam("first") @DefaultValue("0") Integer first,
                               @QueryParam("max") @DefaultValue("20") Integer max,
                               @QueryParam("email") String email,
                               @QueryParam("search") String search);

    @GET
    @Path("/users/count")
    Map<String, Integer> getUsersCount();

    @GET
    @Path("/users/{userId}")
    VirtualUser getUserById(@PathParam("userId") String userId);

    @GET
    @Path("/credentials/{userId}")
    VirtualUserCredential getCredentialModel(@PathParam("userId") String userId);

    @PUT
    @Path("/credentials/{userId}")
    Response updateCredentialModel(@PathParam("userId") String userId, VirtualUserCredential credential);

}
