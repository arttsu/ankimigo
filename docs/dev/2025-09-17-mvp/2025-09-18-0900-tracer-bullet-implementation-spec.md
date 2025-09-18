### High-Level Plan: AnkiMigo Tracer Bullet

**Stage 1: Core UI Layout & State Initialization**
*   **Description:** Set up the main window with the three-column layout and all specified UI components (labels, text inputs, buttons, read-only text area, list view placeholder, combo box placeholder). Initialize the application's single state atom with the specified initial structure.
*   **Verification:**
    *   Application launches and displays the main window with all elements visible, even if disabled or empty.
    *   The `:status-message` shows "Ready.".
    *   The "Concept" input field is editable.
    *   The "Final Prompt" text area is read-only.
    *   The deck selection dropdown is disabled.
    *   The card list view is not visible.

**Stage 2: Prompt Generation & Copy to Clipboard**
*   **Description:** Implement the real-time update of the `:rendered-prompt` based on the `:prompt-inputs/:concept`. Connect the "Copy Prompt" button to copy the `:rendered-prompt` to the system clipboard, including validation for empty concept.
*   **Verification:**
    *   Typing in the "Concept" input immediately updates the "Final Prompt" text area using the hardcoded template.
    *   Clicking "Copy Prompt" with an empty "Concept" displays an error in `:status-message`.
    *   Clicking "Copy Prompt" with text in "Concept" copies the rendered prompt to the clipboard and updates `:status-message` to "Prompt copied!".
    *   Pasting clipboard content into another application (e.g., a text editor) confirms the correct prompt was copied.

**Stage 3: Response Pasting Dialog & Initial State Update**
*   **Description:** Implement the "Paste Response..." button to show a modal dialog with a text area and an "OK" button. When "OK" is clicked, the dialog closes, the text area content is saved to `:llm-response`, and the dialog's state (`:paste-dialog`) is reset. Implement the "Clear" button to reset `:llm-response` and `:parsed-cards`.
*   **Verification:**
    *   Clicking "Paste Response..." opens a modal dialog.
    *   Typing/pasting text into the dialog's text area works.
    *   Clicking "OK" closes the dialog, and the pasted text is accessible in the application's state (e.g., print to console for verification).
    *   Clicking "Clear" resets the `:llm-response` (verify via state inspection).

**Stage 4: JSON Parsing Logic & Card List Display**
*   **Description:** Integrate `jsonista` to parse the content of `:llm-response` immediately after the paste dialog closes. On successful parsing, populate `:parsed-cards` and display the card names in the list view. Handle parsing errors by setting `:parsed-cards` to `[]` and updating `:status-message`.
*   **Verification:**
    *   Paste valid AnkiConnect JSON output into the dialog and click "OK". The card list view should appear and display the names of the parsed cards, and `:status-message` should confirm success.
    *   Paste invalid JSON and click "OK". The card list view should remain hidden, and `:status-message` should show a parsing error.
    *   Clicking "Clear" hides the card list view.

**Stage 5: AnkiConnect Deck Fetching**
*   **Description:** Implement the `"Fetch Decks"` button. Use `hato` to make an AnkiConnect API call to retrieve available deck names. On success, populate `:available-decks`, enable the deck dropdown, and select the first deck, updating `:selected-deck`. On failure, update `:status-message`.
*   **Verification:**
    *   Ensure Anki (with AnkiConnect) is running.
    *   Click "Fetch Decks". The deck dropdown should become enabled and populated with your Anki decks, and the first one should be pre-selected. `:status-message` should confirm success.
    *   Close Anki and click "Fetch Decks". `:status-message` should display an error.

**Stage 6: AnkiConnect Card Pushing**
*   **Description:** Implement the `"Push to Anki"` button. Validate that a deck is selected and cards are parsed. Iterate through `:parsed-cards`, construct AnkiConnect "addNote" actions, and send them via `hato` to the selected deck. Update `:status-message` with the outcome.
*   **Verification:**
    *   Ensure Anki (with AnkiConnect) is running and decks are fetched.
    *   Parse a valid set of cards (Stage 4).
    *   Select a deck.
    *   Click "Push to Anki".
    *   Check Anki to confirm the cards were added to the selected deck. `:status-message` should confirm success.
    *   Attempt to push without parsed cards or selected deck, and verify appropriate error messages in `:status-message`.
