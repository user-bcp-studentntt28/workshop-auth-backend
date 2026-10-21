package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        assertSuccessfulLogin("student", "Workshop123!", "Workshop Student", "STUDENT");
    }

    @Test
    void logsInWithTrainerAndPreservesPersistedFields() {
        assertSuccessfulLogin("trainer", "Trainer123!", "Workshop Trainer", "TRAINER");
    }

    @Test
    void rejectsNullRequestWithBaselineError() {
        assertLoginError("null", 400, "Request is null");
    }

    @Test
    void rejectsMissingOrInvalidUsernameBeforeOtherFields() {
        List<String> requests = List.of(
                "{\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}",
                "{\"username\":null,\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}",
                "{\"username\":\"\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}",
                "{\"username\":\"   \",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}"
        );
        requests.forEach(request -> assertLoginError(request, 400, "Username is required"));
        assertLoginError("{\"password\":null}", 400, "Username is required");
    }

    @Test
    void rejectsMissingOrInvalidPasswordAfterUsername() {
        List<String> requests = List.of(
                "{\"username\":\"student\",\"client\":\"angular-workshop\"}",
                "{\"username\":\"student\",\"password\":null,\"client\":\"angular-workshop\"}",
                "{\"username\":\"student\",\"password\":\"\",\"client\":\"angular-workshop\"}",
                "{\"username\":\"student\",\"password\":\"   \",\"client\":\"angular-workshop\"}"
        );
        requests.forEach(request -> assertLoginError(request, 400, "Password is required"));
        assertLoginError("{\"username\":\"student\",\"password\":null}", 400, "Password is required");
    }

    @Test
    void invalidCredentialCasesKeepUnauthorizedContract() {
        assertLoginError(
                "{\"username\":\"student\",\"password\":\"wrong-password\",\"client\":\"angular-workshop\"}",
                401,
                "Invalid user student or password"
        );
        assertLoginError(
                "{\"username\":\"unknown-user\",\"password\":\"not-empty\",\"client\":\"angular-workshop\"}",
                401,
                "Invalid user unknown-user or password"
        );
    }

    @Test
    void preservesEscapingAndRoleForSyntheticDisplayName() {
        UserRecord syntheticUser = new UserRecord(99, "synthetic-trainer", "<Trainer>", "TRAINER");
        jakarta.ws.rs.core.Response response = invokeResource(
                new StubLegacyDatabase(syntheticUser, null),
                "synthetic-trainer",
                "synthetic-password"
        );

        assertEquals(200, response.getStatus());
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        Map<?, ?> user = (Map<?, ?>) body.get("user");
        assertEquals("synthetic-trainer", user.get("username"));
        assertEquals("&lt;Trainer&gt;", user.get("displayName"));
        assertEquals("TRAINER", user.get("role"));
    }

    @Test
    void preservesInternalErrorBodyWhenFailureHasNoMessage() {
        assertInternalError(new RuntimeException(), "Unknown error");
    }

    @Test
    void preservesInternalErrorBodyWhenFailureHasMessage() {
        assertInternalError(new RuntimeException("Test failure"), "Test failure");
    }

    @Test
    void healthIsPublicAndKeepsBaselineStructure() {
        Response response = given().when().get("/q/health");

        assertEquals(200, response.statusCode());
        Map<?, ?> body = response.as(Map.class);
        assertTrue(body.get("status") instanceof String);
        assertTrue(body.get("checks") instanceof List<?>);
    }

    @Test
    void characterizesIgnoredClientAndLegacyTokenLayout() {
        Response withoutClient = postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\"}");
        Response withClient = postLogin(
                "{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"unused-client-value\"}"
        );

        assertEquals(200, withoutClient.statusCode());
        assertEquals(200, withClient.statusCode());
        String firstPayload = decodeToken(withoutClient.jsonPath().getString("token"));
        String secondPayload = decodeToken(withClient.jsonPath().getString("token"));
        String[] firstParts = firstPayload.split("\\|", -1);
        String[] secondParts = secondPayload.split("\\|", -1);

        assertEquals(4, firstParts.length);
        assertEquals(4, secondParts.length);
        assertEquals("student", firstParts[0]);
        assertEquals("STUDENT", firstParts[1]);
        assertDoesNotThrow(() -> Long.parseLong(firstParts[2]));
        assertFalse(firstParts[3].isBlank());
        assertEquals(firstParts[0], secondParts[0]);
        assertEquals(firstParts[1], secondParts[1]);
        assertFalse(secondPayload.contains("unused-client-value"));
    }

    private void assertSuccessfulLogin(String username, String password, String displayName, String role) {
        Response response = postLogin(
                "{\"username\":\"" + username + "\",\"password\":\"" + password
                        + "\",\"client\":\"angular-workshop\"}"
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.getContentType().startsWith("application/json"));
        Object expiresIn = response.jsonPath().get("expiresIn");
        assertNotNull(expiresIn);
        assertEquals(Integer.class, expiresIn.getClass());
        assertEquals(3600, expiresIn);
        assertEquals(username, response.jsonPath().getString("user.username"));
        assertEquals(displayName, response.jsonPath().getString("user.displayName"));
        assertEquals(role, response.jsonPath().getString("user.role"));
        assertEquals("authenticated-with-legacy-flow", response.jsonPath().getString("debug"));
        assertNotNull(response.jsonPath().getString("token"));
        assertFalse(response.jsonPath().getString("token").isBlank());
    }

    private void assertLoginError(String request, int status, String message) {
        Response response = postLogin(request);

        assertEquals(status, response.statusCode());
        Map<?, ?> body = response.as(Map.class);
        assertEquals(message, body.get("message"));
        assertFalse(body.containsKey("token"));
    }

    private void assertInternalError(RuntimeException failure, String message) {
        jakarta.ws.rs.core.Response response = invokeResource(
                new StubLegacyDatabase(null, failure),
                "synthetic-user",
                "synthetic-password"
        );

        assertEquals(500, response.getStatus());
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        assertEquals(message, body.get("message"));
        assertEquals(RuntimeException.class.getName(), body.get("exception"));
        assertFalse(body.containsKey("token"));
    }

    private jakarta.ws.rs.core.Response invokeResource(LegacyDatabase database, String username, String password) {
        AuthResource resource = new AuthResource();
        resource.database = database;
        LoginRequest request = new LoginRequest();
        request.username = username;
        request.password = password;
        request.client = "characterization-client";
        return resource.login(request);
    }

    private Response postLogin(String body) {
        return given()
                .contentType("application/json")
                .body(body)
                .when().post("/auth/login");
    }

    private String decodeToken(String token) {
        return new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
    }

    private static final class StubLegacyDatabase extends LegacyDatabase {
        private final UserRecord user;
        private final RuntimeException failure;

        private StubLegacyDatabase(UserRecord user, RuntimeException failure) {
            this.user = user;
            this.failure = failure;
        }

        @Override
        public UserRecord findByCredentials(String username, String password) {
            if (failure != null) {
                throw failure;
            }
            return user;
        }
    }
}
