(ns turbo-eureka.model
  (:require [reagent.core :refer [atom]]))


(defonce model (atom { :products nil
                       :products-loaded? false
                       :selected-item nil}))
