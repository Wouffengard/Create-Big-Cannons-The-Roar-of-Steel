/**
 * Data generation providers.
 *
 * <p>Phase 0 ships this package empty. The {@code data} run configuration is
 * already declared in build.gradle and writes to
 * {@code src/generated/resources}, so later phases only need to add a
 * {@code DataGenerator} entry point plus providers here.
 *
 * <p>Planned providers, in the order they become useful:
 * <ol>
 *   <li>Block states and models for the tungsten steel barrels and breeches
 *       (feasibility report, design decision 5).</li>
 *   <li>Block and item tags, including the caliber tags declared in
 *       {@link com.cbcrs.index.CBSTags}.</li>
 *   <li>Recipes: pressing, melting, casting and boring for the new cannon cast
 *       moulds (design decision 6).</li>
 *   <li>Loot tables, including the boring scrap drop (report item 28).</li>
 *   <li>Lang for en_us. zh_cn is maintained by hand, since the added content is
 *       Chinese-first.</li>
 * </ol>
 */
package com.cbcrs.datagen;
