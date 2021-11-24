package de.novareto.keycloak.user;

import de.novareto.keycloak.external.VirtualStorageService;
import de.novareto.keycloak.external.VirtualUser;
import de.novareto.keycloak.external.VirtualUserCredential;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@JBossLog
public class VirtualUserStorageProvider implements UserStorageProvider,
        UserLookupProvider.Streams, UserQueryProvider.Streams,
        CredentialInputValidator, CredentialInputUpdater,
        UserRegistrationProvider {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final VirtualStorageService service;

    // map of loaded users in this transaction
    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public VirtualUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        this.service = new VirtualStorageService(session, model);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        log.debugf("Try to update credentials type %s for user %s.", input.getType(), user.getId());
        if (!(input instanceof UserCredentialModel)) return false;
        if (!supportsCredentialType(input.getType())) return false;

        UserCredentialModel cred = (UserCredentialModel) input;

        PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
        PasswordHashProvider passwordHashProvider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        PasswordCredentialModel passwordCredentialModel =
                passwordHashProvider.encodedCredential(cred.getChallengeResponse(), passwordPolicy.getHashIterations());

        VirtualUserCredential virtualUserCredential = VirtualUserCredential.fromPasswordCredentialModel(passwordCredentialModel);

        log.debugf("Sending updateCredential request for userId %s", user.getId());
        log.tracef("Payload for updateCredential request: %s", virtualUserCredential);
        return service.updateCredentialData(StorageId.externalId(user.getId()), virtualUserCredential);
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Set.of();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        VirtualUserCredential vuCredentialData;
        try {
            vuCredentialData = service.getCredentialData(StorageId.externalId(user.getId()));
            log.debugf("Received credential data for userId %s: %s", user.getId(), vuCredentialData);
            if (vuCredentialData == null) {
                return false;
            }
        } catch (WebApplicationException e) {
            log.errorf(e, "Request to verify credentials for userId %s failed with response status %d",
                    user.getId(), e.getResponse().getStatus());
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel) input;

        if ("plain".equalsIgnoreCase(vuCredentialData.getAlgorithm())) {
            return cred.getChallengeResponse().equals(vuCredentialData.getValue());
        }

        PasswordCredentialModel passwordCredentialModel = vuCredentialData.toPasswordCredentialModel();
        PasswordHashProvider passwordHashProvider = session.getProvider(PasswordHashProvider.class, vuCredentialData.getAlgorithm());
        boolean isValid = passwordHashProvider.verify(cred.getChallengeResponse(), passwordCredentialModel);
        log.debugf("Password validation result: %b", isValid);
        return isValid;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        log.debugf("getUserById: %s", id);
        String externalId = StorageId.externalId(id);
        return findUser(realm, externalId, service::findUserById);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        log.debugf("getUserByUsername: %s", username);
        return findUser(realm, username, service::findUserById);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        log.debugf("getUserByEmail: %s", email);
        return findUser(realm, email, service::findUserByEmail);
    }

    private UserModel findUser(RealmModel realm, String identifier, Function<String, VirtualUser> fnFindUser) {
        UserModel adapter = loadedUsers.get(identifier);
        if (adapter == null) {
            VirtualUser user = fnFindUser.apply(identifier);
            log.debugf("Received user data for identifier <%s> from API: %s", identifier, user);
            if (user != null) {
                adapter = new VirtualUserAdapter(session, realm, model, user);
                loadedUsers.put(identifier, adapter);
            }
        } else {
            log.debugf("Found user data for %s in loadedUsers.", identifier);
        }
        return adapter;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return service.getUsersCount();
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return toUserModelStream(service.getUsers(firstResult, maxResults), realm);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return toUserModelStream(service.searchUsers(search, firstResult, maxResults), realm);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        return toUserModelStream(service.getUsers(firstResult, maxResults), realm);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        // not used, but fixed return value to prevent internal errors
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        // not used, but fixed return value to prevent internal errors
        return false;
    }

    private Stream<UserModel> toUserModelStream(List<VirtualUser> virtualUsers, RealmModel realm) {
        log.debugf("Received %d users from API", virtualUsers.size());
        return virtualUsers.stream().map(user -> new VirtualUserAdapter(session, realm, model, user));
    }
}
