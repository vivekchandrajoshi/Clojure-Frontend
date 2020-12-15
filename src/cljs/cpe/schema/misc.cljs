(ns cpe.schema.misc
  (:require [ht.util.schema :as u]))


(def schema
  {:misc {:id     u/id-field
          :name   "name"
          :data   "data"}})