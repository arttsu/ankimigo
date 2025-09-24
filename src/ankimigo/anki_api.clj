(ns ankimigo.anki-api
  "HTTP API layer for AnkiConnect communication"
  (:require [jsonista.core :as json]
            [hato.client :as http]))

(def ^:private anki-connect-url "http://localhost:8765")

(defn call-anki-connect
  "Make a generic call to AnkiConnect API"
  [action params]
  (http/post anki-connect-url
             {:body (json/write-value-as-string
                     {:action action
                      :version 6
                      :params params})
              :headers {"Content-Type" "application/json"}
              :timeout 5000}))

(defn fetch-deck-names
  "Fetches available deck names from AnkiConnect"
  []
  (let [response (call-anki-connect "deckNames" {})]
    (json/read-value (:body response))))

(defn add-notes
  "Add notes to Anki via AnkiConnect"
  [notes]
  (let [response (call-anki-connect "addNotes" {:notes notes})]
    (json/read-value (:body response) json/keyword-keys-object-mapper)))