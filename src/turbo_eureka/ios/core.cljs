(ns turbo-eureka.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.async :as a :refer [alts! <! >!]]
            [turbo-eureka.controller :as c]
            [turbo-eureka.model :as m]
            [turbo-eureka.styles :as s]
            [turbo-eureka.core.components :as comp :refer [view text image touchable-highlight
                                                           app-registry web-view scroll-view activity-indicator
                                                           animated-view animated-event animated-spring
                                                           ValueXY PanResponder DataSource]])
  (:require-macros [cljs.core.async.macros :as a :refer [go go-loop alt!]]))



(defn web-3d-view [style input-chan output-chan]
  (let [webview (atom nil)
        input (a/chan 1 (comp (map (fn [message] {:data message}))
                              (map clj->js)
                              (map #(.stringify js/JSON %))))

        output (a/chan 1 (comp (map #(-> % .-nativeEvent .-data))
                               (map #(.parse js/JSON %))
                               (map #(js->clj % :keywordize-keys true))
                               (map :data)
                               (map (fn [web-view-data] [:web-3d-view/web-event web-view-data]))))
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
         (fn [style input-chan output-chan]
           [view (merge style {:on-layout #(let [values (-> % .-nativeEvent .-layout)
                                                 layout {:layout {:x (.-x values) :y (.-y values)
                                                                  :width (.-width values) :height (.-height values)}}]
                                              (a/put! output-chan [:web-3d-view/on-layout layout]))})


            [web-view {:ref #(reset! webview %)
                       :source {:uri "web/index.html"}
                       :bounces false
                       :scroll-enabled false
                       :on-message #(a/put! output %)}]])
       :component-will-unmount
         (fn [_] (a/put! closer :close) (a/close! closer))})))






(defn selection-view [action-channel]
  (let [drag-model (atom {:dragging? false
                          :pan (new ValueXY)})
        panResponder (.create PanResponder #js{:onPanResponderStart #(swap! drag-model merge {:dragging? true})
                                                :onPanResponderEnd #(swap! drag-model merge {:dragging? false})
                                                :onStartShouldSetPanResponder (constantly true)
                                                :onPanResponderMove (animated-event #js[nil, #js{:dx (.-x (:pan @drag-model)) :dy (.-y (:pan @drag-model))}])
                                                :onPanResponderRelease #(let [drop-position {:x (-> % .-nativeEvent .-pageX) :y (-> % .-nativeEvent .-pageY)}
                                                                              drop-event {:item (:selected-item @m/model) :at drop-position}]
                                                                          (a/put! action-channel [:selection-view/drop-item drop-event]))})]

    (fn [action-channel]
      (let [dragging? (:dragging? @drag-model)
            pan (:pan @drag-model)]
        (when (-> @m/model :selected-item)
          (let [item (some-> @m/model :selected-item)]
            [view {:style (-> s/selection-view :main)}
              [view {:style (-> s/selection-view :item)}
                  [animated-view
                    (merge (js->clj (.-panHandlers panResponder)) (when dragging? {:style [(.getLayout pan) {:position "absolute"}]}))
                    [view {:style (-> s/selection-view :handle)}
                      [image {:style (-> s/selection-view :image) :source (:img item)}]
                      [text (:name item)]]]]

              [view {:style (-> s/selection-view :details)}
                [text (:description item)]]]))))))



(defn list-view-item [action-channel item]
  [touchable-highlight {:on-press #(a/put! action-channel [:list-item-view/select item])}
    [view {:style s/list-view-item}
      [image {:style s/list-view-item-image :source (:img item)}]
      [text {:style s/list-view-item-text} (:name item)]]])

(defn list-view []
  (let [dataSource (new DataSource #js{:rowHasChanged not=})]
    (fn []
      (let [items-loaded? (:products-loaded? @m/model)
            items         (:products @m/model)]
          (if-not items-loaded?
            [view {:style s/list-view-loading}
              [activity-indicator]]
            (let [ds (.cloneWithRows dataSource (clj->js items))]
              [comp/list-view {:style s/list-view
                               :dataSource ds
                               :renderRow #(r/as-element [list-view-item c/action-channel (js->clj % :keywordize-keys true)])}]))))))

(defn app-root []
  [view {:flex 1}
    [view {:style s/main-view}
      [list-view]
      [web-3d-view {:style s/web-3d-view} c/web-input-events c/action-channel]]
    [selection-view c/action-channel]])


;;fake a backend call
(go (<! (a/timeout 1000))
    (>! c/action-channel
        [:async/loaded-products
           (take 100
              (cycle [{:name "Active Dress" :id "3212344" :description "The most active of all the dresses" :img (js/require "./images/Active_Dress_1.jpg")}
                      {:name "Floral Dress" :id "3212345" :description "Pritty pritty flowers, i like flowers." :img (js/require "./images/Floral_Dress_1.jpg")}
                      {:name "Pattern Shirt" :id "3212346" :description "Cool design bro... much like." :img (js/require "./images/Pattern_Shirt_1.jpg")}
                      {:name "Cotton Top" :id "3212347" :description "Cotton... Mostly." :img (js/require "./images/Cotton_Top_1.jpg")}]))]))


(defn init []
  (.registerComponent app-registry "TurboEureka" #(r/reactify-component app-root)))
