;; styles for component section
(ns cpe.component.section.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [cpe.app.style :as app-style]))

(def summary
  {::stylefy/sub-styles
   {:add-summary {:padding-bottom "10px",
                  :border-bottom "#54c9e9 solid 1px"}
    :history-block {:background-color "#f3f3f3"
                    :padding "10px"
                    :margin "10px 0px"
                    :margin-right "10px"}
    :header {:font-weight "bold"
             :font-size "12px"
             :color (color :monet-pink)
             :margin-bottom "35px"}
    :header-date {:font-weight "normal"
                  :font-style "italic"}
    :header-left {:position "absolute"
                  :display "inline-block"}
    :header-right {:position "absolute"
                   :right "7px" ;;"15px"
                   :margin-top "-7px"
                   :display "inline-block"
                   :text-align "right"}
    }})

(defn chart-container [view-size]
  (let [h (:height view-size)
        w (* (:width view-size) 0.75)]
    {:container  {:height           h
                  :width            w}
     :scroll-box {:height  (- h 50)
                  :width   w
                  :display "inline-block"}}))


(def chart-style
  {::stylefy/sub-styles
   {:loader       {:height            (px 100)
                   :width             (px 100)
                   :margin            "auto"
                   ;:background-image  "url('../../images/hexagon_spinner.gif')"
                   :background-repeat "no-repeat"
                   :background-size   "contain"
                   :position          "relative"
                   :top               "30%"
                   :display           "block !important"}
    :full-screen  {:position         "fixed"
                   :width            "100%"
                   :height           "100%"
                   :background-color "rgba(11, 11, 11, 0.8)"
                   :z-index          1000
                   :left             0
                   :top              0
                   :padding          "4% 0 0 1%"}
    :display-grid {}}})

(def tab-hader-icon
  {:icon-button-left  {:background-color "#54c9e9"
                       :position         "absolute"
                       :height           (px 25)
                       :border-radius    "50%"
                       :left             (px 4)
                       :top              (px 2)}
   :icon-button-right {:background-color "#54c9e9"
                       :position         "absolute"
                       :height           (px 25)
                       :border-radius    "50%"
                       :top              (px 2)}
   :icon-style        {:color     "#fff"
                       :font-size (px 8)}})
