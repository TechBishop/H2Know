const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTimestamp() {
  return new Date(Date.now() - 1000);
}

function validWaterIssuePayload(userId = ALICE_UID, overrides = {}) {
  const ts = validTimestamp();
  return {
    userId,
    pwsid: "TX2270001",
    systemName: "Austin Water",
    zip: "78701",
    category: "Taste / Odor",
    title: "Chlorine taste on cold tap",
    description: "Noticed stronger chloramine taste this morning after hydrant flushing.",
    severity: "medium",
    status: "open",
    affectedCount: 1,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validLocationHistoryPayload(userId = ALICE_UID, overrides = {}) {
  const ts = validTimestamp();
  return {
    userId,
    pwsid: "TX2270001",
    systemName: "Austin Water",
    zip: "78701",
    city: "Austin",
    state: "TX",
    qualityScore: 86,
    fiveYearDelta: 4,
    tenYearDelta: 9,
    primaryWhySummary: "Surface water from Colorado River with seasonal disinfection byproducts.",
    exceedsHealthGoalCount: 3,
    violationCount: 1,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read or create water_issues or location_history", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("water_issues").get());
  await assertFails(
    unauthDb.collection("water_issues").doc("issue_1").set(validWaterIssuePayload())
  );
  await assertFails(unauthDb.collection("location_history").get());
});

test("2. Authenticated user can create and query their own water_issues with userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("water_issues").doc("issue_alice_1").set(validWaterIssuePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("water_issues").where("userId", "==", ALICE_UID).get()
  );
});

test("3. Authenticated user fails query without userId filter (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("water_issues").get());
  await assertFails(aliceDb.collection("location_history").get());
});

test("4. Cross-user isolation: Alice cannot read, update, or delete Bob's water_issues or location_history", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("water_issues").doc("bob_issue").set(validWaterIssuePayload(BOB_UID))
  );
  await assertSucceeds(
    bobDb.collection("location_history").doc("bob_hist").set(validLocationHistoryPayload(BOB_UID))
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("water_issues").doc("bob_issue").get());
  await assertFails(aliceDb.collection("water_issues").doc("bob_issue").delete());
  await assertFails(aliceDb.collection("location_history").doc("bob_hist").get());
  await assertFails(aliceDb.collection("location_history").doc("bob_hist").delete());
});

test("5. Identity spoofing and ghost field injection are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("water_issues").doc("spoof_1").set(validWaterIssuePayload(BOB_UID))
  );
  await assertFails(
    aliceDb
      .collection("water_issues")
      .doc("ghost_1")
      .set(validWaterIssuePayload(ALICE_UID, { isVerifiedAdmin: true }))
  );
});

test("6. Terminal state locking prevents updating resolved water_issues", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("water_issues")
      .doc("resolved_issue")
      .set(validWaterIssuePayload(ALICE_UID, { status: "resolved" }))
  );
  await assertFails(
    aliceDb.collection("water_issues").doc("resolved_issue").update({
      affectedCount: 5,
      updatedAt: validTimestamp(),
    })
  );
});

test("7. Authenticated user can create, update, and query their own location_history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("location_history")
      .doc("hist_78701")
      .set(validLocationHistoryPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("location_history").doc("hist_78701").update({
      qualityScore: 88,
      updatedAt: validTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("location_history").where("userId", "==", ALICE_UID).get()
  );
});
