(ns ankimigo.anki
  "Business logic for Anki integration"
  (:require [ankimigo.anki-api :as api]
            [clojure.string :as str])
  (:import [java.net ConnectException SocketTimeoutException]
           [java.io IOException]))

(defn fetch-deck-names
  "Fetches available deck names from AnkiConnect"
  []
  (try
    (let [body (api/fetch-deck-names)]
      (if-let [error (get body "error")]
        {:success false
         :error (str "AnkiConnect error: " error)}
        {:success true
         :result {:decks (get body "result" [])}}))
    (catch ConnectException e
      {:success false
       :error "Cannot connect to Anki. Please make sure Anki is running with AnkiConnect add-on installed."})
    (catch SocketTimeoutException e
      {:success false
       :error "Request timed out. Anki may be busy or unresponsive."})
    (catch IOException e
      {:success false
       :error (str "Network error connecting to Anki: " (.getMessage e))})
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


(defn process-add-notes-response
  "Process AnkiConnect addNotes response and create result"
  [body valid-cards]
  (let []
    (if (:error body)
      {:success false
       :error (str "AnkiConnect error: " (:error body))}
      (let [results (:result body)
            successful (remove nil? results)
            failed (filter nil? results)]
        {:success true
         :result {:total (count valid-cards)
                  :added (count successful)
                  :duplicates (count failed)
                  :note-ids successful
                  :cards-with-ids (mapv (fn [card note-id]
                                          (assoc card :anki-note-id note-id))
                                        valid-cards results)}}))))

(defn push-cards-to-anki
  "Push validated cards to Anki via AnkiConnect addNotes API"
  [cards deck-name]
  (try
    (let [valid-cards (filter :valid cards)]
      (if (empty? valid-cards)
        {:success false
         :error "No valid cards to push"}
        (let [notes (cards-to-anki-notes valid-cards deck-name)
              response-body (api/add-notes notes)]
          (process-add-notes-response response-body valid-cards))))
    (catch ConnectException e
      {:success false
       :error "Cannot connect to Anki. Please make sure Anki is running with AnkiConnect add-on installed."})
    (catch SocketTimeoutException e
      {:success false
       :error "Request timed out. Anki may be busy or unresponsive."})
    (catch IOException e
      {:success false
       :error (str "Network error pushing cards to Anki: " (.getMessage e))})
    (catch Exception e
      {:success false
       :error (str "Failed to push cards to Anki: " (.getMessage e))})))