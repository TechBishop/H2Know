# H2Know Firestore Security Specification

## 1. Data Invariants
1. **Authentication Invariant**: No unauthenticated read or write is permitted on `/water_issues/{issueId}` or `/location_history/{historyId}`.
2. **Ownership Isolation Invariant**: Every document in `/water_issues` and `/location_history` must carry a `userId` matching `request.auth.uid`. List and get queries are strictly restricted to `resource.data.userId == request.auth.uid`.
3. **Immutable Identity & Creation Metadata**: `userId`, `pwsid`, and `createdAt` cannot be mutated on `update`.
4. **Terminal State Locking**: Once a `water_issues` document reaches `status == 'resolved'`, no further updates are permitted.
5. **Strict Schema & Bounds**: Every string field enforces minimum and maximum character lengths, enums (`severity`, `status`) enforce exact allowed values, integer metrics enforce numeric ranges, and timestamps require `is timestamp && <= request.time`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: `unauthDb.collection("water_issues").get()` -> Must fail (`PERMISSION_DENIED`).
2. **Cross-User Read**: Alice reading Bob's `/water_issues/bob_issue` -> Must fail (`PERMISSION_DENIED`).
3. **Unfiltered Query Scraping**: Alice calling `aliceDb.collection("water_issues").get()` without `.where("userId", "==", ALICE_UID)` -> Must fail (`PERMISSION_DENIED`).
4. **Identity Spoofing on Create**: Alice creating `/water_issues/issue_1` with `userId: "bob_456"` -> Must fail (`PERMISSION_DENIED`).
5. **Shadow / Ghost Field Injection**: Adding `isAdminVerified: true` on `create` or `update` -> Must fail (`hasOnly` rejection).
6. **Immutable Field Mutation**: Updating `userId`, `pwsid`, or `createdAt` on `/water_issues/{id}` -> Must fail.
7. **Terminal State Bypass**: Updating a `/water_issues/{id}` document whose existing `status` is `'resolved'` -> Must fail.
8. **Invalid Enum Casing / Value**: Creating a water issue with `severity: "CRITICAL"` or `status: "closed"` -> Must fail.
9. **Future Timestamp Spoofing**: Submitting `createdAt` in the future (`> request.time`) -> Must fail.
10. **String Overflow / Denial of Wallet**: Submitting a 2,000-character `description` or empty `title` -> Must fail.
11. **Out-of-Bounds Score**: Creating a `/location_history/{id}` record with `qualityScore: 150` or `-5` -> Must fail.
12. **Cross-User Delete**: Alice attempting to delete Bob's `/location_history/bob_hist` -> Must fail.
