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

*To be filled during implementation session*
