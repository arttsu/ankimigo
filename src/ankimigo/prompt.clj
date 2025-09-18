(ns ankimigo.prompt
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def prompt-template-path "prompts/default_prompt_v1.md")

;; Load template once at startup
(def prompt-template
  (try
    (-> prompt-template-path
        io/resource
        slurp)
    (catch Exception e
      (println "Error loading prompt template:" (.getMessage e))
      nil)))

(defn template-loaded? []
  (not (nil? prompt-template)))

(defn render-prompt
  "Renders the prompt template with values from prompt-inputs map"
  [prompt-inputs]
  (if-not (template-loaded?)
    (str "ERROR: Prompt template could not be loaded. Check that " prompt-template-path " exists.")
    (let [concept (get prompt-inputs :concept "")]
      (if (str/blank? concept)
        prompt-template
        (str/replace prompt-template "{{concept}}" concept)))))
