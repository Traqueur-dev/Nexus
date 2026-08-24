plugins {
    id("nexus.java-conventions")
}

description = "Domain model and public plugin SDK. Zero third-party dependencies."

/*
 * No dependencies block, and that is the contract.
 *
 * This module ships to third-party plugin authors as-is: anything added here is
 * imposed on every plugin. The absence of a dependencies block is what makes the
 * rule enforceable by the compiler rather than by review.
 */
