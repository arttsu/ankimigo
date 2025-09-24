(ns ankimigo.effects
  (:require [ankimigo.anki :as anki])
  (:import [javafx.scene.input Clipboard ClipboardContent]))

;; Effect handlers

(defn http-effect
  "Execute an async HTTP operation and dispatch result/error events"
  [{:keys [operation on-result on-error]} dispatch!]
  (future
    (try
      (let [result (operation)]
        (dispatch! (assoc on-result :result result)))
      (catch Exception e
        (dispatch! (assoc on-error :error (.getMessage e)))))))

(defn dispatch-later-effect
  "Dispatch an event after a delay"
  [{:keys [delay-ms event]} dispatch!]
  (future
    (Thread/sleep delay-ms)
    (dispatch! event)))

(defn clipboard-effect
  "Copy text to clipboard"
  [{:keys [text on-success on-error]} dispatch!]
  (try
    (let [clipboard (Clipboard/getSystemClipboard)
          content (ClipboardContent.)]
      (.putString content text)
      (.setContent clipboard content)
      (when on-success
        (dispatch! on-success)))
    (catch Exception e
      (when on-error
        (dispatch! (assoc on-error :error (.getMessage e)))))))

;; Effect registry
(def effects
  {:http http-effect
   :dispatch-later dispatch-later-effect
   :clipboard clipboard-effect})

(defn perform-effects
  "Execute a collection of effect descriptions"
  [effect-descriptions dispatch!]
  (doseq [effect effect-descriptions]
    (when-let [handler (get effects (:type effect))]
      (handler effect dispatch!))))