(ns turbo-eureka.ios.components
  (:require [reagent.core :as r]))

(def ReactNative (js/require "react-native"))


(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))


(def web-view (r/adapt-react-class (.-WebView ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def activity-indicator (r/adapt-react-class (.-ActivityIndicator ReactNative)))
(def animated-view (r/adapt-react-class (-> ReactNative .-Animated .-View)))

(def switch (r/adapt-react-class (-> ReactNative .-Switch)))

(defn animated-event [props]
  (let [event-fn (-> ReactNative .-Animated .-event)]
    (event-fn (clj->js props))))

(def animated-spring (-> ReactNative .-Animated .-spring))

(def list-view (r/adapt-react-class (-> ReactNative .-ListView)))
(def DataSource (-> ReactNative .-ListView .-DataSource))

(defn data-source [props]
  (new DataSource (clj->js props)))

(defn clone-ds-with-rows [ds rowData]
  (.cloneWithRows ds (clj->js rowData)))


(def ValueXY (-> ReactNative .-Animated .-ValueXY))

(defn value-xy
  "Creates a new ValueXY object"
  []
  (new ValueXY))

(defn set-value-xy!
  "sets the X and Y of a ValueXY object"
  [value-xy {:keys [x y]}]
  (.setValue value-xy #js {:x x :y y}))

(def PanResponder (-> ReactNative .-PanResponder))
