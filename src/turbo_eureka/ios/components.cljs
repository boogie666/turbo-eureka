(ns turbo-eureka.core.components
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


(def list-view (r/adapt-react-class (-> ReactNative .-ListView)))
(def DataSource (-> ReactNative .-ListView .-DataSource))



(def animated-event (-> ReactNative .-Animated .-event))
(def animated-spring (-> ReactNative .-Animated .-spring))

(def ValueXY (-> ReactNative .-Animated .-ValueXY))
(def PanResponder (-> ReactNative .-PanResponder))
