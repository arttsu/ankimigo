### **Requirements: AnkiMigo - Tracer Bullet UI**

**1. Rationale for the Task**

The primary goal of this "Tracer Bullet" task is to de-risk the AnkiMigo MVP by building a minimal, end-to-end version
of the core user workflow. This initial implementation will validate three critical components simultaneously:

1.  **Technical Feasibility:** Prove that the chosen libraries (`cljfx`, `jsonista`, `hato`) work together effectively.
2.  **Core Logic:** Confirm that we can reliably communicate with the AnkiConnect API (fetch decks, add notes) and parse
    a structured JSON response.
3.  **User Experience:** Establish a basic but functional UI layout and interaction model that can be refined and built
    upon for the full MVP.

By focusing on this thin slice of functionality, we ensure the most complex integrations are working before investing
time in the full set of UI features.

**2. Functional Requirements**

**2.1. State Management** The application's UI will be driven by a single state atom. The initial state is defined as
follows, with `nil` values used to explicitly represent states that have not yet been set.

*   `:prompt-inputs {:concept ""}`: Stores user input for prompt generation.
*   `:rendered-prompt ""`: The fully-formed prompt text, ready to be copied.
*   `:llm-response nil`: The raw JSON string pasted by the user. `nil` indicates no response has been provided.
*   `:parsed-cards nil`: A collection of card maps after successful parsing. `nil` indicates no response has been
    parsed. An empty collection `[]` indicates a parse attempt was made but resulted in zero cards or an error.
*   `:available-decks nil`: A collection of deck names fetched from Anki. `nil` indicates decks have not been fetched.
*   `:selected-deck nil`: The deck name currently selected by the user.
*   `:paste-dialog {:shown? false :text ""}`: Manages the visibility and content of the paste pop-up dialog.
*   `:status-message "Ready."`: A string for providing feedback to the user.

**2.2. UI Layout & Components** The main window will be organized into a three-column horizontal layout.

*   **Column 1: Prompt Inputs**
    *   A text label: `"Concept"`
    *   A single-line text input field bound to `:prompt-inputs/:concept`.

*   **Column 2: Rendered Prompt**
    *   A text label: `"Final Prompt"`
    *   A multi-line, read-only text area displaying the `:rendered-prompt`.
    *   A button labeled `"Copy Prompt"`.

*   **Column 3: LLM Response & Anki Integration**
    *   A horizontal container with two buttons:
        *   `"Paste Response..."`
        *   `"Clear"`
    *   A list view that displays the content of `:parsed-cards` (e.g., the "name" of each card). This view is only
        visible when `:parsed-cards` contains one or more cards.
    *   A horizontal container for Anki controls, displayed below the card list:
        *   A button labeled `"Fetch Decks"`.
        *   A dropdown/combo-box for deck selection. It will be disabled until `:available-decks` is populated.
        *   A button labeled `"Push to Anki"`.

**2.3. User Workflows**

**2.3.1. Prompt Generation and Copying**
1.  As the user types in the "Concept" input field, the `:rendered-prompt` text area updates in real-time. A hardcoded
    template string will be used to merge the concept into the full prompt.
2.  The user clicks the `"Copy Prompt"` button.
3.  The application checks if the "Concept" input is empty.
    *   If empty, the `:status-message` is updated with an error (e.g., "Error: Concept cannot be empty.").
    *   If not empty, the content of `:rendered-prompt` is copied to the system clipboard, and the `:status-message`
        confirms the action (e.g., "Prompt copied!").

**2.3.2. Response Pasting and Parsing**
1.  The user clicks the `"Paste Response..."` button.
2.  A modal dialog appears with a text area and an "OK" (or "Parse") button.
3.  The user pastes the LLM's JSON output into the text area.
4.  The user clicks "OK".
5.  The dialog closes, and the application immediately attempts to parse the pasted text.
    *   On successful parsing, the `:parsed-cards` state is populated, and the card list view updates to display the
        results. The `:status-message` indicates success (e.g., "Parsed 4 cards.").
    *   If parsing fails, `:parsed-cards` is set to an empty collection, and the `:status-message` displays a parsing
        error.
6.  The user can click the `"Clear"` button at any time to reset `:llm-response` and `:parsed-cards` to `nil`, clearing
    the card list view.

**2.3.3. Pushing Cards to Anki**
1.  The user clicks the `"Fetch Decks"` button.
2.  The application makes an API call to AnkiConnect.
    *   On success, the `:available-decks` state is populated, the deck selection dropdown is enabled, and the first
        deck is selected by default.
    *   On failure (e.g., Anki is not running), the `:status-message` displays a connection error.
3.  The user selects a deck from the dropdown. This updates the `:selected-deck` state.
4.  The user clicks the `"Push to Anki"` button.
5.  The application validates that a deck is selected and that there are parsed cards to push.
6.  The application iterates through each card in `:parsed-cards` and sends it to AnkiConnect using the selected deck
    name.
7.  The `:status-message` is updated with the result of the operation (e.g., "Pushed 4 cards to 'My Spanish Deck'." or
    "Error: Failed to add cards.").

**3. Technical Requirements**
*   **Language/Platform:** Clojure, running on the JVM.
*   **UI Framework:** `cljfx`.
*   **HTTP Client:** `hato`.
*   **JSON Parsing:** `jsonista`.
*   **External Dependencies:** A running instance of the Anki desktop application with the AnkiConnect add-on installed
    and enabled.

**4. Out of Scope for This Task**
*   Direct LLM API integration.
*   Persistence of any state (inputs, settings) to a configuration file.
*   A lenient or error-correcting JSON parser. A strict parser is sufficient.
*   Editing or selecting/deselecting individual cards in the list view.
*   Management of multiple prompt templates or user profiles.
*   Setup wizards, installers, or any application packaging.
