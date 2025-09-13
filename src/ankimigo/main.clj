(ns ankimigo.main
  (:require [cljfx.api :as fx])
  (:gen-class))

(def *state
  (atom {:count 0}))

(defn root [{:keys [count]}]
  {:fx/type :stage
   :showing true
   :title "AnkiMigo"
   :width 400
   :height 200
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :alignment :center
                  :spacing 20
                  :children [{:fx/type :label
                              :text (str "Button clicked " count " times")}
                             {:fx/type :button
                              :text "Click Me"
                              :on-action {:event/type ::increment}}]}}})

(defn map-event-handler [event]
  (case (:event/type event)
    ::increment (swap! *state update :count inc)))

(defn -main [& args]
  (println "Starting AnkiMigo")
  (fx/mount-renderer *state
                     (fx/create-renderer
                      :middleware (fx/wrap-map-desc assoc :fx/type root)
                      :opts {:fx.opt/map-event-handler map-event-handler})))
