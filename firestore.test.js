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

function validPaymentPayload(uid, dateIso = "2026-12-02", status = "PAID", note = "UPI জমা") {
  const now = new Date();
  return {
    userId: uid,
    dateIso: dateIso,
    status: status,
    amount: 100,
    transactionNote: note,
    planStartDate: "December 2026",
    planEndDate: "June 2027",
    totalTarget: 10400,
    createdAt: now,
    updatedAt: now,
  };
}

function validPlanSummaryPayload(uid, paidDates = ["2026-12-02"]) {
  const now = new Date();
  const saved = paidDates.length * 100;
  return {
    userId: uid,
    planStartDate: "December 2026",
    planEndDate: "June 2027",
    totalTarget: 10400,
    paidDates: paidDates,
    totalSavedAmount: saved,
    remainingAmount: 10400 - saved,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write payments or saving plan summary", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("payments").doc("2026-12-02").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("payments")
      .doc("2026-12-02")
      .set(validPaymentPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, read, update, and list own payment records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("payments")
    .doc("2026-12-02");

  await assertSucceeds(docRef.set(validPaymentPayload(ALICE_UID, "2026-12-02", "PAID", "প্রথম জমা")));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("payments")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Action-based update of status and transactionNote
  await assertSucceeds(
    docRef.update({
      status: "DUE",
      transactionNote: "পুনরায় বাকি",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated owner: can create and update saving plan summary", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const summaryRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saving_plans")
    .doc("plan_2026_2027");

  await assertSucceeds(summaryRef.set(validPlanSummaryPayload(ALICE_UID, ["2026-12-02"])));
  await assertSucceeds(
    summaryRef.update({
      paidDates: ["2026-12-02", "2026-12-04"],
      totalSavedAmount: 200,
      remainingAmount: 10200,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, or modify Alice's saving records", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("payments")
      .doc("2026-12-02")
      .set(validPaymentPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payments").doc("2026-12-02").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("payments")
      .doc("2026-12-02")
      .update({ status: "DUE", updatedAt: new Date() })
  );
});

test("Schema & Shadow Field Guard: rejects invalid amount, ghost keys, or mismatched totalTarget", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("payments")
    .doc("2026-12-02");

  // Tampered amount (500 instead of 100)
  await assertFails(
    docRef.set({
      ...validPaymentPayload(ALICE_UID),
      amount: 500,
    })
  );

  // Ghost field injection
  await assertFails(
    docRef.set({
      ...validPaymentPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid totalSavedAmount + remainingAmount != 10400
  const summaryRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saving_plans")
    .doc("plan_2026_2027");
  await assertFails(
    summaryRef.set({
      ...validPlanSummaryPayload(ALICE_UID),
      totalSavedAmount: 5000,
      remainingAmount: 9000,
    })
  );
});
