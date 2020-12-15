;; styles for component settings
(ns cpe.component.settings.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [cpe.app.style :as app-style]))

(def widget-bg-d (color-hex :alumina-grey 30))
(def widget-bg-e (color-hex :sky-blue))
(def widget-bg-h (color-hex :sky-blue 20))
(def widget-fg (color-hex :white))
(def widget-err (color-hex :red))
(def widget-imp (color-hex :monet-pink))
(def widget-transition {:std "450ms ease-in-out"})

(def no-config {:font-size "32px"
                :text-align "center"
                :padding "32px 0"
                :color (ht.style/color-hex :red)})

(defn column [w]
  {:display "inline-block"
   :width (px  w)
   :padding (px 5)
   :border-right "solid 1px"
   :border-right-color "#1db0d8"
   :text-align "center"})

(defn body [width height]
  (let [fs-h (- height 25)
        fs-w (* (- width 55) 1)
        ;f-w (- fs-w 5)
        ;c-w (- (* 0.5 f-w) 5)
        ;c-w-2 (- f-w 5)
        ]
    {:width         (px width)
     :height        (px height)
     :padding       "0px 0px"
     :border       "none"
     :border-radius "8px"
     :background-color (:white ht/colors)

     ::stylefy/sub-styles
     {:form-scroll {:height (px fs-h)
                    :width (px fs-w)
                    :display "inline-block"
                    :vertical-align "top"}


      :table {:width (px 785)
              :margin "0px auto"
              :border-color (:sky-blue ht/colors)
              :border "#00bcf3 solid 1px"
              :border-radius "13px"
              :overflow "hidden"}

      :table-header {:background-color (:sky-blue ht/colors)
                     :color (:white ht/colors)}
      :table-header-left (column 300)
      :table-header-middle (column 300)
      :table-header-right  (-> (column 150)
                               (assoc  :border-right "none"))}}
    ))



(defn chart-config[width height]
  (let [fs-h (- height 25)
        fs-w (* (- width 32) 1)
        ;f-w (- fs-w 5)
        ;c-w (- (* 0.5 f-w) 5)
        ;c-w-2 (- f-w 5)
        ]
    {:width         (px width)
     :height        (px height)
     :padding       "14px 10px 0px 10px"
     :border        "none"
     :border-radius "8px"
     :background-color (:white ht/colors)

     ::stylefy/sub-styles
     {:form-scroll {:height (px fs-h)
                    :width (px fs-w)
                    :display "inline-block"
                    :vertical-align "top"}

      :table {:width (px (- width 90))
              :margin "0px auto"
              :border-color (:sky-blue ht/colors)
              :border "#00bcf3 solid 1px"
              :border-radius "13px"
              :overflow "hidden"}

      :table-header {:background-color (:sky-blue ht/colors)
                     :color (:white ht/colors)}
      :table-header-row (column (/ (- width 300) 6))
			:table-header-row-fixed (column 80)
      :table-header-right (-> (column (/ (- width 300) 6))
                              (assoc  :border-right "none")) }}))

(defn row [bg-index]
  (let [ column (column 300)
        row-columns (assoc column :padding "0px 5px"
                                  :line-height "48px"
                                  :height "48px"
                                  :font-size "14px"
                                  :font-weight 400
                                  :border-right-color (:sky-blue ht/colors))
        row-bg-color (if (= bg-index 0)
                       (:white ht/colors)
                       "#e7e9ea"
                       )]
    {:background-color row-bg-color
     ;;sub-styles
     ::stylefy/sub-styles
                       {:left row-columns
                        :middle row-columns
                        :right (assoc row-columns :border-right "none" :width (px 150) :line-height "" :display "inline")}}))

(defn chart-config-row [bg-index w]
  (let [ column (column (/ (- w 270) 6))
        row-columns (assoc column :padding "0px 5px"
                                  :line-height "48px"
                                  :height "48px"
                                  :font-size "12px"
                                  :font-weight 400
                                  :border-right-color (:sky-blue ht/colors))
        row-bg-color (if (= bg-index 0)
                       (:white ht/colors)
                       "#e7e9ea")]
    {:background-color row-bg-color
     ;;sub-styles
     ::stylefy/sub-styles
                       {:left row-columns
                        :middle row-columns
                        :right (assoc row-columns  :border-right-color "none"
                                                   :border-right "none")}}))
(defn chart-config-row-fixed [bg-index w]
	(let [ column (column 80)
				row-columns (assoc column :padding "0px 5px"
																	:line-height "48px"
																	:height "48px"
																	:font-size "12px"
																	:font-weight 400
																	:border-right-color (:sky-blue ht/colors))
				row-bg-color (if (= bg-index 0)
											 (:white ht/colors)
											 "#e7e9ea")]
		{:background-color row-bg-color
		 ;;sub-styles
		 ::stylefy/sub-styles
											 {:left row-columns
												:middle row-columns
												:right (assoc row-columns  :border-right-color "none"
																									 :border-right "none")}}))

;; (120+)*x48
(defn text-input [read-only? valid?]
  {:display "inline-block"
   :padding "0px 12px"
   :vertical-align "top"
   ::stylefy/sub-styles
   {:main {:height "32px"
           :border (str "1px solid " (if valid? widget-bg-e widget-err))
           :border-radius "16px"
           :min-width "32px"
           :padding "0 12px"
           :font-size "12px"
           :color (if valid? widget-bg-e widget-err)
           :background-color (if read-only?
															 widget-bg-d
															 widget-fg)}}})

(defn toggle [on? disabled?]
	(let [widget-bg (if disabled? widget-bg-d widget-bg-e)]
		{:display "inline-block"
		 :padding "13px 12px"
		 :vertical-align "top"
		 :width "72px", :height "48px"
		 ::stylefy/sub-styles
		 {:main {:cursor (if-not disabled? "pointer")
						 :width "48px", :height "22px"
						 :border (str "1px solid " widget-bg)
						 :border-radius "11px"
						 :position "relative"
						 :background (if on? widget-bg widget-fg)
						 :color (if on? widget-fg widget-bg)
						 :transition (:std widget-transition)}
			:label {:display "block"
							:user-select "none"
							:font-size "12px"
							:font-weight 300
							:margin "-15px 10px 0 10px"
							:text-align (if on? "left" "right")}
			:circle {:border-radius "50%"
							 :width "14px", :height "14px"
							 :border (if-not on? (str "1px solid " widget-bg))
							 :position "absolute"
							 :top "3px"
							 :background widget-fg
							 :left (if on? "29px" "4px")
							 :transition (:std widget-transition)}}}))
