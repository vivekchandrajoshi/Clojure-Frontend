(ns cpe.schema.section
  (:require [ht.util.schema :as u]))

(def schema
  {:section {:id     u/id-field
             :name "name"
             :charts "charts"}})