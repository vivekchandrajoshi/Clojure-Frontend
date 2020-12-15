;; styles for dialog add_comment
(ns cpe.dialog.add-comment.style
	(:require [stylefy.core :as stylefy]
						[garden.color :as gc]
						[garden.units :refer [px]]
						[ht.style :as ht :refer [color color-hex color-rgba]]
						[ht.app.style :as ht-style :refer [vendors]]
						[cpe.app.style :as app-style]))
(def flex-col {
							 :display              "flex"
							 :flex-direction       "column"
							 ::stylefy/auto-prefix #{:flex-direction :flex}
							 ::stylefy/vendors     vendors})

(def flex-row (assoc flex-col :flex-direction "row"
															))

(def container
	(-> flex-row
			(merge {:width      "100%", :height "100%" :font-size "12px"
							:min-height "50px"})
			(assoc ::stylefy/sub-styles
						 {:left  (assoc flex-col :width "200px" :margin-right "20px" )
							:right (assoc flex-col :flex 1
																		 :border-left (str "1px solid "
																											 (color-hex :alumina-grey)))})))
(defn comment-block [h w]
	 {:row   {:width (/ w 1)}
		:col-1 {:width   (/ w 12)
						:text-align "center"
						;:display "-webkit-inline-box"
						:vertical-align "middle"
						:display "inline-block"
						:padding "0 0 6px 0"
						:position "relative"}
		:col-2 {:width   (/ w 2)
						:text-align "center"
						:display "inline-block"
						:vertical-align "middle"
						:padding "0 0 6px 0"
						:position "relative"}
		:comment-label {:vertical-align "top"
										:padding-top "35px"
										}})



(defn body [width]
	(let [f-w (- width 20)
				c-w-1 (- f-w 5)
				c-w-2 (* 0.5 (- f-w 5))
				c-w-3 (* 0.33 (- f-w 5))
				c-w-4 (* 0.25 (- f-w 5))]
		{::stylefy/sub-styles
		 {:data {:f-w f-w
						 :c-w-1 c-w-1, :c-w-2 c-w-2
						 :c-w-3 c-w-3, :c-w-4 c-w-4}
			:form-cell-1 {:width (px c-w-1)
										:vertical-align "top"
										:display "inline-block"
										:padding "0 0 6px 0"
										:position "relative"}
			:form-cell-2 {:width (px c-w-2)
										:vertical-align "top"
										:display "inline-block"
										:padding "0 0 6px 0"
										:position "relative"}
			:form-cell-3 {:width (px c-w-3)
										:vertical-align "top"
										:display "inline-block"
										:padding "0 0 6px 0"
										:position "relative"}
			:form-cell-4 {:width (px c-w-4)
										:vertical-align "top"
										:display "inline-block"
										:padding "0 0 6px 0"
										:position "relative"}
			:form-cell-4x3 {:width (px (* 3 c-w-4))
											:vertical-align "top"
											:display "inline-block"
											:padding "0 0 6px 0"
											:position "relative"}
			:form-heading-label {:color (color-hex :royal-blue)
													 :font-size "14px"
													 :font-weight 400
													 :display "block"
													 :padding "14px 12px 0 12px"
													 :vertical-align "top"}
			:form-label {:color (color-hex :royal-blue)
									 :font-size "12px"
									 :font-weight 300
									 :display "inline-block"
									 :padding "14px 12px 0 12px"
									 :vertical-align "top"}
			:form-value {:color (color-hex :royal-blue)
									 :font-size "12px"
									 :font-weight 700
									 :display "inline-block"
									 :padding "14px 12px 0 12px"
									 :vertical-align "top"}
			:form-error {:color (color-hex :red)
									 :font-size "11px"
									 :display "block"
									 :position "absolute"
									 :bottom 0,
									 :left "12px"}
			:div-error {:color (color-hex :red)
									:font-size "12px"
									:display "block"
									:margin "12px"}
			:div-warning {:color (color-hex :amber)
										:font-size "14px"
										:display "block"
										:margin "12px"}}}))

