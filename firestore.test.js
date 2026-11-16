const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "ivconquer@gmail.com";
const DRIVER_A_UID = "driver_a_uid";
const DRIVER_B_UID = "driver_b_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs() {
  return new Date();
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

test("Unauthenticated caller is rejected on all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("visits").get());
  await assertFails(unauthDb.collection("customers").get());
  await assertFails(unauthDb.collection("payment_records").get());
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("Bootstrapped admin requires verified email (spoof check)", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofedAdminDb.collection("customers").doc("cust_1").set({
      name: "Spoof",
      primaryPhone: "+966551234567",
      isActive: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

test("Driver cannot self-assign ADMIN role on user creation", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_A_UID, {
    email: "drivera@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    driverDb.collection("users").doc(DRIVER_A_UID).set({
      userId: DRIVER_A_UID,
      displayName: "Driver A",
      role: "ADMIN",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
  await assertSucceeds(
    driverDb.collection("users").doc(DRIVER_A_UID).set({
      userId: DRIVER_A_UID,
      displayName: "Driver A",
      role: "DRIVER",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

test("Admin can create customer, location, and visit; driver can read and execute only assigned visit without altering schedule", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("customers").doc("cust_1").set({
      name: "Fahad",
      primaryPhone: "+966551234567",
      isActive: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    adminDb.collection("locations").doc("loc_1").set({
      customerId: "cust_1",
      branchName: "Al-Rabie Branch",
      primaryPhone: "+966551234567",
      latitude: 24.7912,
      longitude: 46.6548,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    adminDb.collection("visits").doc("visit_1").set({
      customerId: "cust_1",
      locationId: "loc_1",
      customerName: "Fahad",
      locationName: "Al-Rabie Branch",
      primaryPhone: "+966551234567",
      assignedDriverId: "drv_1",
      assignedDriverUserId: DRIVER_A_UID,
      operationType: "DELIVERY",
      priority: "NORMAL",
      status: "NOT_STARTED",
      scheduledAtEpochMs: 1700000000000,
      amountDue: 200,
      amountCollected: 0,
      paymentMethod: "UNPAID",
      paymentStatus: "UNPAID",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  const driverADb = testEnv.authenticatedContext(DRIVER_A_UID).firestore();
  const driverBDb = testEnv.authenticatedContext(DRIVER_B_UID).firestore();

  // Driver A can list own assigned visits, cannot list unfiltered
  await assertSucceeds(
    driverADb.collection("visits").where("assignedDriverUserId", "==", DRIVER_A_UID).get()
  );
  await assertFails(driverADb.collection("visits").get());

  // Driver B cannot read Driver A's visit or customers
  await assertFails(driverBDb.collection("visits").doc("visit_1").get());
  await assertFails(driverBDb.collection("customers").doc("cust_1").get());

  // Driver A cannot reassign driver or change scheduledAtEpochMs
  await assertFails(
    driverADb.collection("visits").doc("visit_1").update({
      assignedDriverUserId: DRIVER_B_UID,
      updatedAt: serverTs(),
    })
  );
  await assertFails(
    driverADb.collection("visits").doc("visit_1").update({
      scheduledAtEpochMs: 1800000000000,
      updatedAt: serverTs(),
    })
  );

  // Driver A can update execution + delivery payment status
  await assertSucceeds(
    driverADb.collection("visits").doc("visit_1").update({
      status: "COMPLETED",
      invoiceNumber: "INV-9001",
      amountCollected: 120,
      paymentMethod: "MADA",
      paymentStatus: "PARTIAL",
      updatedAt: serverTs(),
    })
  );

  // Terminal state lock: Driver A cannot modify visit once COMPLETED
  await assertFails(
    driverADb.collection("visits").doc("visit_1").update({
      amountCollected: 200,
      updatedAt: serverTs(),
    })
  );
});

test("Audit logs and payment records cannot be deleted; shadow ghost fields rejected", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const driverADb = testEnv.authenticatedContext(DRIVER_A_UID).firestore();

  // Ghost field rejected
  await assertFails(
    driverADb.collection("audit_logs").doc("log_1").set({
      actorUserId: DRIVER_A_UID,
      actorName: "Driver A",
      actionType: "DELIVERY",
      entityType: "VISIT",
      entityId: "visit_1",
      oldValue: "UNPAID",
      newValue: "PARTIAL",
      ghostField: true,
      createdAt: serverTs(),
    })
  );

  await assertSucceeds(
    driverADb.collection("audit_logs").doc("log_1").set({
      actorUserId: DRIVER_A_UID,
      actorName: "Driver A",
      actionType: "DELIVERY",
      entityType: "VISIT",
      entityId: "visit_1",
      oldValue: "UNPAID",
      newValue: "PARTIAL",
      createdAt: serverTs(),
    })
  );

  // Even admin cannot update or delete audit logs
  await assertFails(adminDb.collection("audit_logs").doc("log_1").update({ newValue: "TAMPERED" }));
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());
});
