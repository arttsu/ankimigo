(ns ankimigo.main
  (:require [cljfx.api :as fx]
            [ankimigo.prompt :as prompt]
            [ankimigo.anki :as anki]
            [ankimigo.effects :as effects]
            [clojure.string :as str]
            [jsonista.core :as json])
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
    (catch Exception _e
      {:success false
       :error "Failed to parse JSON. Please check the format."})))


(defn init-state []
  {:prompt-inputs {:concept ""}
   :status-message (if (prompt/template-loaded?)
                     "Ready."
                     "ERROR: Failed to load prompt template!")
   :llm-response ""
   :parsed-cards []
   :available-decks []
   :selected-deck nil
   :fetching-decks? false
   :pushing-cards? false
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

(defn anki-controls [available-decks selected-deck parsed-cards fetching-decks? pushing-cards?]
  [{:fx/type :separator}
   {:fx/type :label
    :text "Deck:"}
   {:fx/type :combo-box
    :disable (or (empty? available-decks) fetching-decks?)
    :items available-decks
    :value selected-deck
    :prompt-text "Select deck..."
    :max-width Double/MAX_VALUE
    :on-value-changed {:event/type ::deck-selected}}
   {:fx/type :button
    :text (if fetching-decks? "Fetching..." "Fetch Decks")
    :max-width Double/MAX_VALUE
    :disable fetching-decks?
    :on-action {:event/type ::fetch-decks}}
   {:fx/type :button
    :text (if pushing-cards? "Pushing..." "Push to Anki")
    :max-width Double/MAX_VALUE
    :disable (or (nil? selected-deck)
                 (empty? parsed-cards)
                 (empty? (filter :valid parsed-cards))
                 fetching-decks?
                 pushing-cards?)
    :on-action {:event/type ::push-to-anki}}])

(defn anki-column [parsed-cards available-decks selected-deck fetching-decks? pushing-cards?]
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
                     (anki-controls available-decks selected-deck parsed-cards fetching-decks? pushing-cards?))})

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

;; Pure event handler - returns {:state new-state} or {:state new-state :effects [...]}
(defn handle-event [state event]
  (case (:event/type event)
    ;; Simple synchronous events first
    ::concept-changed
    {:state (update-prompt-input state :concept (:fx/event event))}

    ::deck-selected
    {:state (assoc state :selected-deck (:fx/event event))}

    ::clear
    {:state (assoc state :llm-response "" :parsed-cards [])}

    ::paste-response
    {:state (assoc-in state [:paste-dialog :visible] true)}

    ::cancel-paste
    {:state (-> state
                (assoc-in [:paste-dialog :visible] false)
                (assoc-in [:paste-dialog :text] ""))}

    ::paste-dialog-text-changed
    {:state (assoc-in state [:paste-dialog :text] (:fx/event event))}

    ;; Async events with effects
    ::fetch-decks
    {:state (assoc state
                   :status-message "Fetching decks from Anki..."
                   :fetching-decks? true)
     :effects [{:type :http
                :operation anki/fetch-deck-names
                :on-result {:event/type ::fetch-decks-result}
                :on-error {:event/type ::fetch-decks-error}}]}

    ::fetch-decks-result
    (let [result (:result event)]
      (if (:success result)
        {:state (-> state
                    (assoc :available-decks (:decks result)
                           :selected-deck (first (:decks result))
                           :status-message (str "Found " (count (:decks result)) " decks")
                           :fetching-decks? false))}
        {:state (-> state
                    (assoc :status-message (:error result)
                           :fetching-decks? false))}))

    ::fetch-decks-error
    {:state (-> state
                (assoc :status-message (str "Error fetching decks: " (:error event))
                       :fetching-decks? false))}

    ::push-to-anki
    (let [cards (:parsed-cards state)
          deck-name (:selected-deck state)
          valid-cards (filter :valid cards)]
      (cond
        (nil? deck-name)
        {:state (assoc state :status-message "Please select a deck first")}

        (empty? cards)
        {:state (assoc state :status-message "No cards to push")}

        (empty? valid-cards)
        {:state (assoc state :status-message "No valid cards to push")}

        :else
        {:state (assoc state
                       :status-message "Pushing cards to Anki..."
                       :pushing-cards? true)
         :effects [{:type :http
                    :operation #(anki/push-cards-to-anki cards deck-name)
                    :on-result {:event/type ::push-to-anki-result
                                :cards cards}
                    :on-error {:event/type ::push-to-anki-error}}]}))

    ::push-to-anki-result
    (let [result (:result event)
          cards (:cards event)]
      (if (:success result)
        (let [invalid-cards (remove :valid cards)
              updated-cards (concat (:cards-with-ids result) invalid-cards)]
          {:state (-> state
                      (assoc :status-message
                             (str "Pushed " (:added result) " cards"
                                  (when (> (:duplicates result) 0)
                                    (str ", " (:duplicates result) " duplicates")))
                             :pushing-cards? false
                             :parsed-cards updated-cards))})
        {:state (-> state
                    (assoc :status-message (:error result)
                           :pushing-cards? false))}))

    ::push-to-anki-error
    {:state (-> state
                (assoc :status-message (str "Error pushing cards: " (:error event))
                       :pushing-cards? false))}

    ::copy-prompt
    (let [concept (get-in state [:prompt-inputs :concept] "")]
      (if (str/blank? concept)
        {:state (assoc state :status-message "Please enter a concept first!")}
        (let [prompt-text (prompt/render-prompt (:prompt-inputs state))]
          {:effects [{:type :clipboard
                      :text prompt-text
                      :on-success {:event/type ::copy-prompt-success}
                      :on-error {:event/type ::copy-prompt-error}}]})))

    ::copy-prompt-success
    {:state (assoc state :status-message "Prompt copied to clipboard!")}

    ::copy-prompt-error
    {:state (assoc state :status-message "Failed to copy prompt to clipboard!")}

    ::confirm-paste
    (let [pasted-text (get-in state [:paste-dialog :text] "")
          parse-result (parse-json-response pasted-text)]
      (if (:success parse-result)
        {:state (-> state
                    (assoc :llm-response pasted-text
                           :parsed-cards (:cards parse-result)
                           :status-message (str "Successfully parsed "
                                                (count (:cards parse-result))
                                                " cards"))
                    (assoc-in [:paste-dialog :visible] false)
                    (assoc-in [:paste-dialog :text] ""))}
        {:state (-> state
                    (assoc :llm-response ""
                           :parsed-cards []
                           :status-message (str "JSON parsing error: " (:error parse-result)))
                    (assoc-in [:paste-dialog :visible] false)
                    (assoc-in [:paste-dialog :text] ""))}))

    ;; Return unchanged state for unhandled events (for now)
    {:state state}))

(defn root [{:keys [prompt-inputs status-message llm-response parsed-cards available-decks selected-deck
                     fetching-decks? pushing-cards?]}]
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
                                         (anki-column parsed-cards available-decks selected-deck
                                                      fetching-decks? pushing-cards?)]}
                             ;; Status bar
                             {:fx/type :separator}
                             (status-bar status-message)]}}})

(defn map-event-handler [event]
  ;; Debug logging
  (println "Event:" (:event/type event))

  ;; All events now go through pure handler
  (let [result (handle-event @*state event)]
    ;; Log if there are effects
    (when (:effects result)
      (println "  Effects:" (map :type (:effects result))))

    ;; Update state
    (when-let [new-state (:state result)]
      (reset! *state new-state))

    ;; Handle any effects
    (when-let [effects (:effects result)]
      (effects/perform-effects effects map-event-handler))))

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
