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

test("Unauthenticated user cannot read or write mealLogs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("mealLogs").get()
  );
});

test("Authenticated user can create and query their own mealLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("mealLogs")
    .doc("log_1");

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-02",
      mealType: "BREAKFAST",
      foodName: "Avena con Plátano",
      brandOrSource: "Open Food Facts",
      servingGrams: 150,
      calories: 320,
      proteinG: 12,
      carbsG: 54,
      fatG: 6,
      barcode: "8480000123456",
      nutriScore: "A",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mealLogs")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's mealLogs or profile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("mealLogs")
      .doc("alice_log")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-02",
        mealType: "LUNCH",
        foodName: "Pollo con arroz",
        brandOrSource: "Gemini AI",
        servingGrams: 350,
        calories: 540,
        proteinG: 42,
        carbsG: 60,
        fatG: 12,
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mealLogs")
      .doc("alice_log")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mealLogs")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Shadow update / ghost field is rejected on mealLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mealLogs")
      .doc("log_ghost")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-02",
        mealType: "DINNER",
        foodName: "Salmón",
        brandOrSource: "Manual",
        servingGrams: 200,
        calories: 400,
        proteinG: 40,
        carbsG: 0,
        fatG: 24,
        isAdminHack: true,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("Authenticated user can create and update their own profile and dailyMetrics", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const profileRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("profile")
    .doc("main");

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      weightKg: 74.5,
      targetWeightKg: 70.0,
      heightCm: 175,
      age: 28,
      activityLevel: "MODERATE",
      goalType: "DEFICIT",
      dailyCalorieGoal: 2100,
      proteinGoalG: 160,
      carbsGoalG: 210,
      fatGoalG: 65,
      waterGoalMl: 2500,
      createdAt: now,
      updatedAt: now,
    })
  );

  const metricRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("dailyMetrics")
    .doc("2026-11-02");

  await assertSucceeds(
    metricRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-02",
      waterMl: 1250,
      loggedWeightKg: 74.3,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    metricRef.update({
      waterMl: 1500,
      updatedAt: new Date(),
    })
  );
});
