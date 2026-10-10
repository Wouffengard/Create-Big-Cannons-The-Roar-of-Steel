/**
 * Content registration index classes.
 *
 * <p>Conventions used from Phase 1 onward:
 * <ul>
 *   <li>Every index class registers into the single shared
 *       {@link com.cbcrs.wouffengard.TheRoarOfSteelRegistrate} instance.</li>
 *   <li>Big-cannon content lives in {@code Cannon*} classes and uses the
 *       {@code cannon_} id prefix.</li>
 *   <li>Autocannon content lives in {@code Autocannon*} classes and uses the
 *       {@code autocannon_} id prefix.</li>
 *   <li>Registration is driven explicitly from
 *       {@link com.cbcrs.wouffengard.index.CBSIndex}; nothing is registered from static
 *       initialisers that could run before the registrate bootstrap.</li>
 * </ul>
 */
package com.cbcrs.wouffengard.index;
