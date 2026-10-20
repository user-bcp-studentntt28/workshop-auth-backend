# Skills Training Model: scan fixture

This standalone Maven POM intentionally declares outdated dependencies so the
training model can be checked against dependency-scanning tools. It is not a
production module and is not referenced by the backend's production POM.

The fixture contains no application source code and does not exercise the
vulnerable functionality. Its dependency versions are associated with these
known findings:

- `org.yaml:snakeyaml:1.26` — CVE-2022-1471
- `commons-collections:commons-collections:3.2.1` — CVE-2015-6420

## Validate the fixture

From the backend repository root, resolve the declared dependencies:

```powershell
mvn -f .\skills-training-model-fixture\pom.xml dependency:tree
```

Point Fortify, Jira SAST, Xray, or the scanner under test at this fixture's
`pom.xml` (or its directory). Confirm that the scanner reports the vulnerable
components. Detection and severity can vary with the scanner's vulnerability
feed and configuration.

Do not add this fixture as a module or dependency of the production build, and
do not suppress its findings. Remove the fixture when the training exercise is
complete.
