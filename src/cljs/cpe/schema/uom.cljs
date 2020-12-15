(ns cpe.schema.uom
  (:require [ht.util.schema :as u]))

(def schema
  {:uom {:id u/id-field
         :name "name"
         :units {:name "units"
                 :schema  {:id u/id-field
                           :name "name"
                           :factor "factor"
                           :offset "offset"}
                 :array? true}}})
