# Feature Intake -> Spec Checklist

## 1. Summary
- Add a pre-send confirmation step for transfers, including a summary message and data validation.
- Expected impact: reduce complaints caused by duplicate transfers by 20% within one month.

## 2. Scope
- **In scope**
  - Show a transfer summary and request explicit confirmation before sending.
  - Validate transfer data before submission.
  - Integrate with the existing accounts and transfers services.
- **Out of scope**
  - Infrastructure changes.
  - A complete frontend redesign.

## 3. Assumptions
- The feature will use the existing REST API, web frontend, and standardized logging.
- The specific summary fields and validation rules have not yet been defined.

## 4. Technical decisions to make (pending or resolved)
- **Pending:** Determine which transfer details appear in the confirmation summary.
- **Pending:** Define validation rules and how validation errors are presented.
- **Pending:** Agree on the baseline and data source for measuring duplicate-transfer complaints.
- **Pending:** Confirm which existing service owns each part of the transfer flow.

## 5. Risks and mitigations
- **Sensitive data exposure in logs:** Review the transfer flow's logs and verify that sensitive values are not recorded.
- **Incorrect amount validation:** Agree on amount-validation rules and cover each agreed case with tests.
- **Integration issues with accounts or transfers services:** Verify the confirmation flow against both existing service integrations before release.

## 6. Acceptance criteria (measurable)
- Every transfer submission requires explicit user confirmation after the summary is displayed.
- Transfer data that fails an agreed validation rule is not submitted and produces an appropriate validation message.
- Every agreed validation case has a passing automated test.
- Duplicate-transfer complaints decrease by 20% within one month of release, measured against the agreed baseline.

## 7. Validation plan
- **Unit tests:** Cover the confirmation gate, summary data, and agreed valid and invalid input cases.
- **Functional tests:** Verify the transfer flow with the accounts and transfers services, including that no transfer is submitted before confirmation.
- **Security (minimum checks):** Inspect relevant logs to verify that sensitive transfer data is not exposed.
- **Required evidence (what to attach):** Unit and functional test results, a completed log-review checklist, and updated brief documentation.

## 8. Suggested tasks by role
- **Chapter/PO**
  - Confirm the 20% target, measurement source, and baseline.
  - Define the information shown in the confirmation summary.
  - Agree on expected validation behavior and user-facing messages.
- **Tech Lead**
  - Confirm the design fits the existing REST API and web frontend.
  - Define integration responsibilities for the accounts and transfers services.
  - Review validation, logging, and acceptance criteria with the team.
- **Backend**
  - Implement the agreed transfer-data validation.
  - Integrate the flow with the existing accounts and transfers services.
  - Verify that sensitive values are excluded from relevant logs.
- **Frontend**
  - Implement the pre-send confirmation step.
  - Display the agreed transfer summary.
  - Present the agreed validation messages and prevent submission until confirmation.
- **QA**
  - Test confirmed and unconfirmed transfer flows.
  - Verify agreed valid and invalid data cases.
  - Attach test results and complete the log-review checklist.
- **Cyber**
  - Review the risk of sensitive data exposure in logs.
  - Verify that the confirmation summary contains only agreed transfer details.
  - Record the results of the minimum security checks.

## 9. Open questions
- Which fields should appear in the confirmation summary?
- What are the exact validation rules for transfer data and amounts, and what error messages should users see?
- What source and time period will establish the baseline for duplicate-transfer complaints?
- Which service owns each part of the confirmation and transfer submission contract?