HEI generic insertion experiment — 2026-11-18

Scope: generic RecipeRegistry flattening and RecipeMap insertion only.

Baseline: installed had-enough-items-557549-9167450.jar (HEI 4.36.1), SHA-256
C9B812AC3396F3628CA314F10DD775E0F0098CDBAEC56BE13B4A4F57B70C4443.
The analysis-copy jar and actual client mods jar have identical hashes.
Checked with javap -c -p: private RecipeMap.addRecipe gets the helper, creates the
category row, allocates HashSet, expands subtypes, ignores nulls, deduplicates UIDs,
appends wrappers, then invokes virtual addRecipeCategory (which calls getUniqueId
again). The replacement preserves those callbacks and their order, including the
second UID call omitted by Nova's previous redirect. No callback result is cached.
The erased overwrite descriptor matches the installed private method exactly.

Capacity experiment: HeiGenericInsertionTest.main with --capacity; Zulu JDK 25,
installed runtime libraries including fastutil 8.5.18, five rounds of 200,000 escaped
map allocations per case, ThreadMXBean.getThreadAllocatedBytes on the current
thread. Both size-zero and size-one maps measured 368.0 bytes/op with the default
constructor versus 128.0 bytes/op with expected size 1 in every round: 240 bytes
saved per eligible temporary map. Timing is a microbenchmark, not a startup claim.
This is consistent with the earlier warm-client capacity experiment; the earlier
startup timings are not newly measured by this test.

Only flatten maps with <=1 type are shrunk. Multi-type maps retain default capacity
because fastutil identity hash-table iteration order depends on capacity. Lists
remain mutable, detached copies, including empty lists. Ingredients' original
input/output maps are unchanged because their capacity can affect type order.
RecipeMap uses its normal expanded-list iterator and lazily allocates a HashSet
only when a second distinct non-null ingredient UID appears. This also avoids
allocation for all-null and single-UID duplicate lists without trusting input size.
Wrapper-list capacities are unchanged: their eventual fan-out was not measured.

Validation: direct javac -proc:none using JDK 25 compiled the two mixins, accessor,
helper and test. Main replay passed 2,014 cases against the private method in the
installed jar, plus callback-exception cases. Includes empty/singleton expansion,
input mutation during expansion, duplicates, null ingredients/UIDs, colliding
strings, stateful UID callbacks, repeated insertion, subclass category callbacks,
wrapper row multiplicities and category maps. Actual flatten method is exercised
for 0..39 equal-but-nonidentical ingredient types, checking identity keys, iteration
order, null elements, mutable lists, and copy independence. Replays use installed
HEI and runtime fastutil directly, without launching Minecraft or a Mixin runtime.
An IDE targeted build timed out without diagnostics. Gradle and a transformed
client startup remain for the caller; no shared mixin JSON edits or new entries
are required (both mixins and AccessorIngredients are already registered).

Re-run via Gradle test filter:
  --tests github.kasuminova.novaeng.common.performance.HeiGenericInsertionTest
For standalone replay, compile IngredientGroups, HeiRecipeInsertion,
AccessorIngredients, MixinRecipeRegistryInsertion, MixinRecipeMapDeduplication and
HeiGenericInsertionTest against HEI, runtime fastutil/Guava/Minecraft, Mixin and
JUnit Jupiter. Invoke HeiGenericInsertionTest.main, optionally with --capacity.
Local compiled classes, resolved classpath and raw measurements are under
build/hei-generic-check/ (disposable build output).
