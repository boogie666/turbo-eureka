(ns turbo-eureka.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.async :as a :refer [alts! <! >!]]
            [turbo-eureka.controller :as c]
            [turbo-eureka.model :as m]
            [turbo-eureka.styles :as s]
            [turbo-eureka.ios.components :as comp :refer [view text image touchable-opacity
                                                           app-registry web-view scroll-view activity-indicator
                                                           animated-view animated-event animated-spring
                                                           ValueXY PanResponder DataSource]])
  (:require-macros [cljs.core.async.macros :as a :refer [go go-loop alt!]]))

(enable-console-print!)


(defn web-view-event [{:keys [event] :as raw-event}]
  (if (nil? event)
    [:web-3d-view/bad-messages-from-viewer raw-event]
    (let [event-type (keyword "web-3d-view" event)]
      [event-type raw-event])))


(defn web-3d-view [source style input-chan output-chan]
  (let [webview (atom nil)
        input (a/chan 1 (comp (map (fn [message] {:data message}))
                              (map clj->js)
                              (map #(.stringify js/JSON %))))

        output (a/chan 1 (comp (map #(-> % .-nativeEvent .-data))
                               (map #(.parse js/JSON %))
                               (map #(js->clj % :keywordize-keys true))
                               (map :data)
                               (map web-view-event)))
        closer (a/chan)]

    (a/pipe input-chan input)
    (a/pipe output output-chan)

    (go-loop []
      (let [[val c] (alts! [input closer])]
        (when-not (or (= c closer) (nil? val))
          (try
            (some-> @webview (.postMessage val))
            (catch :default e
              (.trace js/console e)))
          (recur))))

    (r/create-class
      {:reagent-render
         (fn [source style input-chan output-chan]
           [view (merge style {:on-layout #(let [values (-> % .-nativeEvent .-layout)
                                                 layout {:layout {:x (.-x values) :y (.-y values)
                                                                  :width (.-width values) :height (.-height values)}}]
                                              (a/put! output-chan [:web-3d-view/on-layout layout]))})


            [web-view {:ref #(reset! webview %)
                       :source {:uri source}
                       :bounces false
                       :scroll-enabled false
                       :on-message #(a/put! output %)}]
            (when-let [error (:web-view-event-error @m/model)]
              [view {:flex 1}
                [text {:style {:background-color "red" :color "white"}}
                  (:error-message error)]
                [text {:style {:background-color "red" :color "white"}}
                  (:error-details error)]])])

       :component-will-unmount
         (fn [_] (a/put! closer :close) (a/close! closer))})))


(defn create-drag-model []
  (atom {:dragging? false
         :pan (comp/value-xy)}))

(defn create-pan-responder [drag-model]
  (.create PanResponder
     #js{:onPanResponderStart #(swap! drag-model merge {:dragging? true})
         :onPanResponderEnd #(swap! drag-model merge {:dragging? false
                                                      :current-item nil})
         :onStartShouldSetPanResponder (constantly true)
         :onPanResponderMove
            (animated-event [nil {:dx (.-x (:pan @drag-model)) :dy (.-y (:pan @drag-model))}])
         :onPanResponderRelease
            #(let [drop-position {:x (-> % .-nativeEvent .-pageX) :y (-> % .-nativeEvent .-pageY)}
                   drop-event    {:item (:selected-item @m/model) :at drop-position}]
               (c/dispatch! [:selection-view/drop-item drop-event])
               (comp/set-value-xy! (:pan @drag-model) {:x 0 :y 0})
               (swap! drag-model merge {:dragging? false
                                        :current-item nil}))}))

(defn selection-view []
  (let [drag-model   (create-drag-model)
        panResponder (create-pan-responder drag-model)]
    (fn []
      (when (-> @m/model :selected-item)
        (let [item (some-> @m/model :selected-item)]
          [view {:style (-> s/selection-view :main)}

            [animated-view (merge (js->clj (.-panHandlers panResponder))
                                  {:style [(.getLayout (:pan @drag-model))
                                           (-> s/selection-view :draggable-item)]})
              [view {:style (-> s/selection-view :item)}
                [view {:style (-> s/selection-view :handle)}
                  [image {:style (-> s/selection-view :image) :source (:img item)}]
                  [text (:name item)]]]]
            [text {:style (-> s/selection-view :details)} (:description item)]])))))

(defn list-view-item [item]
    [touchable-opacity {:on-press #(c/dispatch! [:list-item-view/select item])}
      [view {:style s/list-view-item}
        [image {:style s/list-view-item-image :source (:img item)}]
        [text {:style s/list-view-item-text} (:name item)]]])

(defn list-view []
  (let [dataSource (comp/data-source {:rowHasChanged not=})]
    (fn []
      (let [items-loaded? (:products-loaded? @m/model)
            items         (:products @m/model)]
        (if-not items-loaded?
          [view {:style s/list-view-loading}
            [activity-indicator]]
          (let [ds (comp/clone-ds-with-rows dataSource items)]
            [comp/list-view
              {:style s/list-view
               :dataSource ds
               :renderRow #(r/as-element [list-view-item (js->clj % :keywordize-keys true)])}]))))))

(defn app-root []
  (let [current-view (:selected-view @m/model)]
    [view {:style {:flex 1}}
      [text current-view]
      [comp/switch {:value (= :osgjs current-view)
                    :on-value-change #(swap! m/model assoc :selected-view (if (= :osgjs current-view) :threejs :osgjs))}]
      [view {:style s/main-view}
        (if (= :osgjs current-view)
          [web-3d-view "web/osgjs/index.html" {:style s/web-3d-view} c/web-input-events c/action-channel]
          [web-3d-view "web/threejs/new_3d.html" {:style s/web-3d-view} c/web-input-events c/action-channel])]

      [selection-view]]))


;;fake a backend call
(go (<! (a/timeout 1000))
    (c/dispatch!
        [:async/loaded-products
           (take 100
              (cycle [{:name "Active Dress" :option-name "option2" :id "3212344" :description "The most active of all the dresses" :img (js/require "./images/Active_Dress_1.jpg")}
                      {:name "Floral Dress" :option-name "option1" :id "3212345" :description "Pritty pritty flowers, i like flowers." :img (js/require "./images/Floral_Dress_1.jpg")}
                      {:name "Pattern Shirt" :option-name "option3" :id "3212346" :description "Cool design bro... much like." :img (js/require "./images/Pattern_Shirt_1.jpg")}
                      {:name "Cotton Top" :option-name "option4" :id "3212347" :description "Cotton... Mostly." :img (js/require "./images/Cotton_Top_1.jpg")}]))]))


(defn init []
  (.registerComponent app-registry "TurboEureka" #(r/reactify-component app-root)))
