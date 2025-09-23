# Async Effects/Futures - Don't Block JavaFX Thread

## Goal
Make HTTP calls (fetch-deck-names and push-cards-to-anki) run asynchronously using futures to prevent UI freezing during slow network/Anki operations.

## References
- [Original feedback document - Section 1](../2025-09-23-2029-gtp-5-tracer-bullet-feedback.md#1-dont-block-the-javafx-thread-use-async-effectsfutures)
- [cljfx HTTP effects example](https://github.com/cljfx/cljfx/blob/master/examples/e18_pure_event_handling.clj)

## Refinement

### Current State Analysis
**Blocking Issues Identified:**
- `src/ankimigo/main.clj:387-395`: `fetch-deck-names` executes synchronously in `::fetch-decks` event handler
- `src/ankimigo/main.clj:396-425`: `push-cards-to-anki` executes synchronously in `::push-to-anki` event handler

These blocking calls freeze the JavaFX UI thread during network operations.

### Files That Need Changes
1. **`src/ankimigo/main.clj`** - Primary target for async conversion
   - `map-event-handler` function (lines 369-450)
   - `::fetch-decks` handler (lines 387-395)
   - `::push-to-anki` handler (lines 396-425)
   - `fetch-deck-names` function (lines 55-74)
   - `push-cards-to-anki` function (lines 118-131)

2. **New files to create** (for enhanced effects system):
   - `src/ankimigo/effects.clj` - HTTP effects handler
   - `src/ankimigo/anki.clj` - AnkiConnect operations (if we extract)

### State Changes Required
- Add `:fetching-decks?` boolean to app state
- Add `:pushing-cards?` boolean to app state
- Enhance error handling structure in state

### Implementation Approaches
**Option A: Quick Future Fix**
- Wrap HTTP calls in `future` blocks within existing handlers
- Minimal code changes, immediate relief

**Option B: Full Effects System**
- Implement cljfx effects pattern from e18 example
- Separate pure events from side effects
- More robust but requires more refactoring

### Q & A

**Q1:** Should we start with Option A (quick future fix) or go directly to Option B (full effects system)?
**A1:** Go directly to Option B (full effects system)

**Q2:** Do we want to extract AnkiConnect operations to a separate namespace as part of this change, or keep it in main.clj for now?
**A2:** Extract to separate namespace

**Q3:** Should we add visual loading indicators (progress bars/spinners) as part of this async implementation, or handle that separately?
**A3:** Start with simple loading indicators

### Loading Indicators Complexity Analysis

**Simple approach (Low complexity):**
- Add `:fetching-decks?` and `:pushing-cards?` to state
- Disable buttons and show "Loading..." text in status bar
- 5-10 lines of UI code changes

**Medium complexity:**
- Add JavaFX ProgressIndicator widgets
- Show/hide based on loading states
- ~20-30 lines of UI code changes

**Higher complexity:**
- Custom progress bars with cancellation
- Real-time progress feedback
- 50+ lines of new UI code

**Recommendation:** Start with simple approach (disable buttons + status text) since it's minimal overhead and gives immediate user feedback.

## Implementation

### Completed Steps

1. **Extracted AnkiConnect operations** to `src/ankimigo/anki.clj`
   - Moved all Anki-related functions to separate namespace
   - Cleaner separation of concerns

2. **Created effects handler** in `src/ankimigo/effects.clj`
   - Simple dispatch-effect function for async operations
   - Foundation for future effects expansion

3. **Added loading states** to app state
   - `:fetching-decks?` and `:pushing-cards?` flags
   - Used for UI feedback

4. **Converted event handlers** to async
   - `::fetch-decks` runs in future, dispatches result events
   - `::push-to-anki` runs in future, dispatches result events
   - Added result/error handlers for both operations

5. **Updated UI with loading indicators**
   - Buttons show "Fetching..."/"Pushing..." during operations
   - Buttons disabled during async operations
   - Status messages provide feedback

### Result
- UI remains fully responsive during network operations
- Clear visual feedback for user actions
- No blocking of JavaFX thread
- Clean separation between UI events and async operations