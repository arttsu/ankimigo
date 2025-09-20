(ns ankimigo.main
  (:require [cljfx.api :as fx]
            [ankimigo.prompt :as prompt]
            [clojure.string :as str]
            [jsonista.core :as json]
            [hato.client :as http])
  (:import [javafx.scene.input Clipboard ClipboardContent])
  (:gen-class))

;; State update helpers
(defn update-prompt-input
  "Updates a prompt input field"
  [state field-key new-value]
  (assoc-in state [:prompt-inputs field-key] new-value))

;; Card parsing helpers
(defn valid-card?
  "Validates that a card has all required fields"
  [card]
  (and (map? card)
       (contains? card :name)
       (contains? card :front)
       (contains? card :back)
       (not (str/blank? (:name card)))
       (not (str/blank? (:front card)))
       (not (str/blank? (:back card)))))

(defn parse-json-response
  "Parses JSON response and marks invalid cards"
  [json-text]
  (try
    (let [parsed (json/read-value json-text)
          cards (get parsed "cards" [])]
      {:success true
       :cards (->> cards
                   (map #(let [card (hash-map :name (get % "name")
                                              :front (get % "front")
                                              :back (get % "back"))]
                          (if (valid-card? card)
                            (assoc card :valid true)
                            (assoc card :valid false
                                   :error (cond
                                           (str/blank? (:name card)) "Missing or empty name"
                                           (str/blank? (:front card)) "Missing or empty front"
                                           (str/blank? (:back card)) "Missing or empty back"
                                           :else "Invalid card")))))
                   vec)})
    (catch Exception e
      {:success false
       :error "Failed to parse JSON. Please check the format."})))

;; AnkiConnect helper functions
(def anki-connect-url "http://localhost:8765")

(defn fetch-deck-names
  "Fetches available deck names from AnkiConnect"
  []
  (try
    (let [response (http/post anki-connect-url
                               {:body (json/write-value-as-string
                                       {:action "deckNames"
                                        :version 6})
                                :headers {"Content-Type" "application/json"}
                                :timeout 5000})
          body (json/read-value (:body response))]
      (if (nil? (get body "error"))
        {:success true
         :decks (get body "result" [])}
        {:success false
         :error (str "AnkiConnect error: " (get body "error"))}))
    (catch Exception e
      {:success false
       :error (str "Failed to connect to Anki. Is Anki running with AnkiConnect? "
                   (.getMessage e))})))

(defn cards-to-anki-notes
  "Convert cards to AnkiConnect note format"
  [cards deck-name]
  (mapv (fn [card]
          {:deckName deck-name
           :modelName "Basic"
           :fields {:Front (:front card)
                    :Back (:back card)}
           :tags ["ankimigo"]})
        cards))

(defn send-cards-to-anki
  "Send notes to AnkiConnect API"
  [notes]
  (hato.client/post anki-connect-url
                    {:content-type :json
                     :accept :json
                     :body (json/write-value-as-string
                            {:action "addNotes"
                             :version 6
                             :params {:notes notes}})
                     :timeout 5000}))

(defn process-anki-response
  "Process AnkiConnect response and create result"
  [response valid-cards]
  (let [body (json/read-value (:body response) json/keyword-keys-object-mapper)]
    (if (:error body)
      {:success false
       :error (str "AnkiConnect error: " (:error body))}
      (let [results (:result body)
            successful (remove nil? results)
            failed (filter nil? results)]
        {:success true
         :total (count valid-cards)
         :added (count successful)
         :duplicates (count failed)
         :note-ids successful
         :cards-with-ids (mapv (fn [card note-id]
                                 (assoc card :anki-note-id note-id))
                               valid-cards results)}))))

(defn push-cards-to-anki
  "Push validated cards to Anki via AnkiConnect addNotes API"
  [cards deck-name]
  (try
    (let [valid-cards (filter :valid cards)]
      (if (empty? valid-cards)
        {:success false
         :error "No valid cards to push"}
        (let [notes (cards-to-anki-notes valid-cards deck-name)
              response (send-cards-to-anki notes)]
          (process-anki-response response valid-cards))))
    (catch Exception e
      {:success false
       :error (str "Failed to push cards to Anki: " (.getMessage e))})))

;; Clipboard helper functions
(defn copy-to-clipboard!
  "Copy text to system clipboard using JavaFX"
  [text]
  (try
    (let [clipboard (Clipboard/getSystemClipboard)
          content (ClipboardContent.)]
      (.putString content text)
      (.setContent clipboard content)
      true)
    (catch Exception e
      (println "Error copying to clipboard:" (.getMessage e))
      false)))

(defn init-state []
  {:prompt-inputs {:concept ""}
   :status-message (if (prompt/template-loaded?)
                     "Ready."
                     "ERROR: Failed to load prompt template!")
   :llm-response ""
   :parsed-cards []
   :available-decks []
   :selected-deck nil
   :paste-dialog {:visible false
                  :text ""}})

(def *state
  (atom (init-state)))

(defn input-column [prompt-inputs]
  {:fx/type :v-box
   :spacing 10
   :h-box/hgrow :always
   :min-width 200
   :pref-width 600
   :max-width 400
   :children [{:fx/type :label
               :text "Input"
               :style "-fx-font-weight: bold; -fx-font-size: 14px;"}
              {:fx/type :separator}
              {:fx/type :h-box
               :spacing 10
               :children [{:fx/type :label
                           :text "Concept:"}]}
              {:fx/type :text-field
               :text (get-in prompt-inputs [:concept] "")
               :on-text-changed {:event/type ::concept-changed}}]})

(defn output-column [prompt-inputs llm-response]
  {:fx/type :v-box
   :spacing 10
   :h-box/hgrow :always
   :min-width 250
   :pref-width 1000
   :max-width 1000
   :children [{:fx/type :label
               :text "Output"
               :style "-fx-font-weight: bold; -fx-font-size: 14px;"}
              {:fx/type :separator}
              {:fx/type :label
               :text "Final Prompt:"}
              {:fx/type :text-area
               :text (prompt/render-prompt prompt-inputs)
               :editable false
               :wrap-text true
               :v-box/vgrow :always}
              {:fx/type :button
               :text "Copy Prompt"
               :max-width Double/MAX_VALUE
               :on-action {:event/type ::copy-prompt}}
              (if (empty? llm-response)
                {:fx/type :button
                 :text "Paste Response..."
                 :max-width Double/MAX_VALUE
                 :on-action {:event/type ::paste-response}}
                {:fx/type :button
                 :text "Clear"
                 :max-width Double/MAX_VALUE
                 :on-action {:event/type ::clear}})]})

(defn render-valid-card [idx card]
  (filterv some?
    [{:fx/type :label
      :text (str "Card " (inc idx) ": " (:name card))
      :style "-fx-font-weight: bold;"}
     {:fx/type :label
      :text (str "Front: " (:front card))
      :wrap-text true}
     {:fx/type :label
      :text (str "Back: " (:back card))
      :wrap-text true}
     (when (:anki-note-id card)
       {:fx/type :label
        :text (str "Anki ID: " (:anki-note-id card))
        :style "-fx-text-fill: green; -fx-font-size: 11px;"})]))

(defn render-invalid-card [idx card]
  [{:fx/type :label
    :text (str "Card " (inc idx) " (Invalid)")
    :style "-fx-font-weight: bold; -fx-text-fill: #cc0000;"}
   {:fx/type :label
    :text (str "Error: " (:error card))
    :style "-fx-text-fill: #cc0000;"
    :wrap-text true}
   {:fx/type :label
    :text (str "Name: " (or (:name card) "(missing)"))
    :wrap-text true}
   {:fx/type :label
    :text (str "Front: " (or (:front card) "(missing)"))
    :wrap-text true}
   {:fx/type :label
    :text (str "Back: " (or (:back card) "(missing)"))
    :wrap-text true}])

(defn render-card [idx card]
  {:fx/type :v-box
   :spacing 5
   :style (if (:valid card)
            "-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-background-radius: 5;"
            "-fx-background-color: #ffcccc; -fx-padding: 10; -fx-background-radius: 5;")
   :children (if (:valid card)
               (render-valid-card idx card)
               (render-invalid-card idx card))})

(defn card-list [parsed-cards]
  {:fx/type :v-box
   :v-box/vgrow :always
   :children [(if (seq parsed-cards)
                {:fx/type :v-box
                 :spacing 5
                 :children [{:fx/type :label
                             :text (str "Cards to add (" (count parsed-cards) "):")}
                            {:fx/type :scroll-pane
                             :v-box/vgrow :always
                             :fit-to-width true
                             :content {:fx/type :v-box
                                       :spacing 10
                                       :padding 5
                                       :children (map-indexed render-card parsed-cards)}}]}
                {:fx/type :label
                 :text "No cards parsed yet"
                 :style "-fx-text-fill: gray;"})]})

(defn anki-controls [available-decks selected-deck parsed-cards]
  [{:fx/type :separator}
   {:fx/type :label
    :text "Deck:"}
   {:fx/type :combo-box
    :disable (empty? available-decks)
    :items available-decks
    :value selected-deck
    :prompt-text "Select deck..."
    :max-width Double/MAX_VALUE
    :on-value-changed {:event/type ::deck-selected}}
   {:fx/type :button
    :text "Fetch Decks"
    :max-width Double/MAX_VALUE
    :on-action {:event/type ::fetch-decks}}
   {:fx/type :button
    :text "Push to Anki"
    :max-width Double/MAX_VALUE
    :disable (or (nil? selected-deck)
                 (empty? parsed-cards)
                 (empty? (filter :valid parsed-cards)))
    :on-action {:event/type ::push-to-anki}}])

(defn anki-column [parsed-cards available-decks selected-deck]
  {:fx/type :v-box
   :spacing 10
   :h-box/hgrow :always
   :min-width 400
   :pref-width 550
   :children (concat [{:fx/type :label
                       :text "Anki Integration"
                       :style "-fx-font-weight: bold; -fx-font-size: 14px;"}
                      {:fx/type :separator}
                      (card-list parsed-cards)]
                     (anki-controls available-decks selected-deck parsed-cards))})

(defn paste-response-dialog [paste-dialog]
  {:fx/type :stage
   :showing (:visible paste-dialog)
   :title "Paste LLM Response"
   :modality :application-modal
   :width 600
   :height 400
   :on-close-request {:event/type ::cancel-paste}
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :spacing 10
                  :padding 20
                  :children [{:fx/type :label
                              :text "Paste the JSON response from your LLM (ChatGPT, Claude, etc.):"}
                             {:fx/type :text-area
                              :text (:text paste-dialog "")
                              :wrap-text true
                              :v-box/vgrow :always
                              :on-text-changed {:event/type ::paste-dialog-text-changed}}
                             {:fx/type :h-box
                              :spacing 10
                              :alignment :center-right
                              :children [{:fx/type :button
                                          :text "Cancel"
                                          :on-action {:event/type ::cancel-paste}}
                                         {:fx/type :button
                                          :text "OK"
                                          :default-button true
                                          :disable (str/blank? (:text paste-dialog ""))
                                          :on-action {:event/type ::confirm-paste}}]}]}}})

(defn status-bar [status-message]
  {:fx/type :label
   :text status-message})

(defn root [{:keys [prompt-inputs status-message llm-response parsed-cards paste-dialog available-decks selected-deck]}]
  {:fx/type :stage
   :showing true
   :title "AnkiMigo - Tracer Bullet"
   :width 1200
   :height 700
   :on-close-request (fn [_] (System/exit 0))
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :spacing 10
                  :padding 10
                  :children [;; Three-column main layout
                             {:fx/type :h-box
                              :spacing 20
                              :v-box/vgrow :always
                              :children [(input-column prompt-inputs)
                                         (output-column prompt-inputs llm-response)
                                         (anki-column parsed-cards available-decks selected-deck)]}
                             ;; Status bar
                             {:fx/type :separator}
                             (status-bar status-message)]}}})

(defn map-event-handler [event]
  (case (:event/type event)
    ::concept-changed (swap! *state update-prompt-input :concept (:fx/event event))
    ::copy-prompt (let [concept (get-in @*state [:prompt-inputs :concept] "")]
                    (println "Copy Prompt button clicked!")
                    (if (str/blank? concept)
                      (do (println "Please enter a concept first!")
                          (swap! *state assoc :status-message "Please enter a concept first!"))
                      (let [prompt-text (prompt/render-prompt (:prompt-inputs @*state))]
                        (if (copy-to-clipboard! prompt-text)
                          (do (println "Successfully copied prompt to clipboard!")
                              (swap! *state assoc :status-message "Prompt copied to clipboard!"))
                          (do (println "Failed to copy prompt to clipboard!")
                              (swap! *state assoc :status-message "Failed to copy prompt to clipboard!"))))))
    ::paste-response (do (println "Paste Response button clicked!")
                         (swap! *state assoc-in [:paste-dialog :visible] true))
    ::clear (do (println "Clear button clicked!")
                (swap! *state assoc :llm-response "" :parsed-cards []))
    ::fetch-decks (do (println "Fetch Decks button clicked!")
                      (swap! *state assoc :status-message "Fetching decks from Anki...")
                      (let [result (fetch-deck-names)]
                        (if (:success result)
                          (swap! *state #(-> %
                                             (assoc :available-decks (:decks result)
                                                    :selected-deck (first (:decks result))
                                                    :status-message (str "Found " (count (:decks result)) " decks"))))
                          (swap! *state assoc :status-message (:error result)))))
    ::push-to-anki (do (println "Push to Anki button clicked!")
                       (let [current-state @*state
                             cards (:parsed-cards current-state)
                             deck-name (:selected-deck current-state)
                             valid-cards (filter :valid cards)]
                         (cond
                           (nil? deck-name)
                           (swap! *state assoc :status-message "Please select a deck first")

                           (empty? cards)
                           (swap! *state assoc :status-message "No cards to push")

                           (empty? valid-cards)
                           (swap! *state assoc :status-message "No valid cards to push")

                           :else
                           (do
                             (swap! *state assoc :status-message "Pushing cards to Anki...")
                             (let [result (push-cards-to-anki cards deck-name)]
                               (println "Push result:" result)
                               (if (:success result)
                                 (let [invalid-cards (remove :valid cards)
                                                       updated-cards (concat (:cards-with-ids result) invalid-cards)]
                                                   (swap! *state #(-> %
                                                                     (assoc :status-message
                                                                            (str "Pushed " (:added result) " cards"
                                                                                 (when (> (:duplicates result) 0)
                                                                                   (str ", " (:duplicates result) " duplicates"))))
                                                                     (assoc :parsed-cards updated-cards))))
                                 (swap! *state assoc :status-message (:error result))))))))
    ::deck-selected (swap! *state assoc :selected-deck (:fx/event event))
    ::cancel-paste (do (println "Cancel paste clicked!")
                       (swap! *state #(-> %
                                          (assoc-in [:paste-dialog :visible] false)
                                          (assoc-in [:paste-dialog :text] ""))))
    ::confirm-paste (do (println "Confirm paste clicked!")
                        (let [pasted-text (get-in @*state [:paste-dialog :text] "")
                              parse-result (parse-json-response pasted-text)]
                          (if (:success parse-result)
                            (swap! *state #(-> %
                                               (assoc :llm-response pasted-text
                                                      :parsed-cards (:cards parse-result)
                                                      :status-message (str "Successfully parsed "
                                                                           (count (:cards parse-result))
                                                                           " cards"))
                                               (assoc-in [:paste-dialog :visible] false)
                                               (assoc-in [:paste-dialog :text] "")))
                            (swap! *state #(-> %
                                               (assoc :llm-response ""
                                                      :parsed-cards []
                                                      :status-message (str "JSON parsing error: " (:error parse-result)))
                                               (assoc-in [:paste-dialog :visible] false)
                                               (assoc-in [:paste-dialog :text] ""))))))
    ::paste-dialog-text-changed (swap! *state assoc-in [:paste-dialog :text] (:fx/event event))
    (println "Unhandled event:" (:event/type event))))

(defn -main [& _args]
  (println "Starting AnkiMigo Tracer Bullet")
  ;; Main window renderer
  (fx/mount-renderer *state
                     (fx/create-renderer
                      :middleware (fx/wrap-map-desc assoc :fx/type root)
                      :opts {:fx.opt/map-event-handler map-event-handler}))
  ;; Dialog renderer
  (fx/mount-renderer *state
                     (fx/create-renderer
                      :middleware (fx/wrap-map-desc (fn [state]
                                                      (paste-response-dialog (:paste-dialog state))))
                      :opts {:fx.opt/map-event-handler map-event-handler})))
