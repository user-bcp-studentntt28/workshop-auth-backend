---
name: security-review
version: 1.0.0-SNAPSHOT
description: "Review implementation and dependencies for CVEs, SAST findings, and security weaknesses. Use when validating code or dependencies with Fortify-, JFrog Xray-, or SAST-compatible evidence."
---

# Security Review Skill - Training Model

## Purpose

Perform evidence-based security validation of the proposed or implemented change and its dependencies. Identify confirmed vulnerabilities, distinguish them from hypotheses, and recommend or make only safe, narrowly scoped corrections.

This file is a training model. It does not claim that Fortify, JFrog Xray, or any other scanner ran unless the relevant tool was available and its output was inspected.

## Inputs

- Issue/spec and approved scope, when available.
- Repository root and changed files or commit range.
- Applicable build, test, dependency, SAST, and security policies.
- Whether the request authorizes implementing remediations or review-only output.

If the scope, target revision, or permission to modify code is unclear, ask before making changes. Never broaden the approved scope to remediate unrelated findings.

## Workflow

1. **Establish scope and baseline**
   - Read the issue/spec, applicable repository instructions, and relevant project documentation.
   - Inspect the working tree and identify the exact changed files and dependency manifests.
   - Preserve pre-existing changes. Do not stage, commit, or discard user changes.
   - Identify applicable protected contracts and behavior that must remain unchanged.

2. **Inventory dependency inputs**
   - Find manifests, lockfiles, dependency catalogs, and build files for the technologies present (for example `package.json`/lockfiles, Python requirements/lockfiles, Maven/Gradle files, NuGet manifests, Go modules, Cargo files, and container manifests).
   - Check whether manifests and lockfiles agree and whether direct and transitive dependencies are covered.
   - Do not infer a dependency version, vulnerability, or remediation from a manifest alone.

3. **Run available dependency vulnerability checks**
   - Prefer repository-configured and ecosystem-appropriate scanners. Examples include `npm audit`, `pip-audit`, configured Maven/Gradle vulnerability checks, and JFrog Xray scans.
   - Run only commands supported by the repository and installed/configured tooling. Check tool availability and version first.
   - Do not install a scanner or change project dependencies merely to make the review appear complete. If a required tool, database, license, configuration, or network access is missing, record the exact limitation as a blocked/pending gate.
   - For each reported CVE/advisory, verify the affected package, installed/resolved version, dependency path, affected range, fixed version if published, and source/output. Do not label a package exploitable without evidence for the relevant usage and exposure.
   - Treat Xray policy violations and CVEs as separate evidence types. Record the Xray policy/rule and affected component when available; do not invent a CVE for a policy finding.

4. **Review implementation for SAST/security defects**
   - Review changed code and the directly relevant call paths for input validation, injection, authentication/authorization, insecure cryptography, secret handling, sensitive logging, unsafe file handling, error handling, and unsafe configuration.
   - Use configured Fortify/SAST tools when available. Record the scanner, version, target revision, command/job, result, and relevant finding identifiers.
   - Corroborate scanner findings against source and data flow. Separate confirmed findings, false positives, and unverified hypotheses.
   - Never print discovered credentials, tokens, personal data, or secret values. Redact them in evidence and stop exposing the value.
   - Do not exploit systems, use destructive payloads, or test external targets.

5. **Remediate only when authorized and safe**
   - If implementation is authorized, make the smallest change that addresses a confirmed, in-scope finding.
   - Prefer a vendor-supported fixed dependency version compatible with the project. Update the appropriate manifest and lockfile together, and avoid broad or major upgrades unless explicitly approved.
   - Do not suppress, downgrade, or disable a scanner, test, policy, or CI gate to obtain a passing result.
   - If the fix is ambiguous, breaking, requires a design decision, or affects out-of-scope code, report the options and stop for human approval rather than guessing.

6. **Validate corrections and regressions**
   - Run focused tests for each correction, relevant project tests/build, and applicable dependency/SAST scanners again.
   - Confirm that the intended finding is resolved and that no new findings or contract regressions were introduced.
   - State exactly which commands ran and their real outcomes. A local pass is not a CI pass; pending, unavailable, or failed checks must remain explicit.

## Output

Return a concise review with:

1. **Scope and revision** - repository, target revision/diff, and files examined.
2. **Validation performed** - tool and version, exact command or CI job, and outcome for dependency, SAST, tests, and build checks.
3. **Findings** - one entry per finding with:
   - Status: confirmed, hypothesis, false positive, or blocked.
   - Severity and confidence, with the severity source (scanner/policy or reviewer assessment).
   - File and line, or dependency name/version and dependency path.
   - CVE/advisory or Xray/Fortify rule identifier when one is actually reported.
   - Evidence, impact, and the affected scope.
   - Recommended remediation and fixed version when verified.
4. **Changes made** - only if authorized; list files, tests, and rerun scanner evidence.
5. **Residual risks and gates** - unavailable scanners, failed/pending CI, untriaged findings, and decisions requiring human approval.

If no findings are returned, say which checks were actually run and explicitly list checks that could not be performed. Absence of scanner output is not evidence that the code or dependencies are vulnerability-free.

## Safety and integrity rules

- Never invent CVEs, versions, scanner results, coverage, or CI outcomes.
- Never claim “Fortify-clean”, “Xray-clean”, or “SAST-clean” without successful, attributable output for the reviewed revision.
- Keep sensitive evidence redacted.
- Do not use real customer data, production secrets, or external systems for testing.
- Preserve security gates and distinguish tool limitations from passing results.
- Do not automatically fix unrelated or unapproved findings.

## Completion criteria

A review is complete only when the scope and revision are stated, relevant dependency inputs are identified, available checks have recorded outcomes, findings are evidence-linked, authorized fixes are revalidated, and all unavailable or unresolved gates are disclosed.

## Example Markdown output: dependency observation

### Apache Commons Text 1.9 — CVE-2022-42889

- **Status:** The declared version is in the affected range reported by NVD (1.5 through 1.9). No Fortify, Xray, or other scanner output was reviewed here, and exploitability through this application's current call path is **not confirmed**.
- **Severity:** Not assigned by a scanner in this review.
- **Dependency and evidence:** The backend directly declares `org.apache.commons:commons-text:1.9` in `pom.xml`. `AuthResource.java` imports `org.apache.commons.text.StringEscapeUtils` and calls `StringEscapeUtils.escapeHtml4(user.displayName())` when constructing the login response.
- **Usage assessment:** The dependency is used directly; it should not be removed without first replacing this HTML-escaping behavior. The reviewed call site uses `StringEscapeUtils`, not the vulnerable `StringSubstitutor` interpolation behavior described by the advisory. The vulnerable behavior was therefore not demonstrated in this call path, but the declared artifact version remains affected and should be addressed.
- **Recommended remediation:** Upgrade Commons Text to a vendor-fixed version (1.10.0 or later, subject to project compatibility and current vendor guidance), then rebuild and rerun the dependency scanner. Do not exclude classes from this dependency: class exclusions are not a reliable Maven remediation and could remove the class the application currently uses. Removing the dependency is an option only if its use is replaced with a maintained, appropriate HTML-escaping implementation and covered by tests.
- **References:** [NVD CVE-2022-42889](https://nvd.nist.gov/vuln/detail/CVE-2022-42889); `pom.xml` (dependency declaration); `src/main/java/com/bcp/workshop/auth/AuthResource.java` (import and use).
- **Validation limitation:** This entry records dependency-version and source-use evidence only. No scanner or test was run as part of this example.
