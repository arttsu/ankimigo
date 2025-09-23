(ns ankimigo.anki
  (:require [jsonista.core :as json]
            [hato.client :as http]))

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
  (http/post anki-connect-url
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