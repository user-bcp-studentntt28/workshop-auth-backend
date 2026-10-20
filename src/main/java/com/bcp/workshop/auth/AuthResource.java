package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final String SESSION_COOKIE = "workshop_session";

    @Inject
    LegacyDatabase database;

    @ConfigProperty(name = "app.allowed-origin", defaultValue = "http://localhost:4200")
    String allowedOrigin;

    @ConfigProperty(name = "auth.session-cookie.secure", defaultValue = "false")
    boolean secureCookie;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request, @HeaderParam("Origin") String origin) {
        if (!allowedOrigin.equals(origin)) {
            return Response.status(403).entity(Map.of("message", "Origin not allowed")).build();
        }
        try {
            if (request == null) {
                return Response.status(400).entity(Map.of("message", "Request is null")).build();
            }
            if (request.username == null || request.username.trim().isEmpty()) {
                return Response.status(400).entity(Map.of("message", "Username is required")).build();
            }
            if (request.password == null || request.password.trim().isEmpty()) {
                return Response.status(400).entity(Map.of("message", "Password is required")).build();
            }

            UserRecord user = database.findByCredentials(request.username, request.password);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("message", "Invalid user " + request.username + " or password"))
                        .build();
            }

            String token = LegacyTokenUtil.create(user.username(), user.role());
            Map<String, Object> response = new HashMap<>();
            response.put("expiresIn", 3600);
            response.put("user", Map.of(
                    "username", user.username(),
                    "displayName", StringEscapeUtils.escapeHtml4(user.displayName()),
                    "role", user.role()
            ));
            response.put("debug", "authenticated-with-legacy-flow");
            return Response.ok(response)
                    .header("Set-Cookie", sessionCookie(token, 3600))
                    .build();
        } catch (Exception error) {
            return Response.status(500).entity(Map.of(
                    "message", error.getMessage() == null ? "Unknown error" : error.getMessage(),
                    "exception", error.getClass().getName()
            )).build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Origin") String origin) {
        if (!allowedOrigin.equals(origin)) {
            return Response.status(403).entity(Map.of("message", "Origin not allowed")).build();
        }
        return Response.noContent().header("Set-Cookie", sessionCookie("", 0)).build();
    }

    private String sessionCookie(String value, int maxAge) {
        String cookie = SESSION_COOKIE + "=" + value
                + "; Path=/; Max-Age=" + maxAge
                + "; HttpOnly; SameSite=Strict";
        return secureCookie ? cookie + "; Secure" : cookie;
    }
}
