/**
 * Mixin package.
 *
 * <p>Phase 0 declares the package and the (currently empty) mixin
 * configurations {@code cbc_rs.mixins.json} and
 * {@code cbc_rs.client.mixins.json}, so the Mixin service is wired up and later
 * phases can add mixins without touching the build script.
 *
 * <p>Rules for later phases, from the feasibility report's engineering
 * discipline:
 * <ul>
 *   <li>Injection points into Create Big Cannons internals are listed in
 *       chapter 8 of the report. Do not invent new ones without updating that
 *       list.</li>
 *   <li>Any mixin changing CBC behaviour must be justified by a report item;
 *       unused features must not alter CBC's behaviour.</li>
 *   <li>Client-only mixins belong in the {@code .client.} config, not in the
 *       common one.</li>
 * </ul>
 */
package com.cbcrs.wouffengard.mixin;
