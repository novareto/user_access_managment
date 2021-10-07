package de.novareto.keycloak.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.keycloak.common.util.MultivaluedHashMap;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualUser {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private MultivaluedHashMap<String, String> attributes;
    private List<String> groups;
}
