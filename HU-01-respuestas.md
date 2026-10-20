# Feature Intake -> Spec Checklist

## 1. Summary

- Update `POST /auth/login` so valid tokens include the `sub`, `client`, `iat`, and `exp` claims.
- Validate the `client` from the request, ensure `expiresIn` aligns with `exp - now` within a documented tolerance, and prevent sensitive values from being logged.

## 2. Scope

- **In scope**
- Require the specified claims and obtain `client` from the request, validate it, and include it in the token.
- Check that `expiresIn` is consistent with `exp - now` within a documented tolerance.
- Remove the default secret outside the lab or document it as lab-only.
- Avoid logging tokens, secrets, and sensitive values.
- Test valid login, invalid credentials, and expired tokens.
- Document the HMAC-versus-JWT-library decision.
- **Out of scope**
- Not specified in the HU.

## 3. Assumptions

- None. The HU does not confirm the client-validation rules, expiration tolerance, or whether HMAC or a JWT library will be used.

## 4. Technical decisions to make

- Decide whether to keep HMAC signing or adopt a JWT library.
- Define and document the tolerance for checking `expiresIn` against `exp - now`.
- Determine how the default secret will be handled outside the lab: removed or explicitly documented as lab-only.
- Define how the `client` value from the request will be validated.

## 5. Risks and mitigations

- **Token or secret exposure in logs:** Verify through log review that tokens, secrets, and sensitive values are not recorded.
- **Inconsistent expiration reporting:** Document the tolerance and test that `expiresIn` is consistent with `exp - now` within that tolerance.
- **Unintended use of a default secret outside the lab:** Remove it outside the lab or document that it is lab-only.

## 6. Acceptance criteria

- A valid token contains `sub`, `client`, `iat`, and `exp`.
- The `client` claim reflects a value from the request that passes the agreed validation.
- `expiresIn` matches `exp - now` within the documented tolerance.
- Tests cover valid login, invalid credentials, and expired tokens.
- Logs contain no tokens, secrets, or sensitive values.
- The HMAC-versus-JWT-library decision is documented.

## 7. Validation plan

- **Unit tests:** Verify required claims, client validation, expiration consistency within the documented tolerance, and relevant login error cases.
- **Functional tests:** Exercise `POST /auth/login` for valid login and invalid credentials; verify behavior for expired tokens.
- **Security (minimum checks):** Review relevant logs for token, secret, or sensitive-value exposure; check that default-secret handling matches the documented lab scope.
- **Required evidence:** Tests for claims, expiration, and authentication errors; documented `AUTH_TOKEN_SECRET` configuration; evidence of redacted logs; documented HMAC-versus-JWT-library decision.

## 8. Suggested tasks by role

- **Chapter/PO**
- Confirm whether the default secret is removed outside the lab or documented as lab-only.
- Agree on the client-validation rules.
- Confirm the expected evidence for the acceptance criteria.
- **Tech Lead**
- Decide whether to retain HMAC or adopt a JWT library, and document the decision.
- Define and document the expiration tolerance.
- Review the secret-handling and logging approach against the HU.
- **Backend**
- Implement the required claims and client handling in the login flow.
- Implement the expiration consistency check using the documented tolerance.
- Ensure sensitive values are not logged and add tests for the specified login and token cases.
- **Frontend**
- The HU does not specify frontend changes; confirm whether any frontend work applies.
- **QA**
- Verify valid-token claims and client validation.
- Test `expiresIn` against `exp - now` using the documented tolerance.
- Run and record tests for valid login, invalid credentials, expired tokens, and log redaction.
- **Cyber**
- Review logs for token, secret, or sensitive-value exposure.
- Review the documented handling of the default secret outside the lab.
- Review the documented signing approach decision.

## 9. Open questions

- What are the exact rules for validating `client` from the request?
- What tolerance should be documented for comparing `expiresIn` with `exp - now`?
- Should the default secret be removed outside the lab or documented as lab-only?
- Will signing continue to use HMAC, or will a JWT library be adopted?
- Does this HU require any frontend changes?