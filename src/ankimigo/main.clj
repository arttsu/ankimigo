(ns ankimigo.main
  (:require [cljfx.api :as fx]
            [ankimigo.prompt :as prompt])
  (:gen-class))

;; State update helpers
(defn update-prompt-input
  "Updates a prompt input field"
  [state field-key new-value]
  (assoc-in state [:prompt-inputs field-key] new-value))

(defn init-state []
  {:prompt-inputs {:concept ""}
   :status-message (if (prompt/template-loaded?)
                     "Ready."
                     "ERROR: Failed to load prompt template!")})

(def *state
  (atom (init-state)))

(defn root [{:keys [prompt-inputs status-message]}]
  {:fx/type :stage
   :showing true
   :title "AnkiMigo - Tracer Bullet"
   :width 1000
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :spacing 10
                  :padding 10
                  :children [{:fx/type :h-box
                              :spacing 10
                              :children [{:fx/type :label
                                          :text "Concept:"}
                                         {:fx/type :text-field
                                          :text (get-in prompt-inputs [:concept] "")
                                          :pref-width 300
                                          :on-text-changed {:event/type ::concept-changed}}]}
                             {:fx/type :separator}
                             {:fx/type :label
                              :text "Final Prompt:"}
                             {:fx/type :text-area
                              :text (prompt/render-prompt prompt-inputs)
                              :editable false
                              :wrap-text true
                              :pref-row-count 15
                              :v-box/vgrow :always}
                             {:fx/type :separator}
                             {:fx/type :label
                              :text status-message}]}}})

(defn map-event-handler [event]
  (case (:event/type event)
    ::concept-changed (swap! *state update-prompt-input :concept (:fx/event event))
    (println "Unhandled event:" (:event/type event))))

(defn -main [& _args]
  (println "Starting AnkiMigo Tracer Bullet")
  (fx/mount-renderer *state
                     (fx/create-renderer
                      :middleware (fx/wrap-map-desc assoc :fx/type root)
                      :opts {:fx.opt/map-event-handler map-event-handler})))
