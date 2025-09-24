(ns ankimigo.anki-api
  "HTTP API layer for AnkiConnect communication"
  (:require [jsonista.core :as json]
            [hato.client :as http])
  (:import [java.net ConnectException SocketTimeoutException]
           [java.io IOException]))

(def ^:private anki-connect-url "http://localhost:8765")

(defn call-anki-connect
  "Make a generic call to AnkiConnect API"
  [action params]
  (try
    (let [response (http/post anki-connect-url
                              {:body (json/write-value-as-string
                                       {:action action
                                        :version 6
                                        :params params})
                               :headers {"Content-Type" "application/json"}
                               :timeout 5000})]
      {:success true :result response})
    (catch ConnectException e
      {:success false
       :error "Cannot connect to Anki. Please make sure Anki is running with AnkiConnect add-on installed."})
    (catch SocketTimeoutException e
      {:success false
       :error "Request timed out. Anki may be busy or unresponsive."})
    (catch IOException e
      {:success false
       :error (str "Network error connecting to Anki: " (.getMessage e))})))

(defn fetch-deck-names
  "Fetches available deck names from AnkiConnect"
  []
  (let [api-result (call-anki-connect "deckNames" {})]
    (if (:success api-result)
      (let [body (json/read-value (:body (:result api-result)))]
        (if-let [error (get body "error")]
          {:success false
           :error (str "AnkiConnect error: " error)}
          {:success true
           :result {:decks (get body "result" [])}}))
      api-result)))

(defn add-notes
  "Add notes to Anki via AnkiConnect"
  [notes]
  (let [api-result (call-anki-connect "addNotes" {:notes notes})]
    (if (:success api-result)
      (let [body (json/read-value (:body (:result api-result)) json/keyword-keys-object-mapper)]
        (if (:error body)
          {:success false
           :error (str "AnkiConnect error: " (:error body))}
          {:success true
           :result body}))
      api-result)))