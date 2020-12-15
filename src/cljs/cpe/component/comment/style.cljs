;; styles for component home
(ns cpe.component.comment.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [cpe.app.style :as app-style]))

(defn comment-style [h w]
  {:comment-tab     {
                     ;;:background-color "#40aade"
                     :height           "30px"
                     :width            w
                     :color            "#ffff"
                     ;:text-align       "right"

                     }
   :comment-header  {
                     ;:height      "30px"
                     :width       (* (/ (/ w 4) 100) 80)
                     :text-align  "center"
                     :padding-top "5px"
                     :color       "#fff"
                     :display "inline-block"
                     :font-weight "500"
                     :padding "3px 0 3px 0"}
   :new-comment     {:width      (* (/ (/ w 4) 100) 10)
                     :padding-right "5px"
                     :text-align "right"
                     :display    "inline-block" }
   :subscribe { :display    "inline-block "
               :width (* (/ (/ w 4) 100) 10)
               :padding "0 5px 0 5px"}
   :comment-section {:color       (color :royal-blue)
                     :padding-top "10px"}
   :comment-subject {:position "relative"
                     :bottom   "2px"
                     :color    (color :royal-blue)}
   :comment-by      {:padding     "5px"
                     :color       (color :monet-pink)
                     :font-size   "12px"
                     :font-weight "bold"}
   :comment-title   {:padding-bottom 5
                     :color          (color :monet-pink)
                     :font-size      "12px"
                     :font-weight    "bold"}
   :comment-item    {:padding "0 5px 0 5px"}
   :commented-on    {:text-align  "right"
                     :color       (color :royal-blue)
                     :font-size   10
                     :padding-top 8}
   :success-msg     {:color        (color :green)
                     :padding-left "20px"}
   :hide            {:display "none"}})

(defn comment-status-icon [level]
  {:icon {:color        (color (case (:name level)
                                 "Critical" :red
                                 "Warning" :amber
                                 "Good" :green
                                 :black))
          :font-size    "20px"
          :margin-right "10px"}})

(defn comment-block [read w]
  {:margin           "5px 5px 0px 5px"
   :padding          "5px"
   :height           "30%"
   :background (if read
                 (color-rgba :sky-blue 20 0.5)
                       "rgb(255, 255, 255)"
                       )
   :width             (+ w 15)
   :text-align       "justify"
   :font-size        "12px"
   :border-radius "10px"})

(def comment-button
  {:background "none"
   :border     "none "
   :cursor     "pointer"
   :font-size  "20px"
   :color      "#fff"
   :padding  "5px 0"})

(def report-toggle
  {:margin-top "-10px"
   :margin-left "0px"
   :margin-bottom "5px"

   ::stylefy/sub-styles
   {:label {:line-height "48px"}}})
