# ErrorProne Findings Fix Plan — `hivemq-edge/hivemq-edge`

## Context

The ErrorProne static analysis tool reports **1,830+ warnings** across the `hivemq-edge/hivemq-edge` module (report: `hivemq-edge/hivemq-edge/build/reports/errorprone/compileJava.log`). These range from genuine bugs (concurrent modification, boxed primitive equality) to modernization suggestions (pattern matching instanceof, expression switches) to documentation noise (missing Javadoc summaries). This plan organizes the fixes into phases that can each be tested and committed independently.

---

## Findings Summary (by type)

| # | Finding | Count | Risk | Action |
|---|---------|-------|------|--------|
| 1 | MissingSummary | 797 | None | **Suppress** — global disable |
| 2 | MissingOverride | 159 | Low | **Fix** — add `@Override` |
| 3 | FutureReturnValueIgnored | 152 | Med | **Mixed** — suppress Netty handlers at method level, fix/review non-handler code |
| 4 | PatternMatchingInstanceof | 141 | None | **Fix** — modernize to Java 16+ pattern matching |
| 5 | EqualsGetClass | 105 | Low | **Fix** — replace `getClass()` with `instanceof` in equals |
| 6 | StatementSwitchToExpressionSwitch | 51 | None | **Fix** — modernize to expression switches |
| 7 | UnusedVariable | 42 | Low | **Fix** — remove or prefix with `_` |
| 8 | EnumOrdinal | 42 | Med | **Suppress** — `@SuppressWarnings` on ~10 protocol enum classes |
| 9 | EffectivelyPrivate | 42 | None | **Suppress** — global disable (Dagger) |
| 10 | FormatStringShouldUsePlaceholders | 29 | None | **Fix** — use `%s` placeholders |
| 11 | LockNotBeforeTry | 19 | Med | **Fix** — move `lock()` before `try` block |
| 12 | JdkObsolete | 19 | Low | **Fix** — `LinkedList` → `ArrayList`/`ArrayDeque` |
| 13 | EmptyBlockTag | 16 | None | **Suppress** — global disable |
| 14 | OperatorPrecedence | 13 | Med | **Fix** — add explicit parentheses |
| 15 | EmptyCatch | 13 | Low | **Fix** — add comment or log |
| 16 | UnnecessaryParentheses | 12 | None | **Fix** — remove unnecessary parens |
| 17 | ReferenceEquality | 11 | High | **Fix** — use `.equals()` instead of `==` |
| 18 | InvalidBlockTag | 9 | None | **Suppress** — global disable |
| 19 | DefaultCharset | 9 | Low | **Fix** — specify charset explicitly |
| 20 | StringCaseLocaleUsage | 8 | Low | **Fix** — add `Locale.ROOT` |
| 21 | ObjectsHashCodePrimitive | 8 | Low | **Fix** — use primitive-specific hash |
| 22 | ObjectEqualsForPrimitives | 8 | Low | **Fix** — use `==` for primitives |
| 23 | NullablePrimitive | 8 | Low | **Fix** — remove nullness annotation from primitives |
| 24 | NonApiType | 8 | Low | **Fix** — use interface types (`Map` not `HashMap`) |
| 25 | AssignmentExpression | 7 | Low | **Fix** — extract assignments |
| 26 | StringSplitter | 6 | Low | **Fix** — document behavior or use Splitter |
| 27 | InconsistentCapitalization | 6 | None | **Fix** — align parameter/field names |
| 28 | CheckReturnValue | 6 | Med | **Fix** — handle return values |
| 29 | MultipleNullnessAnnotations | 5 | Low | **Fix** — use single annotation |
| 30 | MissingCasesInEnumSwitch | 5 | Med | **Fix** — add default/missing cases |
| 31 | ImmutableEnumChecker | 5 | Low | **Fix** — make enum fields final |
| 32 | StaticAssignmentOfThrowable | 4 | Med | **Suppress** — `@SuppressWarnings` on 4 sites (Netty pattern) |
| 33 | Remaining (≤3 each) | ~45 | Varies | **Fix** individually |

---

## Suppression Strategy

### Global disable in ErrorProne config
**File:** `edge-plugins/src/main/kotlin/com/hivemq/errorprone/ErrorProneConventionPlugin.kt`

Rules that are fundamentally not useful for this project:
- **MissingSummary** (797) — project uses `@author`/`@return` Javadoc style without leading summaries
- **EmptyBlockTag** (16) / **InvalidBlockTag** (9) — Javadoc style noise
- **EffectivelyPrivate** (42) — Dagger DI requires `public` on injected methods/constructors

### `@SuppressWarnings` annotations at code site
Rules that are generally useful but have known false positives in specific places:
- **EnumOrdinal** (42) — suppress at class level on ~10 MQTT protocol enum classes where ordinal matches wire byte value
- **StaticAssignmentOfThrowable** (4) — suppress on 4 pre-allocated Netty exception fields
- **FutureReturnValueIgnored** — suppress at method level on ~35-40 Netty handler methods; review/fix the ~70 non-handler findings

---

## Implementation Phases

### Phase 0: Configure ErrorProne suppressions + `@SuppressWarnings` annotations
**Scope:** Plugin config + ~10 enum classes + 4 exception fields + ~35-40 handler methods

**Step 1 — Global disables** in `ErrorProneConventionPlugin.kt`:
```kotlin
options.errorprone {
    disableWarningsInGeneratedCode.set(true)
    disable("MissingSummary")
    disable("EmptyBlockTag")
    disable("InvalidBlockTag")
    disable("EffectivelyPrivate")
}
```

**Step 2 — `@SuppressWarnings("EnumOrdinal")`** at class level on protocol enum classes:
- `QoS.java`, `Mqtt5PayloadFormatIndicator.java`, and ~8 other MQTT protocol enums

**Step 3 — `@SuppressWarnings("StaticAssignmentOfThrowable")`** on 4 static exception fields:
- `OrderedTopicService.java`, `RetainedMessagesSender.java`, and 2 others

**Step 4 — `@SuppressWarnings("FutureReturnValueIgnored")`** at method level on Netty handler methods (~35-40 methods):
- `*Handler.java` classes doing `ctx.writeAndFlush()` fire-and-forget
- `*InterceptorHandler.java` extension interceptors
- `IncomingPublishService.sendAck()`, `finishUnauthorizedPublish()`, etc.

**Test:** Rebuild to verify suppressed warnings no longer appear.
**Commit after this phase.**

---

### Phase 1: Critical bug fixes
**~20 findings across ~15 files**

| Finding | Fix |
|---------|-----|
| **BoxedPrimitiveEquality** (2) | `==` → `Objects.equals()` for `Boolean` fields in `NorthboundMappingEntity` |
| **ReferenceEquality** (11) | `==` → `.equals()` for object comparisons |
| **ModifyCollectionInEnhancedForLoop** (1) | Use `Iterator.remove()` or collect-then-remove in `IncomingMessageFlowInMemoryLocalPersistence` |
| **WaitNotInLoop** (1) | Wrap `monitor.wait()` in while-loop in `HiveMQEdgeHttpServiceImpl` |
| **UnsynchronizedOverridesSynchronized** (1) | Add `synchronized` to `ApiException.getCause()` |
| **LockNotBeforeTry** (19) | Move `lock.lock()` before `try` block |
| **MissingCasesInEnumSwitch** (5) | Add `default` branches |
| **OperatorPrecedence** (13) | Add parentheses for clarity |
| **SynchronizeOnNonFinalField** (3) | Make synchronized fields `final` |

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 2: FutureReturnValueIgnored — non-handler findings (review & fix)
**~70 findings across ~30 files**

These are in non-Netty code where ignoring a future may be a real bug. Needs case-by-case review:

**Likely bugs to fix:**
- `ProtocolAdapterWrapper` line 339: `stopFuture.thenCompose(v -> actualFuture)` — result not captured
- `PublishPollServiceImpl` — `removeMessageFromQueue()` calls without error handling
- Persistence layer operations (`ClientQueuePersistenceImpl`, `InFileSingleWriter`, etc.)
- Bridge communication (`BridgeMqttClient`, `MessageForwarderImpl`, `RemoteMqttForwarder`)

**For each finding, choose one of:**
1. Fix: add proper error handling / capture the future
2. Suppress at method level with `@SuppressWarnings` if fire-and-forget is intentional

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 3: Correctness & API improvements
**~50 findings across ~40 files**

| Finding | Fix |
|---------|-----|
| **EqualsGetClass** (105 across 101 files) | Replace `getClass() != o.getClass()` with `!(o instanceof Type)` in `equals()` methods |
| **CheckReturnValue** (6) | Assign ignored return values (`var unused = ...`) |
| **NullableOptional** (3) | Replace `@Nullable Optional<>` with `Optional.empty()` |
| **NullOptional** (2) | Replace `null` returns with `Optional.empty()` |
| **NonOverridingEquals** (1) | Rename `equals(Asset)` → `matchesAsset()` in `PulseAssetEntity` |
| **FutureTransformAsync** (1) | Change to `Futures.transform()` in `MessageForwarderImpl` |
| **NullablePrimitive** (8) | Remove `@NotNull`/`@Nullable` from primitive params |
| **ObjectEqualsForPrimitives** (8) | `Objects.equals()` → `==` for primitive comparisons |
| **ObjectsHashCodePrimitive** (8) | `Objects.hashCode(int)` → direct primitive hash |
| **MultipleNullnessAnnotations** (5) | Remove duplicate annotations |
| **ImmutableEnumChecker** (5) | Make enum fields `final` |

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 4: Modernization — `instanceof` pattern matching
**141 findings across 66 files**

Mechanical transformation:
```java
// Before
if (obj instanceof Foo) {
    Foo foo = (Foo) obj;
}
// After
if (obj instanceof Foo foo) {
}

// Before (negated, in equals)
if (!(o instanceof Foo)) return false;
Foo that = (Foo) o;
// After
if (!(o instanceof Foo that)) return false;
```

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 5: Modernization — expression switches & format strings
**~100 findings across ~50 files**

| Finding | Fix |
|---------|-----|
| **StatementSwitchToExpressionSwitch** (51) | Convert to `return switch(x) { case A -> ...; }` |
| **FormatStringShouldUsePlaceholders** (29) | String concat → `%s` placeholders in `Preconditions.checkArgument` |
| **UnnecessaryParentheses** (12) | Remove extra parens |
| **AssignmentExpression** (7) | Extract inline assignments |

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 6: Code cleanup
**~70 findings across ~50 files**

| Finding | Fix |
|---------|-----|
| **UnusedVariable** (42) | Remove unused fields/variables |
| **JdkObsolete** (19) | `LinkedList` → `ArrayList`/`ArrayDeque`, `StringBuffer` → `StringBuilder` |
| **EmptyCatch** (13) | Add comments explaining why catch is empty |
| **DefaultCharset** (9) | Add explicit `StandardCharsets.UTF_8` |
| **StringCaseLocaleUsage** (8) | Add `Locale.ROOT` to `toLowerCase()`/`toUpperCase()` |
| **NonApiType** (8) | `HashMap` → `Map`, etc. in public API signatures |
| **StringSplitter** (6) | Add comment or switch to `Splitter` |
| **InconsistentCapitalization** (6) | Rename parameters to match field names |
| **DoubleBraceInitialization** (2) | Replace anonymous subclass init with `List.of()`/`Map.of()` |

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 7: Add `@Override` annotations
**159 findings across many files**

Mechanical addition of `@Override` to methods that implement or override superclass/interface methods.

**Test:** Run full test suite.
**Commit after this phase.**

---

### Phase 8: Remaining low-count findings
**~15 findings**

| Finding | Fix |
|---------|-----|
| **NarrowCalculation** (3) | Widen before arithmetic |
| **NarrowingCompoundAssignment** (2) | Add explicit cast |
| **JavaLangClash** (3) | Rename classes that clash with `java.lang` |
| **NonCanonicalType** (3) | Use canonical type references |
| **ThreadPriorityCheck** (3) | Remove `setPriority()` or document |
| **ModifiedButNotUsed** (2) | Remove dead stores |
| **JavaUtilDate** (2) | Migrate to `java.time` |
| **InlineFormatString** (2) | Inline format strings |
| **BooleanLiteral** (2) | Simplify `== true`/`== false` |
| **Remaining singletons** (~8) | Fix individually |

**Test:** Run full test suite.
**Commit after this phase.**

---

## Verification

After each phase:
1. Rebuild: `./gradlew :hivemq-edge:compileJava` to confirm ErrorProne warnings are reduced
2. Run tests: `./gradlew :hivemq-edge:test` for smoke tests
3. After final phase: regenerate the ErrorProne report and verify zero (or near-zero) remaining findings
