package de.novareto.keycloak.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
    private Map<String, Object> attributes;
    private List<String> groups;
}
