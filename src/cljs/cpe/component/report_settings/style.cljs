;; styles for component report-settings
(ns cpe.component.report-settings.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [cpe.app.style :as app-style]))


(def report-settings {})

(def no-config {:font-size "32px"
                :text-align "center"
                :padding "32px 0"
                :color (ht.style/color-hex :red)})

(def column {:display "inline-block"
             :width (px 500)
             :padding (px 5)
             :border-right "solid 1px"
             :border-right-color "#1db0d8"})

(def table-header {:display "inline-block"
                   :font-size "14px"
                   :font-weight "bold"
                   :background-color (:sky-blue ht/colors)
                   :padding "0px 10px"
                   :color (:white ht/colors)})

(def table-row {:display "inline-block"
                :vertical-align "top"
                :line-height "initial"
                :min-height "48px"
                :font-size "14px"
                :padding "0px 10px"
                :color "#000"})

(defn body [width height]
  (let [fs-h (- height 40)
        fs-w (* (- width 40) 1)
        header-column column]
    {:width         (px width)
     :height        (px height)
     :padding       "20px"
     :border        (str "1px solid " app-style/widget-bg-e)
     :border-radius "8px"
     :background-color (:white ht/colors)

     ;;sub-styles
     ::stylefy/sub-styles
     {:form-scroll {:height (px fs-h)
                    :width (px fs-w)
                    :display "inline-block"
                    :vertical-align "top"}
      :unit-system-container {:line-height "40px"
                              :width "785px"
                              :margin "50px auto"}
      :section {:margin "50px auto"
                :border-radius "10px"
                :overflow "hidden"
                :border (str "1px solid " app-style/widget-bg-e)}
      :section-header {:font-size "14px"
                       :font-weight "bold"
                       :padding "0px 10px"
                       :background-color (:sky-blue ht/colors)
                       :color (:white ht/colors)}
      :section-content {:padding "20px"}

      :table {:width (px 785)
              :margin "10px 0px"
              :border (str "1px solid " app-style/widget-bg-e)
              :border-radius "13px"
              :overflow "hidden"}
      :table-header-col1 (assoc table-header :width "280px")
      :table-header-col2 (assoc table-header :width "350px" :border-left "solid 1px #FFF")
      :table-header-col3 (assoc table-header :width "153px" :border-left "solid 1px #FFF")}}))

(defn row [bg-index]
  (let [row-bg-color (if (= bg-index 0)
                       (:white ht/colors)
                       "#e7e9ea")]
    {:background-color row-bg-color
     ::stylefy/sub-styles
     {:col1 (assoc table-row :width "280px"
                   :padding "10px")
      :col2 (assoc table-row :width "350px"
                   :padding "10px"
                   :border-left (str "solid 1px " (:sky-blue ht/colors))
                   :border-right (str "solid 1px " (:sky-blue ht/colors))
                   :line-height "normal")
      :col3 (assoc table-row :width "153px"
                   :padding "10px"
                   :border "none"
                   :line-height "normal")
      :comment-title {:padding-bottom "10px"
                      :color          (color :monet-pink)
                      :font-weight    "bold"}
      :comment-item {}
      :comment-subject {:padding-bottom "10px"}
      :commented-on {:text-align  "right"
                     :color       (color :royal-blue)
                     :font-size   10}}}))
