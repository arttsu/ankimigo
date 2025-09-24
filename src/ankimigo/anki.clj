(ns ankimigo.anki
  "Business logic for Anki integration"
  (:require [ankimigo.anki-api :as api]
            [clojure.string :as str]))

(defn fetch-deck-names
  "Fetches available deck names from AnkiConnect"
  []
  (api/fetch-deck-names))

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


(defn process-add-notes-response
  "Process AnkiConnect addNotes response and create result"
  [api-result valid-cards]
  (if (:success api-result)
    (let [results (get-in api-result [:result :result])
          successful (remove nil? results)
          failed (filter nil? results)]
      {:success true
       :result {:total (count valid-cards)
                :added (count successful)
                :duplicates (count failed)
                :note-ids successful
                :cards-with-ids (mapv (fn [card note-id]
                                        (assoc card :anki-note-id note-id))
                                      valid-cards results)}})
    api-result))

(defn push-cards-to-anki
  "Push validated cards to Anki via AnkiConnect addNotes API"
  [cards deck-name]
  (let [valid-cards (filter :valid cards)]
    (if (empty? valid-cards)
      {:success false
       :error "No valid cards to push"}
      (let [notes (cards-to-anki-notes valid-cards deck-name)
            api-result (api/add-notes notes)]
        (process-add-notes-response api-result valid-cards)))))