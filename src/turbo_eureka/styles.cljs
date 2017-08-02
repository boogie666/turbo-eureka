(ns turbo-eureka.styles)


(def list-view
  {:flex 1})

(def list-view-loading
  {:flex 1
   :align-items "center"
   :justify-content "center"})

(def list-view-item-image
 {:width 50 :height 50})

(def list-view-item
  {:display "flex"
   :flex-direction "row"
   :align-items    "center"
   :justify-content "flex-start"
   :padding-bottom 10
   :padding-left 10
   :margin-top 10})

(def list-view-item-text
  {:text-align "center"
   :text-align-vertical "center"})


(def main-view
  {:flex 3
   :flex-direction "row"
   :margin-top 30})


(def web-3d-view
  {:flex 3
   :background-color "red"})


(def selection-view
  {:main {:flex 1 :flex-direction "row"}
   :item {:flex 1}
   :details {:flex 3}
   :handle {:padding 10}
   :draggable-item {:zIndex 1}
   :image {:width 100 :height 100}})
