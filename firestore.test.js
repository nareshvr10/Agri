const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const FARMER_UID = "farmer_001";
const OTHER_FARMER_UID = "farmer_002";
const EXPERT_UID = "expert_001";
const ADMIN_UID = "admin_001";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("farms").get());
  await assertFails(unauthDb.collection("users").doc(FARMER_UID).get());
  await assertFails(
    unauthDb.collection("farms").doc("farm_1").set({
      ownerId: FARMER_UID,
      name: "Green Valley",
      locationName: "Thanjavur",
      latitude: 10.787,
      longitude: 79.1378,
      areaAcres: 4.5,
      primaryCrop: "PADDY",
      sharedExpertIds: [],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile creation enforces FARMER role and blocks self-escalation", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID, { email: "farmer@example.com", email_verified: true }).firestore();
  const created = pastDate(10000);

  // Farmer self-registration succeeds
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).set({
      userId: FARMER_UID,
      displayName: "Karthik Raja",
      email: "farmer@example.com",
      role: "FARMER",
      district: "Thanjavur",
      preferredLanguage: "en",
      cropsGrown: ["PADDY", "BANANA"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Self-escalation via update fails
  await assertFails(
    farmerDb.collection("users").doc(FARMER_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // Self-registration directly as ADMIN fails
  const otherDb = testEnv.authenticatedContext(OTHER_FARMER_UID).firestore();
  await assertFails(
    otherDb.collection("users").doc(OTHER_FARMER_UID).set({
      userId: OTHER_FARMER_UID,
      displayName: "Rogue Admin",
      role: "ADMIN",
      preferredLanguage: "en",
      cropsGrown: [],
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Email spoofing test: unverified admin email cannot act as admin", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "vrnking.in@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());
});

test("Farm ownership, expert sharing, and cross-user isolation", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_FARMER_UID).firestore();
  const expertDb = testEnv.authenticatedContext(EXPERT_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    farmerDb.collection("farms").doc("farm_1").set({
      ownerId: FARMER_UID,
      name: "Kaveri Delta Plot",
      locationName: "Thanjavur",
      latitude: 10.787,
      longitude: 79.1378,
      areaAcres: 3.2,
      primaryCrop: "PADDY",
      sharedExpertIds: [EXPERT_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Ghost @DocumentId `id` field must fail
  await assertFails(
    farmerDb.collection("farms").doc("farm_2").set({
      id: "farm_2",
      ownerId: FARMER_UID,
      name: "Extra Id Farm",
      locationName: "Trichy",
      latitude: 10.79,
      longitude: 78.70,
      areaAcres: 2.0,
      primaryCrop: "BANANA",
      sharedExpertIds: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered Kotlin queries succeed for owner and shared expert
  await assertSucceeds(farmerDb.collection("farms").where("ownerId", "==", FARMER_UID).get());
  await assertSucceeds(expertDb.collection("farms").where("sharedExpertIds", "array-contains", EXPERT_UID).get());

  // Unfiltered query & unrelated farmer access fail
  await assertFails(otherDb.collection("farms").get());
  await assertFails(otherDb.collection("farms").doc("farm_1").get());
  await assertFails(otherDb.collection("farms").doc("farm_1").update({ name: "Hijacked", updatedAt: pastDate(1000) }));

  // Shared expert can read farm but cannot edit farm ownership/metadata
  await assertSucceeds(expertDb.collection("farms").doc("farm_1").get());
  await assertFails(expertDb.collection("farms").doc("farm_1").update({ name: "Expert Rename", updatedAt: pastDate(1000) }));
});

test("ESP32 impersonation guard: phone client cannot write ESP32_VERIFIED readings", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    farmerDb.collection("farms").doc("farm_1").set({
      ownerId: FARMER_UID,
      name: "Madurai Plot",
      locationName: "Madurai",
      latitude: 9.9252,
      longitude: 78.1198,
      areaAcres: 2.5,
      primaryCrop: "TOMATO",
      sharedExpertIds: [EXPERT_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Spoofing trusted ESP32 source from phone client MUST fail
  await assertFails(
    farmerDb.collection("sensor_readings").doc("read_spoof").set({
      farmId: "farm_1",
      cropZoneId: "zone_1",
      deviceId: "esp32_01",
      ownerId: FARMER_UID,
      sharedExpertIds: [EXPERT_UID],
      source: "ESP32_VERIFIED",
      soilMoisturePct: 55.0,
      soilTempC: 27.0,
      airTempC: 31.0,
      humidityPct: 68.0,
      lightLux: 45000.0,
      rainfallMm: 0.0,
      recordedEpochMs: Date.now() - 2000,
      createdAt: created,
    })
  );

  // Manual field log succeeds
  await assertSucceeds(
    farmerDb.collection("sensor_readings").doc("read_manual").set({
      farmId: "farm_1",
      cropZoneId: "zone_1",
      deviceId: "manual",
      ownerId: FARMER_UID,
      sharedExpertIds: [EXPERT_UID],
      source: "MANUAL_LOG",
      soilMoisturePct: 55.0,
      soilTempC: 27.0,
      airTempC: 31.0,
      humidityPct: 68.0,
      lightLux: 45000.0,
      rainfallMm: 0.0,
      recordedEpochMs: Date.now() - 2000,
      createdAt: created,
    })
  );
});

test("Expert can verify crop scan on shared farm; Admin can promote farmer to expert", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ active: true });
    await db.collection("users").doc(FARMER_UID).set({
      userId: FARMER_UID,
      displayName: "Farmer One",
      role: "FARMER",
      preferredLanguage: "en",
      cropsGrown: ["PADDY"],
      createdAt: pastDate(20000),
      updatedAt: pastDate(20000),
    });
  });

  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const expertDb = testEnv.authenticatedContext(EXPERT_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    farmerDb.collection("farms").doc("farm_1").set({
      ownerId: FARMER_UID,
      name: "Pollachi Grove",
      locationName: "Pollachi",
      latitude: 10.66,
      longitude: 77.01,
      areaAcres: 5.0,
      primaryCrop: "BANANA",
      sharedExpertIds: [EXPERT_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    farmerDb.collection("crop_scans").doc("scan_1").set({
      farmId: "farm_1",
      cropZoneId: "zone_1",
      ownerId: FARMER_UID,
      sharedExpertIds: [EXPERT_UID],
      cropType: "BANANA",
      imageUri: "content://media/picker/123",
      identifiedCrop: "Banana (Musa)",
      possibleCondition: "Possible Sigatoka Leaf Spot (Preliminary)",
      visibleSymptoms: ["Yellow streaks along leaf margins"],
      confidencePct: 74,
      severity: "MODERATE",
      recommendedNextSteps: ["Inspect lower leaves and consult agricultural officer"],
      uncertaintyNotes: "Preliminary screening only; confirm with field expert.",
      expertConfirmed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Shared expert can confirm scan and add expertNotes
  await assertSucceeds(
    expertDb.collection("crop_scans").doc("scan_1").update({
      expertConfirmed: true,
      expertNotes: "Confirmed early Sigatoka; prune severely affected lower leaves.",
      updatedAt: pastDate(1000),
    })
  );

  // Admin can promote user to EXPERT
  await assertSucceeds(
    adminDb.collection("users").doc(FARMER_UID).update({
      role: "EXPERT",
      updatedAt: pastDate(1000),
    })
  );
});
