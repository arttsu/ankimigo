(ns ankimigo.main
  (:require [cljfx.api :as fx]
            [ankimigo.prompt :as prompt]
            [clojure.string :as str])
  (:import [javafx.scene.input Clipboard ClipboardContent])
  (:gen-class))

;; State update helpers
(defn update-prompt-input
  "Updates a prompt input field"
  [state field-key new-value]
  (assoc-in state [:prompt-inputs field-key] new-value))

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
                                       :children (map-indexed
                                                  (fn [idx card]
                                                    {:fx/type :v-box
                                                     :spacing 5
                                                     :style "-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-background-radius: 5;"
                                                     :children [{:fx/type :label
                                                                 :text (str "Card " (inc idx))
                                                                 :style "-fx-font-weight: bold;"}
                                                                {:fx/type :label
                                                                 :text (str "Front: " (:front card))
                                                                 :wrap-text true}
                                                                {:fx/type :label
                                                                 :text (str "Back: " (:back card))
                                                                 :wrap-text true}]})
                                                  parsed-cards)}}]}
                {:fx/type :label
                 :text "No cards parsed yet"
                 :style "-fx-text-fill: gray;"})]})

(defn anki-controls []
  [{:fx/type :separator}
   {:fx/type :label
    :text "Deck:"}
   {:fx/type :combo-box
    :disable true
    :prompt-text "Select deck..."
    :max-width Double/MAX_VALUE}
   {:fx/type :button
    :text "Fetch Decks"
    :max-width Double/MAX_VALUE
    :on-action {:event/type ::fetch-decks}}
   {:fx/type :button
    :text "Push to Anki"
    :max-width Double/MAX_VALUE
    :on-action {:event/type ::push-to-anki}}])

(defn anki-column [parsed-cards]
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
                     (anki-controls))})

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

(defn root [{:keys [prompt-inputs status-message llm-response parsed-cards paste-dialog]}]
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
                                         (anki-column parsed-cards)]}
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
    ::fetch-decks (println "Fetch Decks button clicked!")
    ::push-to-anki (println "Push to Anki button clicked!")
    ::cancel-paste (do (println "Cancel paste clicked!")
                       (swap! *state assoc-in [:paste-dialog :visible] false)
                       (swap! *state assoc-in [:paste-dialog :text] ""))
    ::confirm-paste (do (println "Confirm paste clicked!")
                        (let [pasted-text (get-in @*state [:paste-dialog :text] "")]
                          (swap! *state assoc :llm-response pasted-text)
                          (swap! *state assoc-in [:paste-dialog :visible] false)
                          (swap! *state assoc-in [:paste-dialog :text] "")
                          (swap! *state assoc :status-message (str "Pasted " (count pasted-text) " characters"))))
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
