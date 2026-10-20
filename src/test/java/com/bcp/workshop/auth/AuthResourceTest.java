package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AuthResourceTest {
    private static final String FRONTEND_ORIGIN = "http://localhost:4200";

    @Test
    void logsInWithWorkshopUser() {
        given()
                .header("Origin", FRONTEND_ORIGIN)
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("expiresIn", is(3600))
                .body("token", nullValue())
                .header("Set-Cookie", containsString("HttpOnly"))
                .header("Set-Cookie", containsString("SameSite=Strict"))
                .header("Set-Cookie", containsString("Path=/"))
                .header("Set-Cookie", not(containsString("Domain=")))
                .header("Access-Control-Allow-Origin", is(FRONTEND_ORIGIN))
                .header("Access-Control-Allow-Credentials", is("true"));
    }

    @Test
    void rejectsLoginFromUntrustedOrigin() {
        given()
                .header("Origin", "https://untrusted.example")
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(403);
    }

    @Test
    void expiresSessionCookieOnLogout() {
        given()
                .header("Origin", FRONTEND_ORIGIN)
                .when().post("/auth/logout")
                .then()
                .statusCode(204)
                .header("Set-Cookie", containsString("Max-Age=0"))
                .header("Set-Cookie", containsString("HttpOnly"))
                .header("Set-Cookie", containsString("SameSite=Strict"));
    }
}
