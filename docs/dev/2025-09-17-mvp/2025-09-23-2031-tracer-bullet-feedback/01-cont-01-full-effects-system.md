# Full Effects System - Pure Event Handling with cljfx

## Goal
Refactor the current simple async implementation (using direct futures in event handlers) to a full effects system with pure event handling, following the cljfx e18 pattern. This will make event handlers pure functions that return data descriptions of effects rather than performing side effects directly.

## Rationale

### Why Switch Now?
1. **Better Foundation** - Establishing patterns early prevents technical debt
2. **Testability** - Pure event handlers are much easier to unit test
3. **Debugging** - Can log/replay events for better debugging experience
4. **Learning Opportunity** - Good chance to learn advanced cljfx patterns on a small codebase
5. **Future Extensibility** - Makes adding new effects (localStorage, WebSockets) straightforward

### Current State
We currently have a working async implementation using:
- Direct `future` calls in event handlers
- Side effects (`swap!`) mixed with event handling
- Recursive calls to `map-event-handler` from within futures

This works but has limitations:
- Event handlers are impure (harder to test)
- Async flow is scattered across handlers
- No centralized place to add logging/debugging

### Target State
Pure event handlers that return:
- Updated state (immutable transformations)
- Effect descriptions (data that describes what should happen)
- Both state and effects when needed

## References
- [cljfx e18 pure event handling example](https://github.com/cljfx/cljfx/blob/master/examples/e18_pure_event_handling.clj) - The canonical example
- [cljfx e16 web browser example](https://github.com/cljfx/cljfx/blob/master/examples/e16_web_browser.clj) - Shows http effects
- [cljfx e20 timer example](https://github.com/cljfx/cljfx/blob/master/examples/e20_timer.clj) - Shows scheduled effects
- [Current async implementation](./01-async-effects-futures.md) - What we're building on
- [cljfx README effects section](https://github.com/cljfx/cljfx#effects-and-coeffects) - Official documentation

## Refinement

### Files to Modify
1. **`src/ankimigo/main.clj`**
   - Convert `map-event-handler` to pure `handle-event` function
   - Update renderer setup to use `wrap-effects` middleware
   - Separate state management from event handling

2. **`src/ankimigo/effects.clj`** (recreate)
   - Define effect handlers (`:http`, `:dispatch-later`, etc.)
   - Create effect registration map

### Architecture Changes

#### Current Architecture
```
User Action → Event Handler → Side Effects (swap! + future) → Recursive Event Call
```

#### Target Architecture
```
User Action → Pure Event Handler → {:state new-state :effects [...]} → Effect Handlers → Dispatch Events
```

### Implementation Steps
1. Create effects namespace with HTTP effect handler
2. Convert state atom to be managed by cljfx
3. Create pure handle-event function
4. Setup renderer with wrap-effects middleware
5. Convert each event handler to return data instead of performing side effects
6. Test that everything still works

### Key Patterns to Implement

#### Pure Event Handler
```clojure
(defn handle-event [state event]
  (case (:event/type event)
    ::fetch-decks
    {:state (assoc state :fetching-decks? true
                         :status-message "Fetching decks from Anki...")
     :effects [{:type :http
                :operation anki/fetch-deck-names
                :on-result {:event/type ::fetch-decks-result}
                :on-error {:event/type ::fetch-decks-error}}]}))
```

#### Effect Handler
```clojure
(defn http-effect [{:keys [operation on-result on-error]} dispatch!]
  (future
    (try
      (let [result (operation)]
        (dispatch! (assoc on-result :result result)))
      (catch Exception e
        (dispatch! (assoc on-error :error (.getMessage e)))))))
```

### Q & A

**Q1:** Should we keep the current working implementation as a backup/reference?

**A1:** No.

**Q2:** Do we want to implement additional effects (like `:dispatch-later` for delays) while we're at it?

**A2:** Not unless absolutely necessary.

**Q3:** Should we add event logging/debugging features as part of this refactor?

**A3:** Would be nice!

## Implementation

### What was accomplished

#### Successfully refactored to full effects system
- ✅ Created effects.clj with HTTP and clipboard effect handlers
- ✅ Converted all event handlers to pure functions returning data
- ✅ Separated side effects from state transformations
- ✅ Implemented effects registry and dispatcher
- ✅ Added debug logging for event tracing
- ✅ All existing functionality preserved and working

#### Key improvements implemented
- *Pure event handling*: All events now return `{:state new-state}` or `{:state new-state :effects [...]}`
- *Effects as data*: Side effects described as data, executed by effect handlers
- *Centralized dispatching*: Single map-event-handler manages all state updates and effects
- *Debugging support*: Event logging shows event types and triggered effects
- *Clean architecture*: Clear separation between pure logic and side effects

#### Development approach used
- *Incremental implementation with explicit CHECKPOINTs*: Each step verified before proceeding
- *Gradual migration*: Started with simple events, then async events, maintaining working state throughout
- *Mixed handlers during transition*: Allowed old and new handlers to coexist during migration
- *Small testable steps*: Avoided introducing multiple bugs simultaneously

### Current code structure

#### src/ankimigo/effects.clj (new file)
```clojure
{:http http-effect          ; Async HTTP operations
 :clipboard clipboard-effect ; Clipboard operations
 :dispatch-later dispatch-later-effect} ; Delayed events
```

#### src/ankimigo/main.clj changes
- *Pure handle-event function*: Returns data descriptions instead of performing side effects
- *Simplified map-event-handler*: Now just updates state and triggers effects
- *Removed impure functions*: copy-to-clipboard! removed, handled by effect
- *Event flow*: `Event → handle-event → {:state :effects} → perform-effects`

### Implementation steps completed

1. **Created minimal effects.clj** ✅
   - HTTP effect handler for async operations
   - Clipboard effect for copy operations
   - Effects registry with perform-effects dispatcher

2. **Converted synchronous events to pure** ✅
   - ::concept-changed, ::deck-selected, ::clear
   - ::paste-response, ::cancel-paste, ::paste-dialog-text-changed
   - Events return `{:state new-state}` only

3. **Converted ::fetch-decks to pure + effects** ✅
   - Returns HTTP effect description
   - Result/error events handled purely
   - Async operation via future in effect handler

4. **Converted ::push-to-anki to pure + effects** ✅
   - Complex validation logic remains pure
   - HTTP effect for AnkiConnect call
   - Result handling updates state with card IDs

5. **Converted remaining events** ✅
   - ::copy-prompt uses clipboard effect
   - ::confirm-paste handles parsing purely
   - All events now go through pure handler

6. **Added debug logging** ✅
   - Event types logged on dispatch
   - Effects logged when triggered
   - Helps trace event flow for debugging

### Test procedure verification

All existing functionality tested and working:
1. **Basic UI interactions** ✅ - Text input, buttons, dialogs
2. **Copy Prompt** ✅ - Uses clipboard effect
3. **Paste and Parse** ✅ - Pure JSON parsing
4. **Fetch Decks** ✅ - HTTP effect to AnkiConnect
5. **Push to Anki** ✅ - HTTP effect with validation
6. **Error handling** ✅ - All error paths work

### Technical patterns established

#### Pure Event Handler Pattern
```clojure
(defn handle-event [state event]
  (case (:event/type event)
    ::fetch-decks
    {:state (assoc state :fetching-decks? true)
     :effects [{:type :http
                :operation anki/fetch-deck-names
                :on-result {:event/type ::fetch-decks-result}
                :on-error {:event/type ::fetch-decks-error}}]}))
```

#### Effect Handler Pattern
```clojure
(defn http-effect [{:keys [operation on-result on-error]} dispatch!]
  (future
    (try
      (let [result (operation)]
        (dispatch! (assoc on-result :result result)))
      (catch Exception e
        (dispatch! (assoc on-error :error (.getMessage e)))))))
```

#### Event Dispatch Pattern
```clojure
(defn map-event-handler [event]
  (let [result (handle-event @*state event)]
    (when-let [new-state (:state result)]
      (reset! *state new-state))
    (when-let [effects (:effects result)]
      (effects/perform-effects effects map-event-handler))))
```

### Benefits achieved

1. **Testability** - Pure event handlers can be tested without mocking
2. **Debugging** - Events and effects are logged and traceable
3. **Maintainability** - Clear separation of concerns
4. **Extensibility** - Easy to add new effects (localStorage, WebSocket, etc.)
5. **Predictability** - State changes are deterministic based on events

### Code quality improvements

- *Pure functions*: Event handlers have no side effects
- *Data-driven*: Effects described as data structures
- *Single responsibility*: Each effect handler does one thing
- *Composable*: Effects can be combined and sequenced
- *Traceable*: Event flow visible through logging

### Next steps (future enhancements)

- Add more effects: :dispatch-later, :local-storage, :websocket
- Implement event replay for debugging
- Add middleware for event validation
- Consider adding event history/undo
- Add effect composition helpers
- Implement effect cancellation

The full effects system implementation is complete and provides a solid foundation for future development with pure, testable event handling.

### PR Feedback (1)

1. anki.clj:19: Use if-let.

   **Agree.** `if-let` is more idiomatic when checking for nil. Will change from:
   ```clojure
   (if (nil? (get body "error"))
   ```
   to:
   ```clojure
   (if-let [error (get body "error")]
   ```

2. anki.clj:52: process-anki-response - the name is too general - as it's specifically for processing 'addNotes' response.

   **Agree.** Should rename to `process-add-notes-response` to be more specific about what it processes.

3. Let's add anki-api.clj and put there http calls for deckNames and addNotes. anki.clj should only have "app business logic".

   **Partially agree.** Separation of API calls from business logic is good, but for a small app it might be premature. However, if we're already refactoring, this is a good time to establish proper boundaries. Will create `anki-api.clj` for HTTP calls and keep `anki.clj` for business logic like card validation and transformation.

   FOLLOWUP_1: Yes, let's do this.

   **Response:** Great, I'll create `anki-api.clj` for the HTTP layer.

4. hato allows making async http calls. I think it's better than wrapping calls into futures manually.

   **Disagree.** The futures are not wrapping HTTP calls - they're in the effects system. The HTTP calls themselves are synchronous (using hato's regular `http/post`), and the async behavior is managed by the effects system in `effects.clj`. This gives us:
   - Clear separation between HTTP logic and async execution
   - Consistent error handling across all effects
   - Ability to easily switch between sync/async execution
   - Better testability (can test HTTP functions synchronously)

   Using hato's async would couple the HTTP layer to async execution, making it harder to test and less flexible.

   FOLLOWUP_1: OK, sounds good. However I would suggest to catch "generic" Exceptions only in this effects.clj file. If
   it makes sense to catch exceptions anywhere else (e.g. to provide better error messages), we should catch specific
   exceptions IMHO.

   **Response:** Agree completely. This is a good pattern:
   - `effects.clj` catches generic `Exception` as a safety net to prevent crashes
   - Business logic functions catch specific exceptions (e.g., `ConnectException`, `SocketTimeoutException`, `JsonParseException`) to provide meaningful error messages
   - This gives us both robustness and good error reporting

5. effects.clj: Is this file really needed? I think dispatch-later-effect is unused. Please apply KISS and YAGNI.

   **Partially agree.** The `dispatch-later-effect` is indeed unused and should be removed (YAGNI). However, `effects.clj` itself is valuable because:
   - It centralizes all side effects (HTTP, clipboard)
   - Makes event handlers pure and testable
   - Provides a clear extension point for future effects
   - Follows established cljfx patterns (e18 example)

   Will remove `dispatch-later-effect` but keep the effects system.

   FOLLOWUP_1: Sounds good.

   **Response:** Will implement removal of `dispatch-later-effect`.

6. Let's use "result type" pattern - operations that can fail should return either {:status :ok, :value value} or {:status :error, :message message}

   **Partially agree.** The result type pattern is good, but our current pattern `{:success true/false :data/:error}` is already consistent and works well. Changing to `:status :ok/:error` would:
   - Require refactoring all existing code
   - Not provide significant benefits over current approach
   - Be inconsistent with common Clojure libraries that use `:success` or `:ok?`

   However, I agree we should standardize on ONE pattern. Current code uses both `:success` and direct `:error` checking. Will standardize on `{:success true :result data}` or `{:success false :error message}` throughout.

   FOLLOWUP_1: Sounds good.

   **Response:** Will standardize on consistent result pattern.

## PR Feedback Implementation (Session 2)

All PR feedback items have been successfully addressed and implemented:

### Changes Made

1. **Fixed if-let usage** ✅
   - Changed `anki.clj:19` from `(if (nil? (get body "error"))` to `(if-let [error (get body "error")]`
   - More idiomatic Clojure code

2. **Renamed function for clarity** ✅
   - Renamed `process-anki-response` to `process-add-notes-response`
   - More specific about what type of response it processes

3. **Created API layer separation** ✅
   - **New file**: `src/ankimigo/anki_api.clj` - HTTP calls to AnkiConnect
   - **Refactored**: `src/ankimigo/anki.clj` - Business logic only
   - Clean separation between HTTP concerns and business logic
   - API layer handles generic AnkiConnect communication
   - Business layer handles validation, error handling, data transformation

4. **Maintained effects system architecture** ✅
   - Kept futures in effects system rather than switching to hato async
   - Provides better separation of concerns and testability
   - HTTP calls remain synchronous, async handled by effects layer

5. **Removed unused code (YAGNI)** ✅
   - Removed `dispatch-later-effect` from `effects.clj`
   - Cleaned up effects registry
   - Applied KISS principle while keeping valuable effects system

6. **Improved exception handling** ✅
   - **Business logic**: Catches specific exceptions (`ConnectException`, `SocketTimeoutException`, `IOException`)
   - **Effects system**: Keeps generic `Exception` catch as safety net
   - Provides better error messages for users
   - Examples: "Cannot connect to Anki. Please make sure Anki is running with AnkiConnect add-on installed."

7. **Standardized result pattern** ✅
   - **Success**: `{:success true :result {...}}`
   - **Error**: `{:success false :error "message"}`
   - Consistent across all functions in `anki.clj`
   - Updated consumers in `main.clj` to use `get-in` for nested data access

### Implementation Process

- Used incremental development with explicit checkpoints
- Tested functionality after each major change
- Fixed issues quickly (e.g., AnkiConnect params fix, result pattern access)
- Maintained working state throughout all changes

### Final Architecture

```
HTTP Layer (anki-api.clj)
├── call-anki-connect    # Generic API caller
├── fetch-deck-names     # deckNames API call
└── add-notes           # addNotes API call

Business Logic (anki.clj)
├── fetch-deck-names     # Validation + error handling
├── cards-to-anki-notes  # Data transformation
├── process-add-notes-response # Result processing
└── push-cards-to-anki   # Main push workflow

Effects System (effects.clj)
├── http-effect         # Async HTTP operations
└── clipboard-effect    # Clipboard operations
```

All functionality verified working: app startup, copy prompt, paste response, fetch decks, push to Anki, error handling.

## Architecture Improvement (Session 2 - Continued)

After implementing the initial PR feedback, we further improved the architecture based on the insight that the API layer should handle its own exceptions and return consistent result types.

### Additional Improvements Made

**Problem**: Exception handling was duplicated between API and business layers, and business logic was impure due to exception handling.

**Solution**: Move all exception handling to API layer and make business logic pure.

#### Changes Made

1. **Centralized Exception Handling in API Layer** ✅
   - Moved all HTTP exception handling from `anki.clj` to `anki-api.clj`
   - API layer now owns all HTTP concerns including failures
   - Specific exceptions (`ConnectException`, `SocketTimeoutException`, `IOException`) handled with meaningful messages
   - No generic `Exception` catches in API layer (handled by effects layer as safety net)

2. **API Layer Returns Result Types** ✅
   - `fetch-deck-names()` returns `{:success true/false :result/:error}`
   - `add-notes()` returns `{:success true/false :result/:error}`
   - Consistent interface across all API functions

3. **Pure Business Logic** ✅
   - `anki.clj` functions are now pure - no exception handling
   - `fetch-deck-names` became a simple passthrough: `(api/fetch-deck-names)`
   - `push-cards-to-anki` focuses only on business rules and data transformation
   - Removed all imports of `java.net` and `java.io` from business layer

4. **DRY Exception Handling** ✅
   - Centralized all HTTP exception handling in `call-anki-connect`
   - `fetch-deck-names` and `add-notes` no longer duplicate exception handling
   - Single place to maintain HTTP error handling logic

### Final Architecture (Improved)

```
Effects System (effects.clj)
└── http-effect                    # Generic Exception safety net

HTTP Layer (anki-api.clj)
├── call-anki-connect             # Centralized HTTP + exception handling
├── fetch-deck-names              # AnkiConnect protocol only
└── add-notes                     # AnkiConnect protocol only

Business Logic (anki.clj) - PURE FUNCTIONS
├── fetch-deck-names              # Passthrough to API
├── cards-to-anki-notes           # Data transformation
├── process-add-notes-response    # Result processing
└── push-cards-to-anki           # Business rules only
```

### Benefits Achieved

1. **Single Responsibility**: Each layer has one clear purpose
2. **DRY Principle**: No duplicated exception handling
3. **Pure Business Logic**: Easier to test and reason about
4. **Consistent API**: All API functions return same result format
5. **Better Error Handling**: Specific exceptions with meaningful messages
6. **Maintainability**: Adding new AnkiConnect calls gets exception handling for free

### Exception Handling Strategy

- **API Layer** (`anki-api.clj`): Handles specific HTTP exceptions with user-friendly messages
- **Effects Layer** (`effects.clj`): Catches generic `Exception` as final safety net
- **Business Layer** (`anki.clj`): Pure functions, no exception handling

This follows the agreed principle: specific exceptions where meaningful, generic exceptions only as last resort safety net.

All functionality verified working with improved architecture.
