(ns cpe.schema.unit-system
  (:require [ht.util.schema :as u]
            [cpe.schema.uoms :as uoms]))

(def schema
  {:unit-system {:id   u/id-field
                 :name "name"
                 :uoms {:name "uoms"
                        :schema uoms/schema
                        :array? true}}})
