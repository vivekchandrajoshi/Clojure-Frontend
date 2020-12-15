(ns cpe.util.report
	(:require [ht.util.interop :as i]
						[re-frame.core :as rf]
						[cpe.util.image-data :refer [base64]]
						[ht.app.event :as ht-event]
						[ht.util.common :as c :refer [base64]]
						[cpe.app.charts :as charts]
						[cljs-time.core :as t]
						[cljs-time.format :as tf]))

(def date-formatter (tf/formatter "yyyy-MM-dd"))
(defn format-date [date] (tf/unparse date-formatter (t/date-time date)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Different fonts that    ;;
;; can be used in pdfkit   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 'Courier'               ;;
;; 'Courier-Bold'          ;;
;; 'Courier-Oblique'       ;;
;; 'Courier-BoldOblique'   ;;
;; 'Helvetica'             ;;
;; 'Helvetica-Bold'        ;;
;; 'Helvetica-Oblique'     ;;
;; 'Helvetica-BoldOblique' ;;
;; 'Times-Roman'           ;;
;; 'Times-Bold'            ;;
;; 'Times-Italic'          ;;
;; 'Times-BoldItalic'      ;;
;; 'Symbol'                ;;
;; 'ZapfDingbats'          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def style {:base-font    "Helvetica"
						:base-color   "#000"
						:black        "#000"
						:grey         "#888"
						:alumina-grey "#d0d3d4"
						:blue         "#002856"
						:yellow       "#ffeb3b"
						:yellow-dark  "#dca122"
						:red          "#ff0000"
						:green        "#239a1a"
						:orange       "#fe9700"
						:white        "#fff"
						:sky-blue     "#54c9e9"
						:el           "28"
						:l            "18"
						:n            "13"
						:s            "9"
						:es           "7"
						:heading      {:size 20 :font (get style :base-font)}
						:sub-heading  {:size 20 :font (get style :base-font)}})

(def default-paper {:size         "A4"
										:paper-width  612                       ;;592
										:paper-height 792                       ;;842
										:margins      #js{:top 60 :right 40 :bottom 40 :left 40}
										;:w            (- 612 40 40)
										;:h            (- 792 40 40)
										})
(def comment-icon {
									 :thumbs-up        "M4.571 24q0-0.464-0.339-0.804t-0.804-0
.339-0.804 0.339-0.339 0.804 0.339 0.804 0.804 0.339 0.804-0.339 0.339-0.804zM25.143 13.714q0-0.911-0.696-1.598t-1.589-0.688h-6.286q0-1.036 0.857-2.848t0.857-2.866q0-1.75-0.571-2.589t-2.286-0.839q-0.464 0.464-0.679 1.518t-0.545 2.241-1.063 1.955q-0.393 0.411-1.375 1.625-0.071 0.089-0.411 0.536t-0.563 0.732-0.616 0.759-0.714 0.786-0.688 0.634-0.714 0.482-0.634 0.161h-0.571v11.429h0.571q0.232 0 0.563 0.054t0.589 0.116 0.679 0.196 0.625 0.205 0.634 0.223 0.518 0.188q3.768 1.304 6.107 1.304h2.161q3.429 0 3.429-2.982 0-0.464-0.089-1 0.536-0.286 0.848-0.938t0.313-1.313-0.321-1.232q0.946-0.893 0.946-2.125 0-0.446-0.179-0.991t-0.446-0.848q0.571-0.018 0.955-0.839t0.384-1.446zM27.429 13.696q0 1.589-0.875 2.911 0.161 0.589 0.161 1.232 0 1.375-0.679 2.571 0.054 0.375 0.054 0.768 0 1.804-1.071 3.179 0.018 2.482-1.518 3.92t-4.054 1.438h-2.304q-1.714 0-3.384-0.402t-3.866-1.17q-2.071-0.714-2.464-0.714h-5.143q-0.946 0-1.616-0.67t-0.67-1.616v-11.429q0-0.946 0.67-1.616t1.616-0.67h4.893q0.643-0.429 2.446-2.768 1.036-1.339 1.911-2.286 0.429-0.446 0.634-1.527t0.545-2.259 1.107-1.929q0.696-0.661 1.607-0.661 1.5 0 2.696 0.58t1.821 1.813 0.625 3.321q0 1.661-0.857 3.429h3.143q1.857 0 3.214 1.357t1.357 3.196z"

									 :exclamation-sign "M13.714 2.286q3.732 0 6.884 1.839t4.991 4.991 1.839 6.884-1.839 6.884-4.991 4.991-6.884 1.839-6.884-1.839-4.991-4.991-1.839-6.884 1.839-6.884 4.991-4.991 6.884-1.839zM16 24.554v-3.393q0-0.25-0.161-0.42t-0.393-0.17h-3.429q-0.232 0-0.411 0.179t-0.179 0.411v3.393q0 0.232 0.179 0.411t0.411 0.179h3.429q0.232 0 0.393-0.17t0.161-0.42zM15.964 18.411l0.321-11.089q0-0.214-0.179-0.321-0.179-0.143-0.429-0.143h-3.929q-0.25 0-0.429 0.143-0.179 0.107-0.179 0.321l0.304 11.089q0 0.179 0.179 0.313t0.429 0.134h3.304q0.25 0 0.42-0.134t0.188-0.313z"

									 :remove-circle    "M19.589 19.268l-2.607 2.607q-0.179 0.179-0.411 0.179t-0.411-0.179l-2.446-2.446-2.446 2.446q-0.179 0.179-0.411 0.179t-0.411-0.179l-2.607-2.607q-0.179-0.179-0.179-0.411t0.179-0.411l2.446-2.446-2.446-2.446q-0.179-0.179-0.179-0.411t0.179-0.411l2.607-2.607q0.179-0.179 0.411-0.179t0.411 0.179l2.446 2.446 2.446-2.446q0.179-0.179 0.411-0.179t0.411 0.179l2.607 2.607q0.179 0.179 0.179 0.411t-0.179 0.411l-2.446 2.446 2.446 2.446q0.179 0.179 0.179 0.411t-0.179 0.411zM23.429 16q0-2.643-1.304-4.875t-3.536-3.536-4.875-1.304-4.875 1.304-3.536 3.536-1.304 4.875 1.304 4.875 3.536 3.536 4.875 1.304 4.875-1.304 3.536-3.536 1.304-4.875zM27.429 16q0 3.732-1.839 6.884t-4.991 4.991-6.884 1.839-6.884-1.839-4.991-4.991-1.839-6.884 1.839-6.884 4.991-4.991 6.884-1.839 6.884 1.839 4.991 4.991 1.839 6.884z"

									 :envelope         "M13.929 12.589v-6.429q-0.268 0.301-0.578 0.552-2.243 1.724-3.566 2.829-0.427 0.36-0.695 0.561t-0.724 0.406-0.858 0.205h-0.017q-0.402 0-0.858-0.205t-0.724-0.406-0.695-0.561q-1.323-1.105-3.566-2.829-0.31-0.251-0.578-0.552v6.429q0 0.109 0.080 0.188t0.188 0.080h12.321q0.109 0 0.188-0.080t0.080-0.188zM13.929 3.792v-0.205t-0.004-0.109-0.025-0.105-0.046-0.075-0.075-0.063-0.117-0.021h-12.321q-0.109 0-0.188 0.080t-0.080 0.188q0 1.406 1.23 2.377 1.616 1.272 3.357 2.653 0.050 0.042 0.293 0.247t0.385 0.314 0.372 0.264 0.423 0.23 0.36 0.075h0.017q0.167 0 0.36-0.075t0.423-0.23 0.372-0.264 0.385-0.314 0.293-0.247q1.741-1.381 3.357-2.653 0.452-0.36 0.841-0.967t0.389-1.101zM15 3.482v9.107q0 0.552-0.393 0.946t-0.946 0.393h-12.321q-0.552 0-0.946-0.393t-0.393-0.946v-9.107q0-0.552 0.393-0.946t0.946-0.393h12.321q0.552 0 0.946 0.393t0.393 0.946z"})

(def pdf (atom {:doc   nil
								:paper default-paper}))

(defn document
	([]
	 (document nil))
	([doc-object]
	 (if doc-object doc-object (:doc @pdf))))

(defn paper []
	(:paper @pdf))

(defn set-attribute [{:keys [doc] :as attr-list}]
	(doseq [[k v] attr-list]
		(if (not= k :doc)
			(i/ocall (document doc) k (get style v)))))

(defn height-of-string [doc string width align]
	(i/ocall (document doc) :heightOfString string #js{:width width, :align align}))

(defn move-down [{:keys [doc lines]}]
	(i/ocall (document doc) :moveDown lines))

(defn add-text-1 [{:keys [doc text x y w align color]}]
	(if color
		(i/ocall (document doc) :fillColor color))
	(i/ocall (document doc) :text text
					 (+ (:x (paper)) x)
					 (+ (:y (paper)) y)
					 #js{:width w, :align align
							 }))

(defn add-text [{:keys [doc text w x y align color with-image length]}]
	;(print text "align" align)
	(let [pg-ht (i/oget-in doc [:page :height])
				doc-y (i/oget doc :y)
				doc-x (i/oget doc :x)
				pg-margins (i/oget-in doc [:page :margins :bottom])
				buf-ht (- pg-ht doc-y pg-margins)
				current-line-ht (i/ocall doc :currentLineHeight)
				repeat-number (js/Math.floor (/ buf-ht current-line-ht))
				string-height (height-of-string doc text w align)
				total-height (+ string-height 250)
				split-text (if (> length 0)
										 (subs text 0 length))
				rest-text (if  (> length 0)
										(subs text (+ length 2) (count text)))

				]
		(if (and (> total-height buf-ht) with-image)
			(do
				;(print "page break")
				(i/ocall (document doc) :text (apply str (repeat repeat-number
																												 "\n")) x y)
				;(i/ocall (document doc) :fontSize (:l style))
				;(i/ocall (document doc) :fillColor (:blue style))
				;(i/ocall (document doc) :moveDown 1)
				))
		(if color
			(i/ocall (document doc) :fillColor color))
		(if (> length 0)
			(do
				(i/ocall (document doc) :fontSize (:l style))
				(i/ocall (document doc) :fillColor (:blue style))
				(i/ocall (document doc) :text (str split-text
																					 (if (> (count
																										rest-text) 0)
																						 ":-"))
								 x y #js{:width w, :align align :underline true })
				(i/ocall (document doc) :fontSize (:n style))
				(i/ocall (document doc) :fillColor (:black style))
				(if (> (count rest-text) 0)
					(i/ocall (document doc) :text rest-text
									 #_(+ 90 length (i/oget doc :x))
									 x
									 y #js{:width w, :align align	 })))
			(i/ocall (document doc) :text text
							 x y
							 #js{:width w, :align align
									 }))
		;(i/ocall (document doc) :text text
		;				 x y
		;				 #js{:width w, :align align
		;						 })
		))


(defn add-rect [{:keys [doc x y w h color corner-radius
												stroke-color stroke-width opacity] :as prop}]
	(if-not (nil? corner-radius)
		(i/ocall (document doc) :roundedRect (+ (:x (paper)) x) (+ (:y (paper)) y)
						 w h corner-radius)
		(i/ocall (document doc) :rect (+ (:x (paper)) x) (+ (:y (paper)) y) w h))
	(if-not (nil? stroke-width) (i/ocall (document doc) :lineWidth stroke-width))
	(if-not (nil? opacity) (i/ocall (document doc) :fillOpacity opacity))
	(if-not (nil? color)
		(i/ocall (document doc) :fillAndStroke color stroke-color)
		(i/ocall (document doc) :stroke stroke-color)))

(defn add-image1 [{:keys [doc image x y w]}]
	(i/ocall (document doc) :image image (+ (:x (paper)) x) (+ (:y (paper)) y)
					 #js{:width w}))

(defn add-image [{:keys [doc image x y w h]}]
	(let [pg-ht (i/oget-in doc [:page :height])
				doc-y (i/oget doc :y)
				pg-margins (i/oget-in doc [:page :margins :bottom])
				buf-ht (- pg-ht doc-y pg-margins)
				current-line-ht (i/ocall doc :currentLineHeight)
				repeat-number (js/Math.floor (/ buf-ht current-line-ht))
				]
		;(if (> h buf-ht)
		;	(add-text {:doc doc :text (apply str (repeat repeat-number "\n"))}))
		(i/ocall (document doc) :image image x y #js{:height h :width w})
		#_(if (< h buf-ht)
				(i/ocall (document doc) :text "\n" x y
								 #js{:width w,}))))

(defn add-circle [{:keys [doc x y radius color stroke-color stroke-width opacity]}]
	(i/ocall (document doc) :circle x y radius)
	(if-not (nil? stroke-width) (i/ocall (document doc) :lienWidth stroke-width))
	(if-not (nil? opacity) (i/ocall (document doc) :fillOpacity opacity))
	(if-not (nil? color)
		(i/ocall (document doc) :fillAndStroke color stroke-color)
		(i/ocall (document doc) :stroke stroke-color)))

(defn add-line [{:keys [doc coordinates stroke-color stroke-width]}]
	(i/ocall (document doc) :moveTo (first (first coordinates)) (second (first coordinates)))
	(doseq [[x y] (rest coordinates)]
		(i/ocall (document doc) :lineTo x y))
	(i/ocall (document doc) :stroke stroke-color))

(defn add-svg [{:keys [doc path stroke-color]}]
	(i/ocall (document doc) :path (path comment-icon)
					 #js{:height 10 :width 10})
	(i/ocall (document doc) :stroke stroke-color)
	)



(defn doc-rotate [{:keys [doc direction rotation x y]}]
	(let [new-paper (case direction
										"CW" (-> default-paper
														 (assoc :x (* -1 (:paper-height default-paper)))
														 (assoc :w (:h default-paper))
														 (assoc :h (:w default-paper))
														 (assoc :paper-width (:paper-height default-paper))
														 (assoc :paper-height (:paper-width default-paper)))
										"CCW" (-> default-paper
															(assoc :y (* -1 (:paper-width default-paper)))
															(assoc :w (:h default-paper))
															(assoc :h (:w default-paper))
															(assoc :paper-width (:paper-height default-paper))
															(assoc :paper-height (:paper-width default-paper)))
										"FLIP" (-> default-paper
															 (assoc :x (* -1 (:paper-width default-paper)))
															 (assoc :y (* -1 (:paper-height default-paper))))
										default-paper)]
		(case direction
			"FLIP" (i/ocall (document doc) :rotate -180 #js{:origin #js[0 0]})
			"CW" (i/ocall (document doc) :rotate -90 #js{:origin #js[0 0]})
			"CCW" (i/ocall (document doc) :rotate 90 #js{:origin #js[0 0]})
			(i/ocall (document doc) :rotate rotation #js{:origin #js[x y]}))

		(swap! pdf assoc :paper new-paper)))

(defn add-page-numbers [{:keys [doc color]}]
	(let [pages-data (js->clj (i/ocall (document doc) :bufferedPageRange)
														:keywordize-keys true)
				total-pages (+ (:start pages-data) (:count pages-data))
				page-seq (range 0 total-pages)]
		(reduce (fn [cp page-index]
							(i/ocall (document doc) :switchToPage page-index)
							(i/ocall (document doc) :fontSize (get style :s))
							(i/ocall (document doc) :fillColor (get style :grey))
							(i/ocall (document doc) :text (str cp "/" total-pages), 40, 70,
											 #js{:width (:w (paper)), :align "right"})
							(inc cp)
							) 1 page-seq)))

(defn header [{:keys [date doc client-name]}]
	[:group [[:image {:image (:ht-logo base64), :x 435, :y 40, :w 120}]
					 [:page {:fontSize :s, :fillColor :grey}]
					 [:text {:text (str client-name
															", "
															(format-date date)
															), :x 40, :y 70, :w 532, :align "left"}]
					 [:line {:coordinates [[40 80] [555 80]], :stroke-width 1, :stroke-color (:grey style)}]
					 [:page {:fontSize :n, :fillColor :black}]]])

(defn footer []
	[:group [[:image {:image (:watermark-hexagon base64), :x 20, :y 482, :w 590}]]])

(defn draw-schema [doc schema]
	(mapv (fn [[type property]]
					(case type
						:page (set-attribute (assoc property :doc doc))
						:doc-rotate (doc-rotate (assoc property :doc doc))
						:add-page (set-attribute (assoc property :doc doc))
						:end (set-attribute (assoc property :doc doc))
						:moveDown (move-down (assoc property :doc doc))
						:text (add-text (assoc property :doc doc))
						:rect (add-rect (assoc property :doc doc))
						:image (add-image (assoc property :doc doc))
						:circle (add-circle (assoc property :doc doc))
						:line (add-line (assoc property :doc doc))
						:group (draw-schema doc property)
						:svg (add-svg (assoc property :doc doc))
						:add-page-numbers (add-page-numbers (assoc property :doc doc))
						nil))
				schema))

;;;;; custom function ;;;;;;;


(defn set-page [props]
	(let [custom-font (:custom-font props)]
		(if custom-font
			(do
				(i/ocall (:doc props) :registerFont "customFont" custom-font)
				(i/ocall (:doc props) :font "customFont"))
			(i/ocall (:doc props) :font (:base-font style)))
		[:group [[:page {:fontSize :n, :fillColor :base-color}]
						 (footer)
						 (header props)
						 ]]))

(defn prepare-comment-list [props chart section-name]
	(mapv (fn [d]
					(let [comments (props :comments)
								name (-> (props :chart)
												 (get-in [d :name]))
								title (get-in props [:chart d :title])
								y1-axis-series (get-in props [:chart d :y1-axis :series])
								y2-axis-series (get-in props [:chart d :y2-axis :series])
								y2-axis? (if (get-in props [:chart d :y2-axis])
													 true)
								x-title (get-in props [:chart d :x-axis :label])
								y1-title (get-in props [:chart d :y1-axis :label])
								y2-title (get-in props [:chart d :y2-axis :label])
								get-sql-value (props :sensor-data)
								y1-sensor-name (mapv (fn [d]
																			 (d :name))
																		 y1-axis-series)
								y2-sensor-name (if y2-axis?
																 (mapv (fn [d]
																				 (d :name))
																			 y2-axis-series))
								y1-uom (mapv (fn [d]
															 (let [sensor-uom-id (get-in props
																													 [:sensor d :uom-id])
																		 sensor-uom (get-in (props
																													:units)
																												[sensor-uom-id :selected-unit-id])
																		 selected-uom-list (get-in (props
																																 :units)
																															 [sensor-uom-id
																																:units])
																		 selected-uom-name (remove nil?
																															 (map
																																 (fn [d]
																																	 (if (= (:id d)
																																					sensor-uom)
																																		 (:name d))
																																	 ) selected-uom-list))]


																 (first selected-uom-name)
																 ))
														 y1-sensor-name)
								y2-uom (mapv (fn [d]
															 (let [sensor-uom-id (get-in props
																													 [:sensor d :uom-id])
																		 sensor-uom (get-in (props
																													:units)
																												[sensor-uom-id :selected-unit-id])
																		 selected-uom-list (get-in (props
																																 :units)
																															 [sensor-uom-id
																																:units])
																		 selected-uom-name (remove nil?
																															 (map
																																 (fn [d]
																																	 (if (= (:id d)
																																					sensor-uom)
																																		 (:name d))
																																	 ) selected-uom-list))]


																 (first selected-uom-name)
																 ))
														 y2-sensor-name)
								y1-color-shape (if y1-axis-series
																 (into {} (map (fn [d]
																								 {(:name d) {:color (:color d)
																														 :shape :square
																														 ;(:shape d)
																														 }}) y1-axis-series)))
								y2-color-shape (if y1-axis-series
																 (into {} (map (fn [d]
																								 {(:name d) {:color (:color d)
																														 ;(:color d)
																														 :shape :circle
																														 ;(:shape d)
																														 }}) y2-axis-series)))
								merged-sensor (into (if (> (count y1-sensor-name) 0)
																			y1-sensor-name
																			[])
																		(if y2-axis?
																			y2-sensor-name
																			[]))
								x-min (if merged-sensor
												(apply min (mapv (fn [d]
																					 (get-in get-sql-value
																									 [d :x-min]))
																				 merged-sensor)))
								x-max (if merged-sensor
												(apply max (mapv (fn [d]
																					 (let [x-max (get-in
																												 get-sql-value
																												 [d :x-max])
																								 x-min (get-in
																												 get-sql-value
																												 [d :x-min])
																								 x-dif (- x-max x-min)
																								 x-per (* (/ x-dif 100)
																													1)]
																						 (+ x-max x-per)))
																				 merged-sensor)))
								y1-min (if y1-sensor-name
												 (apply min (mapv (fn [d]
																						(get-in get-sql-value [d :y-min]))
																					y1-sensor-name)))
								y1-max (if y1-sensor-name
												 (apply max (mapv (fn [d]
																						(let [y-max (get-in get-sql-value [d :y-max])
																									y-min (get-in get-sql-value [d :y-min])
																									y-dif (- y-max y-min)
																									y-per (* (/ y-dif 100)
																													 10)]
																							(+ y-max y-per)))
																					y1-sensor-name)))
								y2-min (if y2-sensor-name
												 (apply min (mapv (fn [d]
																						(get-in get-sql-value [d :y-min]))
																					y2-sensor-name)))
								y2-max (if y2-sensor-name
												 (apply max (mapv (fn [d]
																						(let [y-max (get-in get-sql-value [d :y-max])
																									y-min (get-in get-sql-value [d :y-min])
																									y-dif (- y-max y-min)
																									y-per (* (/ y-dif 100)
																													 10)]
																							(+ y-max y-per)))
																					y2-sensor-name)))
								y1-data (if y1-sensor-name
													(into [] (map (fn [d]
																					(let [sensor-desc (get-in props
																																		[:sensor d
																																		 :description])]
																						(merge {:rfq  (get-in
																														props
																														[:sensor-data d
																														 :rfq])
																										:name        d
																										:sensor-desc sensor-desc
																										:data        (map
																																	 (fn [d]
																																		 {:x (:x d)
																																			:y (:y d)})
																																	 (get-in get-sql-value [d :sensor-value]))}
																									 (get y1-color-shape d))))
																				y1-sensor-name)))

								y2-data (if y2-sensor-name
													(into [] (map (fn [d]
																					(let [sensor-desc (get-in props
																																		[:sensor d
																																		 :description])]
																						(merge {:rfq  (get-in
																														props
																														[:sensor-data d
																														 :rfq])
																										:name        d
																										:sensor-desc sensor-desc
																										:data        (map
																																	 (fn [d]
																																		 {:x (:x d)
																																			:y (:y d)})
																																	 (get-in get-sql-value [d :sensor-value]))
																										} (get y2-color-shape d))))
																				y2-sensor-name)))
								y1-domain [y1-min y1-max]
								y2-domain [y2-min y2-max]
								x-domain [x-min x-max]
								y1-label (if (and y1-title (first y1-uom))
													 (str y1-title " " "[" (first y1-uom) "]")
													 y1-title
													 )
								y2-label (if (and y2-title (first y2-uom))
													 (str y2-title " " "[" (first y2-uom) "]")
													 y2-title
													 )

								image (charts/get-chart-blob 490 250 title x-title y1-label
																						 y2-label x-domain
																						 y2-axis? y1-domain y2-domain
																						 y1-data y2-data)
								]
						{:name  name
						 :image image
						 :comments
										(remove nil? (mapv (fn [c]
																				 (let [comment-chart-id
																							 (get-in c [:chart-info
																													:chart-id :id])]

																					 (if (= d comment-chart-id)
																						 c)))
																			 comments))}))
				chart)
	)

(defn generate-report-data [props doc]
	{:doc         doc
	 :client-name (props :client-name)
	 :date        (js/Date.)
	 :is-topsoe   (props :is-topsoe)
	 :sections    (mapv (fn [d]
												(let [section-name (-> (props :section)
																							 (get-in [d :name]))

															section-chart (-> (props :section)
																								(get-in [d :charts]))]
													(do
														{:section section-name
														 :data    (prepare-comment-list props section-chart
																														section-name)
														 })
													))
											(props :plant-section))}
	)

(defn summary [props]
	(let [report-heading "Clearview™"]
		[:group [
						 (set-page {:date (js/Date.) :doc (props :doc) :client-name
															(props :client-name)})
						 [:page {:fontSize :el, :fillColor :blue}]
						 [:moveDown {:doc (props :doc) :lines 1}]
						 [:text {:doc   (props :doc)
										 :text  report-heading
										 :w     515
										 :align "left"
										 }]
						 [:line {:coordinates  [[40 (+ 75 (i/oget (props :doc) :y))]
																		[555 (+ 75 (i/oget (props :doc) :y))]],
										 :stroke-width 1,
										 :stroke-color
																	 (:blue style)}]
						 [:page {:fontSize :l, :fillColor :black}]
						 [:moveDown {:doc (props :doc) :lines 1}]
						 [:text {:doc   (props :doc)
										 :text  "Summary"
										 :w     515
										 :align "right"
										 }]
						 [:page {:fontSize :n, :fillColor :black}]
						 [:moveDown {:doc (props :doc) :lines 2}]
						 [:text {:doc   (props :doc)
										 :text  (props :summary)
										 :w     515
										 :x     40
										 :align "justify"}]]]))

(defn comment-reply [doc comments is-topsoe]
	(if (> (count comments) 0)
		(reduce-kv (fn [col index comment]
								 (let [comment-created (str (if is-topsoe
																							(comment :created-by)
																							(if (= (re-find #"@topsoe.com" (comment :created-by)) "@topsoe.com")
																								(str "Topsoe")
																								(str (comment :created-by))))
																						" :- "
																						)
											 comm-created-length (count comment-created)]
									 (-> col
											 (conj [:page {:fontSize :n, :fillColor :black}])
											 (conj [:text {:doc doc
																		 :text ""
																		 :x 80}])
											 (conj [:text {:doc    doc
																		 :text   comment-created
																		 :x      80
																		 :w      475
																		 :align  "justify"
																		 :length comm-created-length
																		 }])
											 (conj [:text {:doc   doc
																		 :text  (comment :comment)
																		 :x     80
																		 :w     475
																		 :align "justify"}])
											 )))
							 [] comments)
		(conj [] [:page {:fontSize :n, :fillColor :black}])))


(defn chart-commetns [doc props is-topsoe]
	(if (> (count props) 0)
		(reduce-kv (fn [col index comment]
								 (let [type-id (get-in comment [:type-id :id])
											 created-by (comment :created-by)
											 subject (comment :subject)
											 start-date (get-in comment [:chart-info :start-of-run-day])
											 end-date (get-in comment [:chart-info :end-of-run-day])
											 chart-name (get-in comment [:chart-info :chart-id :name])
											 comments (comment :comments)
											 comment-created (if is-topsoe
																				 created-by
																				 (if (= (re-find #"@topsoe.com" created-by) "@topsoe.com")
																					 (str "Topsoe")
																					 (str created-by)))
											 comm-created-length (count comment-created)
											 chart-comment-info (str comment-created
																							 (if start-date
																								 (str ": Run days "
																											start-date))
																							 (if end-date
																								 (str " To " end-date))
																							 ": " chart-name)]
									 (-> col
											 (conj [:page {:fontSize :l, :fillColor :black}])
											 (conj [:text {:doc   doc
																		 :text  (if (= index 0)
																							"Comments:"
																							"")
																		 :w     495
																		 :x     40
																		 :align "left"
																		 }])
											 (conj [:page {:fontSize :n, :fillColor :blue}])
											 (conj [:moveDown {:doc doc :liens 1}])
											 (conj [:text {:doc    doc
																		 :text   chart-comment-info
																		 :w      495
																		 :x      60
																		 :align  "justify"
																		 :length comm-created-length}])
											 (conj [:text {:doc   doc
																		 :text  subject
																		 :w     495
																		 :x     60
																		 :align "justify"}])
											 (conj [:moveDown {:doc doc :liens 1}])
											 (into (comment-reply doc comments is-topsoe))))
								 ) [] (into [] props))
		(conj [] [:page {:fontSize :s, :fillColor :black}])))

(defn normal-comments [props]
	(let [heading "General Comments"
				doc (props :doc)
				is-topsoe (props :is-topsoe)
				comments (props :comments)
				general-comments (remove nil? (map (fn [d]
																						 (let [chart-info (:chart-info d)
																									 chart-id (if chart-info
																																(:chart-id chart-info)
																																)
																									 chart-name (if chart-id
																																(:name chart-id)
																																)
																									 chart (= chart-name
																														 "Select Chart" )]
																							 (if (or (not (:chart-info d))
																											 chart)
																								 d)
																							 )

																						 ) comments))]
		(if (> (count general-comments) 0)
			[:group (into [
										 [:add-page {:addPage nil}]
										 [:page {:fontSize :el, :fillColor :blue}]
										 [:text {:doc   (props :doc)
														 :text  heading
														 :w     523
														 :align "left"
														 }]
										 [:line {:coordinates  [[40 (+ 58 (i/oget doc :y))]
																						[555 (+ 58 (i/oget doc :y))]],
														 :stroke-width 1, :stroke-color (:blue style)}]
										 [:moveDown {:doc doc :liens 0.5}]]
										(chart-commetns doc general-comments is-topsoe ))])))


(defn chart [doc props is-topsoe]
	(reduce-kv (fn [col index chart-data]
							 (-> col
									 (conj [:page {:fontSize :n, :fillColor :black}])
									 (conj [:text {:doc        (props :doc)
																 :text       (str (chart-data :name) " ")
																 :w          523
																 :align      "left"
																 :with-image true
																 :font-size  :l
																 :color      :blue
																 :x          40
																 }])
									 (conj [:image
													{:image (chart-data :image),
													 :x     50
													 :w     490
													 :h     250}])
									 (conj [:moveDown {:doc doc :liens 2}])
									 (into (chart-commetns doc (chart-data :comments) is-topsoe))
									 (conj [:moveDown {:doc doc :liens 2}]))) []
						 (props :data)))

(defn section-page [props]
	(let [doc (props :doc)
				is-topsoe (props :is-topsoe)]
		[:group
		 (reduce (fn [col section-data]
							 (-> col
									 (conj [:add-page {:addPage nil}])
									 (conj [:page {:fontSize :el, :fillColor :blue}])
									 (conj [:text {:doc   (props :doc)
																 :text  (section-data :section)
																 :w     523
																 :align "left"
																 }])
									 (conj [:line {:coordinates  [[40 (+ 58 (i/oget doc :y))]
																								[555 (+ 58 (i/oget doc :y))]],
																 :stroke-width 1, :stroke-color (:blue style)}])
									 (conj [:moveDown {:doc doc :liens 0.5}])
									 (into (chart doc section-data is-topsoe))
									 )) [] (props :sections))]))


(defn page-template [props doc]
	(let [date (js/Date.)
				client-name (props :client-name)]
		(i/ocall (document doc) :image
						 (:watermark-hexagon base64) 20 482
						 #js{:width 590})
		(i/ocall (document doc) :image
						 (:ht-logo base64) 435 40
						 #js{:width 120})
		(i/ocall (document doc) :fontSize (get style :s))
		(i/ocall (document doc) :fillColor (get style :grey))
		(i/ocall (document doc) :text (str client-name
																			 ", " (format-date date))
						 40 70, #js{:width 532, :align "left"})
		(add-line {:coordinates  [[40 80] [555 80]], :stroke-width 1,
							 :stroke-color (:grey style)})
		(i/ocall (document doc) :fontSize (get style :n))
		(i/ocall (document doc) :fillColor (get style :black))
		(i/ocall (document doc) :moveDown 1)))

(defn download [props]
	(let [doc (as-> (js/PDFDocument. #js{
																			 :bufferPages   true
																			 :autoFirstPage true
																			 :margins       (:margins (paper))
																			 :size          (:size (paper))}) doc
									(i/ocall doc :on "pageAdded"
													 (fn []
														 (page-template props doc)
														 ))
									)
				stream (i/ocall doc :pipe (js/blobStream))

				summary-props {:doc doc, :client-name (props :client-name) :summary
														(props :summary)}
				rest-page {:doc doc, :client-name (props :client-name)}
				report-data (generate-report-data props doc)
				comments (props :comments)
				schema [
								(summary summary-props)
								(section-page report-data)
								#_(normal-comments {:comments comments :doc doc
																	:is-topsoe (props :is-topsoe)} )
								[:add-page-numbers {:add-page-numbers nil}]
								[:end {:end nil}]]]
		;; ;;Save the file
		(i/ocall stream :on "finish"
						 (fn []
							 (c/save-as (i/ocall stream :toBlob "application/pdf")
													"report.pdf")
							 (rf/dispatch [::ht-event/set-busy? false])
							 ))

		(swap! pdf assoc :doc doc)
		(draw-schema doc schema)
		nil))


























