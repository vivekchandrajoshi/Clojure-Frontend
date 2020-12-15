;; styles for component uom
(ns cpe.component.uom.style
  (:require [stylefy.core :as stylefy]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [cpe.app.style :as app-style]))

(def uom {})

(def no-config {:font-size "32px"
                :text-align "center"
                :padding "32px 0"
                :color (ht.style/color-hex :red)})

(def column {:display "inline-block"
             :width (px 500)
             :padding (px 5)
             :border-right "solid 1px"
             :border-right-color "#1db0d8"})

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
              :margin "10px -20px -20px"}
      :table-header {:font-size "14px"
                     :font-weight "bold"
                     :background-color "#aaa"
                     :color (:white ht/colors)}
      :table-header-left header-column
      :table-header-right (assoc header-column :border "none" :width (px 150))}}))

(defn row [bg-index]
  (let [row-columns (assoc column :padding "0px 5px"
                                  :line-height "48px"
                                  :height "48px"
                                  :font-size "14px"
                                  :font-weight 400
                                  :border-right-color (:sky-blue ht/colors))
        row-bg-color (if (= bg-index 0)
                       (:white ht/colors)
                       "#e7e9ea")]
    {:background-color row-bg-color
     
     ;;sub-styles
     ::stylefy/sub-styles
                       {:left row-columns
                        :right (assoc row-columns :border-right "none",
                                      :width (px 150) :line-height "",
                                      :display "inline")}}))
