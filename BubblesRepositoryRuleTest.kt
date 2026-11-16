package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppSettings
import com.example.data.model.Customer
import com.example.data.model.CustomerLocation
import com.example.data.model.Driver
import com.example.data.model.Visit
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BubblesRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun adminBootstrapAndFullVisitLifecycle_succeeds() = runBlocking {
        // 1. Pre-create driver UID so we know the target driver's Auth UID
        val driverEmail = "driver.ahmed.${UUID.randomUUID().toString().take(6)}@test.com"
        val driverUid = signInTestUser(driverEmail, markEmailVerified = true)
        auth.signOut()

        // 2. Sign in as bootstrapped Admin
        val adminUid = signInTestUser(ADMIN_EMAIL, markEmailVerified = true)
        val adminRepo = BubblesRepository(firestore, auth)

        val profile = withTimeout(TIMEOUT_MS) {
            adminRepo.ensureUserProfileAndBootstrap(ADMIN_EMAIL).getOrThrow()
        }
        assertEquals("ADMIN", profile.role)
        assertEquals(adminUid, profile.userId)

        val drvId = "drv_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createDriver(
                Driver(
                    id = drvId,
                    userId = driverUid,
                    email = driverEmail,
                    name = "Ahmed Driver",
                    username = "ahmed_${drvId.take(5)}",
                    phone = "+966551234567",
                    status = "ACTIVE",
                    gpsActive = false
                )
            ).getOrThrow()
        }

        val custId = "cust_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createCustomer(
                Customer(
                    id = custId,
                    name = "Fahad Customer",
                    primaryPhone = "+966559998877"
                )
            ).getOrThrow()
        }

        val locId = "loc_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createLocation(
                CustomerLocation(
                    id = locId,
                    customerId = custId,
                    customerName = "Fahad Customer",
                    branchName = "Al-Rabie Branch",
                    primaryPhone = "+966559998877",
                    district = "Al-Rabie",
                    latitude = 24.7912,
                    longitude = 46.6548
                )
            ).getOrThrow()
        }

        val visitId = "vst_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createVisit(
                Visit(
                    id = visitId,
                    customerId = custId,
                    locationId = locId,
                    customerName = "Fahad Customer",
                    locationName = "Al-Rabie Branch",
                    primaryPhone = "+966559998877",
                    latitude = 24.7912,
                    longitude = 46.6548,
                    assignedDriverId = drvId,
                    assignedDriverUserId = driverUid,
                    assignedDriverName = "Ahmed Driver",
                    operationType = "DELIVERY",
                    priority = "NORMAL",
                    status = "NOT_STARTED",
                    scheduledAtEpochMs = System.currentTimeMillis() + 600_000L
                )
            ).getOrThrow()
        }

        // 3. Switch to Driver and observe assigned visit
        auth.signOut()
        val signedDriverUid = signInTestUser(driverEmail, markEmailVerified = true)
        assertEquals(driverUid, signedDriverUid)
        val driverRepo = BubblesRepository(firestore, auth)
        val visits = withTimeout(FLOW_TIMEOUT_MS) {
            driverRepo.observeVisitsForDriver(driverUid, null).first { list -> list.any { it.id == visitId } }
        }
        assertTrue(visits.any { it.id == visitId })
    }

    @Test
    fun driverExecutionAndCrossUserBounds_enforcedByRules() = runBlocking {
        val driverAEmail = "drivera.${UUID.randomUUID().toString().take(6)}@test.com"
        val driverBEmail = "driverb.${UUID.randomUUID().toString().take(6)}@test.com"
        val driverAUid = signInTestUser(driverAEmail, markEmailVerified = true)
        auth.signOut()
        val driverBUid = signInTestUser(driverBEmail, markEmailVerified = true)
        auth.signOut()

        // Admin sets up Driver A, Customer, Location, Visit
        signInTestUser(ADMIN_EMAIL, markEmailVerified = true)
        val adminRepo = BubblesRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) { adminRepo.ensureUserProfileAndBootstrap(ADMIN_EMAIL).getOrThrow() }

        val drvAId = "drv_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createDriver(
                Driver(
                    id = drvAId,
                    userId = driverAUid,
                    email = driverAEmail,
                    name = "Driver A",
                    username = "drva_${drvAId.take(4)}",
                    phone = "+966551112233",
                    status = "ACTIVE"
                )
            ).getOrThrow()
        }

        val custId = "cust_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createCustomer(Customer(id = custId, name = "Sarah", primaryPhone = "+966501112233")).getOrThrow()
        }
        val locId = "loc_${UUID.randomUUID().toString().take(8)}"
        withTimeout(TIMEOUT_MS) {
            adminRepo.createLocation(
                CustomerLocation(
                    id = locId,
                    customerId = custId,
                    branchName = "Yasmin Villa",
                    primaryPhone = "+966501112233",
                    latitude = 24.8100,
                    longitude = 46.6300
                )
            ).getOrThrow()
        }
        val visitId = "vst_${UUID.randomUUID().toString().take(8)}"
        val visit = Visit(
            id = visitId,
            customerId = custId,
            locationId = locId,
            customerName = "Sarah",
            locationName = "Yasmin Villa",
            primaryPhone = "+966501112233",
            latitude = 24.8100,
            longitude = 46.6300,
            assignedDriverId = drvAId,
            assignedDriverUserId = driverAUid,
            assignedDriverName = "Driver A",
            operationType = "DELIVERY",
            priority = "NORMAL",
            status = "NOT_STARTED",
            scheduledAtEpochMs = System.currentTimeMillis() + 300_000L
        )
        withTimeout(TIMEOUT_MS) { adminRepo.createVisit(visit).getOrThrow() }
        auth.signOut()

        // Driver A reads assigned visit and completes delivery with manual invoice & Mada payment
        signInTestUser(driverAEmail, markEmailVerified = true)
        val driverARepo = BubblesRepository(firestore, auth)
        val aVisits = withTimeout(FLOW_TIMEOUT_MS) {
            driverARepo.observeVisitsForDriver(driverAUid, null).first { list -> list.any { it.id == visitId } }
        }
        val observedVisit = aVisits.first { it.id == visitId }
        assertEquals("Yasmin Villa", observedVisit.locationName)

        withTimeout(TIMEOUT_MS) {
            driverARepo.recordCheckInArrival(observedVisit, 24.8101, 46.6301, AppSettings()).getOrThrow()
            driverARepo.completeVisitExecution(
                visit = observedVisit,
                driverId = drvAId,
                driverName = "Driver A",
                invoiceNumber = "INV-7712",
                amountDue = 250.0,
                amountCollected = 150.0,
                paymentMethod = "MADA",
                pickupNotes = "Delivered 2 bags",
                pickupPhotoUri = null,
                driverLat = 24.8101,
                driverLng = 46.6301,
                settings = AppSettings()
            ).getOrThrow()
        }
        auth.signOut()

        // Driver B cannot read Driver A's visit or customers
        signInTestUser(driverBEmail, markEmailVerified = true)
        val driverBRepo = BubblesRepository(firestore, auth)
        var visitDenied = false
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("visits").document(visitId).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            visitDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Driver B must be denied reading Driver A visit", visitDenied)

        var customersDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                driverBRepo.observeCustomers(driverBUid).first()
            }
        } catch (e: Exception) {
            val fe = e.unwrapFirestoreException()
            customersDenied = fe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Driver B must be denied listing customers", customersDenied)
    }

    @Test
    fun unauthenticatedCaller_observeAllVisits_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = BubblesRepository(firestore, auth)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeAllVisits("unauth").first()
            }
        } catch (e: Exception) {
            val fe = e.unwrapFirestoreException()
            denied = fe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Unauthenticated caller must fail with PERMISSION_DENIED", denied)
    }

    private companion object {
        const val ADMIN_EMAIL = "ivconquer@gmail.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
