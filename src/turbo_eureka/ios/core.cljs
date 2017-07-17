(ns turbo-eureka.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.async :as a :refer [alts! <! >!]])
  (:require-macros [cljs.core.async.macros :as a :refer [go go-loop alt!]]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def web-view (r/adapt-react-class (.-WebView ReactNative)))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))


(def x-webview-event
  (comp (map clj->js)
        (map #(.stringify js/JSON %))))

(defn event-from-webview [e]
  (-> (.parse js/JSON (-> e .-nativeEvent .-data))
      (js->clj :keywordize-keys true)
      :data))

(defonce webview-events-chan (a/chan 1 x-webview-event))



(defn web-3d-view []
  (let [webview (atom nil)
        closer (a/chan)]
    (go-loop []
      (let [[val c] (alts! [webview-events-chan closer])]
        (when-not (= c closer)
          (try
            (some-> @webview (.postMessage val))
            (catch :default e
              (.trace js/console e)))
          (recur))))
    (r/create-class
      {:reagent-render
         (fn [webview-events-chan actions-chan]
            [web-view {:ref #(reset! webview %)
                       :source {:uri "web/index.html"}
                       :on-message #(alert (:data (event-from-webview %)))}])
       :component-will-unmount
         (fn [_] (a/put! closer :close) (a/close! closer))})))



(defn app-root []
  [view {:flex 1}
    [web-3d-view]
    [touchable-highlight
     {:on-press #(a/put! webview-events-chan {:message "Hello"})}
     [text "click me"]]])




(defn init []
  (.registerComponent app-registry "TurboEureka" #(r/reactify-component app-root)))
