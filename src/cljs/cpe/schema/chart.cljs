(ns cpe.schema.chart
  (:require [ht.util.schema :as u]))

(def schema
  (let [series {:name  "sensorName"
                :color "color"
                :shape "shape"}
        axis {:series {:name   "series"
                       :schema series
                       :array? true}
              :label  "label"}]
    {:chart {:id          u/id-field
             :title       "title"
             :name        "name"
             :description "description"
             :y1-axis      {:name   "y1Axis"
                           :schema axis}
             :y2-axis     {:name   "y2Axis"
                           :schema axis}
             :x-axis      {:name   "xAxis"
                           :schema axis}}}))