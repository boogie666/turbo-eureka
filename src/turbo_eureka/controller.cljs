(ns turbo-eureka.controller
  (:require [turbo-eureka.model :as m]
            [cljs.core.async :as a :refer [put! <! timeout]]
            [turbo-eureka.styles :as s])
  (:require-macros [cljs.core.async.macros :as a :refer [go go-loop]]))


(defonce action-channel (a/chan))

(defonce web-input-events (a/chan))

(defn dispatch! [action]
  (a/put! action-channel action))


(defmulti process-action! (fn [[type action]] type))

(defmethod process-action! :default [action]
  (println action))

(defmethod process-action! :list-item-view/select [[_ payload]]
  (swap! m/model assoc :selected-item payload))

(defmethod process-action! :async/loaded-products [[_ products]]
  (swap! m/model merge {:products products
                        :products-loaded? true}))

(defmethod process-action! :web-3d-view/on-layout [[_ payload]]
  ;;there seems to be no propper way to get the real position from the View
  ;;so some math is needed.
  ;;the set margin must be added to the position given by the event
  (let [actual-layout {:x (+ (-> payload :layout :x) (or (:margin-left s/main-view) 0))
                       :y (+ (-> payload :layout :y) (or (:margin-top s/main-view) 0))}]
    (swap! m/model assoc :webview-layout (merge (:layout payload) actual-layout))))


(defmethod process-action! :web-3d-view/selectItem [[_ data]]
  (let [id (:id data)
        selection (first (filter #(= (str (:id %)) id) (:products @m/model)))]
    (swap! m/model assoc :selected-item selection)))

(defmethod process-action! :web-3d-view/bad-messages-from-viewer [[_ raw-event]]
  (let [string-event (.stringify js/JSON (clj->js raw-event) nil "\t")]
    (go
      (swap! m/model assoc :web-view-event-error {:error-message "Bad web-view event, all events should have a 'event' key."
                                                  :error-details string-event})
      (<! (a/timeout 5000))
      (swap! m/model dissoc :web-view-event-error))))


(defn in-webview-bounds? [rect point]
  (let [{:keys [x y width height]} rect
        point-x (:x point)
        point-y (:y point)]

    (and (<= 0 point-x (+ x width))
         (<= 0 point-y (+ y height)))))

(defmethod process-action! :selection-view/drop-item [[_ payload]]
  (let [layout (:webview-layout @m/model)
        drop-location {:x (- (-> payload :at :x) (:x layout))
                       :y (- (-> payload :at :y) (:y layout))}]

    (when (in-webview-bounds? layout drop-location)
      (put! web-input-events {:position drop-location
                              :id (-> payload :item :option-name)}))))

(defonce event-loop
  (go-loop []
    (let [value (<! action-channel)]
      (process-action! value))
    (recur)))
