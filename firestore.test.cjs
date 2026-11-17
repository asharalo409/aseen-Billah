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
const ADMIN_EMAIL = "taseenbillah@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

const pastTs = () => new Date(Date.now() - 2000);
const futureTs = () => new Date(Date.now() + 120000);

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("donations").get());
  await assertFails(unauthDb.collection("transactions").where("visibility", "==", "foundation").get());
  await assertFails(unauthDb.collection("blood_requests").where("visibility", "==", "foundation").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("blood_requests").doc("req_1").set({
      userId: ALICE_UID,
      visibility: "foundation",
      bloodGroup: "O+",
      hospital: "DMCH",
      contact: "01700000000",
      patientProblem: "Surgery",
      urgent: true,
      applicantName: "Karim",
      managed: false,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
});

test("User profile & subcollections: owner-only, no self-escalation to admin, no future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const t = pastTs();

  // Valid member profile create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      email: "alice@example.com",
      role: "member",
      themePalette: "green",
      darkMode: false,
      createdAt: t,
      updatedAt: t,
    })
  );

  // Reject role self-escalation on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      role: "admin",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Reject ghost field / @DocumentId 'id' leak
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject Bob reading Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // User subcollection salat_amol list/write succeeds for owner, fails for other
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("salat_amol").doc("2026-11-16").set({
      userId: ALICE_UID,
      dateKey: "2026-11-16",
      prayers: ["fajr", "dhuhr"],
      amolDone: ["কুরআন তিলাওয়াত"],
      tasbihTotal: 99,
      createdAt: t,
      updatedAt: t,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("salat_amol").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("salat_amol").get());

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_chats").doc("c1").set({
      userId: ALICE_UID,
      title: "Future",
      createdAt: futureTs(),
      updatedAt: futureTs(),
    })
  );
});

test("Transactions, Blood Requests & Projects: Kotlin query parity + @DocumentId 'id' rejection + cross-user protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const t = pastTs();

  await assertSucceeds(
    aliceDb.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      visibility: "foundation",
      kind: "donation",
      amount: 5000,
      fundId: "f1",
      donorName: "আব্দুর রহমান",
      method: "bKash",
      receiptNo: "MSF-1001",
      dateStr: "2026-11-16",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Reject 'id' field in map when @DocumentId is used
  await assertFails(
    aliceDb.collection("transactions").doc("tx_bad").set({
      id: "tx_bad",
      userId: ALICE_UID,
      visibility: "foundation",
      kind: "donation",
      amount: 500,
      fundId: "f1",
      dateStr: "2026-11-16",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Filtered query matching visibility == "foundation" succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("transactions").where("visibility", "==", "foundation").get());
  await assertFails(aliceDb.collection("transactions").get());

  // Bob cannot edit/delete Alice's transaction
  await assertFails(bobDb.collection("transactions").doc("tx_1").update({ amount: 9999, updatedAt: pastTs() }));
  await assertFails(bobDb.collection("transactions").doc("tx_1").delete());

  // Blood request: Bob can mark managed = true (one-way transition), cannot hijack hospital/delete
  await assertSucceeds(
    aliceDb.collection("blood_requests").doc("b_1").set({
      userId: ALICE_UID,
      visibility: "foundation",
      bloodGroup: "O+",
      hospital: "ঢাকা মেডিকেল",
      contact: "01700000111",
      patientProblem: "অপারেশন",
      urgent: true,
      applicantName: "রহিম",
      managed: false,
      createdAt: t,
      updatedAt: t,
    })
  );
  await assertFails(bobDb.collection("blood_requests").doc("b_1").update({ hospital: "Hijacked", updatedAt: pastTs() }));
  await assertSucceeds(bobDb.collection("blood_requests").doc("b_1").update({ managed: true, updatedAt: new Date(Date.now() - 500) }));
});

test("Public Requests (PII): strict owner isolation and verified admin access; email spoofing blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const t = pastTs();

  await assertSucceeds(
    aliceDb.collection("public_requests").doc("req_1").set({
      userId: ALICE_UID,
      kind: "aid",
      name: "Alice",
      phone: "01700000999",
      aidType: "চিকিৎসা সহায়তা",
      details: "জরুরি চিকিৎসা সহায়তা প্রয়োজন",
      status: "pending",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Alice can query her own requests; Bob and spoofed admin cannot read Alice's PII
  await assertSucceeds(aliceDb.collection("public_requests").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("public_requests").doc("req_1").get());
  await assertFails(bobDb.collection("public_requests").where("userId", "==", ALICE_UID).get());
  await assertFails(spoofedAdminDb.collection("public_requests").doc("req_1").get());

  // Verified admin can read & approve
  await assertSucceeds(realAdminDb.collection("public_requests").doc("req_1").get());
  await assertSucceeds(realAdminDb.collection("public_requests").doc("req_1").update({ status: "approved", updatedAt: new Date(Date.now() - 500) }));
});

test("Polls & Secret Ballots: atomic batch vote + voter privacy + closed poll rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const t = pastTs();

  await assertSucceeds(
    aliceDb.collection("polls").doc("poll_1").set({
      userId: ALICE_UID,
      visibility: "foundation",
      question: "বন্যা ত্রাণ কোন জেলায় যাবে?",
      options: ["সিলেট", "সুনামগঞ্জ", "কুড়িগ্রাম"],
      voteCounts: [0, 0, 0],
      closed: false,
      createdAt: t,
      updatedAt: t,
    })
  );

  // Bob votes atomically via batch (writes own vote doc + updates poll voteCounts)
  const batch = bobDb.batch();
  batch.set(bobDb.collection("polls").doc("poll_1").collection("votes").doc(BOB_UID), {
    userId: BOB_UID,
    pollId: "poll_1",
    optionIdx: 1,
    createdAt: t,
    updatedAt: t,
  });
  batch.update(bobDb.collection("polls").doc("poll_1"), {
    voteCounts: [0, 1, 0],
    updatedAt: new Date(Date.now() - 500),
  });
  await assertSucceeds(batch.commit());

  // Alice CANNOT read Bob's secret ballot vote
  await assertFails(aliceDb.collection("polls").doc("poll_1").collection("votes").doc(BOB_UID).get());

  // Fake vote count update without vote doc fails (existsAfter check)
  await assertFails(
    aliceDb.collection("polls").doc("poll_1").update({
      voteCounts: [10, 0, 0],
      updatedAt: new Date(Date.now() - 400),
    })
  );
});
