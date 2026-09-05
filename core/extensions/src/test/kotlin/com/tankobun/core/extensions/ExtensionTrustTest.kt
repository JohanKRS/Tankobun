package com.tankobun.core.extensions

import org.junit.Assert.*
import org.junit.Test

class ExtensionTrustTest {
    @Test
    fun approvalIsBoundToBothPackageAndEveryCurrentSigner() {
        val a = "a".repeat(64)
        val b = "b".repeat(64)
        val approved = extensionTrustKey("package.one", setOf(a))
        assertNotEquals(approved, extensionTrustKey("package.two", setOf(a)))
        assertNotEquals(approved, extensionTrustKey("package.one", setOf(b)))
        assertNotEquals(approved, extensionTrustKey("package.one", setOf(a, b)))
        assertEquals(extensionTrustKey("package.one", setOf(a, b)), extensionTrustKey("package.one", setOf(b, a)))
    }

    @Test
    fun missingOrMalformedSigningIdentityCannotBeApproved() {
        assertNull(extensionTrustKey("package.one", emptySet()))
        assertNull(extensionTrustKey("package.one", setOf("invalid")))
    }
}
