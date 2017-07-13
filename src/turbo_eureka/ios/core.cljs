(ns turbo-eureka.ios.core
  (:require [reagent.core :as r :refer [atom]]))

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


(defn app-root []
  [web-view {:style {:background-color "rgba(255,255,255,0.8)",
                     :height 350}
             :source {:uri "web/index.html"}}])

(defn init []
  (.registerComponent app-registry "TurboEureka" #(r/reactify-component app-root)))
