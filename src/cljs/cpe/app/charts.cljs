(ns cpe.app.charts
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [cljs-react-material-ui.reagent :as ui]
            [ht.style :as hts]
            [stylefy.core :as stylefy]
            [ht.util.interop :as i]
            [cpe.app.icon :as ic]
            [cljsjs.filesaverjs]
            [ht.util.common :refer [tooltip-pos save-svg-to-file]]
            [cpe.app.d3 :refer [d3-svg d3-svg-2-string]]))

;; CONSTANTS
(defonce dl-w 160)
(defonce dl-w-half (/ dl-w 2))
(defonce y-t-line-h 15)

;; STYLES

(def s-ch-container
  {:user-select "none"
   :-ms-user-select "none"
   :-moz-user-select "none"
   :position "relative"
   :display "inline-block"})

(defn- s-ch-loader [left top]
  {:position "absolute"
   :top (str top "px")
   :left (str left "px")})

(def s-ch-popup
  {:position "absolute"
   :width (str dl-w "px")
   :color (hts/colors :brown)
   :background-color (hts/color-rgba :monet-pink 90 0.95)
   :border "1px solid"
   :border-radius "3px"
   :padding "4px 8px 4px 8px"
   :font-size "10px"
   :z-index 1000
   :transform "translate(-50%, -100%)"})

(defn- s-button [active?]
  (let [bg-color (-> :monet-pink hts/colors (hts/lighten 90))
        bg-color-a (-> :royal-blue hts/colors)
        color (-> :royal-blue hts/colors)]
    {:font-size   "11px"
     :display           "block"
     :background-color (if active? bg-color-a bg-color)
     :color            (if active? "white" color)
     :padding          "8px 18px 8px 10px"
     :overflow        "hidden"
     :float           "right"
     :max-width       "10px"
     :border-radius   "2px"
     :margin-bottom "1px"
     :width             "auto"
     :transition       "max-width 0.5s"
     :white-space      "nowrap"
     ::stylefy/mode {:hover {:max-width "100px"
                             :background-color bg-color-a
                             :color "white"}}}))

(def s-toolsbtn
  {:display           "block"
   :background-color (-> :monet-pink hts/colors (hts/lighten 75))
   :padding          "6px 21px 6px 7px"
   :color           (-> :royal-blue hts/colors)
   :float           "right"
   :max-width       "10px"
   :border-radius   "0px"})

(def s-ch-toolbar
  {:border-radius "2px"
   :cursor           "pointer"
   :vertical-align   "center", :position "absolute", :font-size "10px"
   :right            0, :margin "2px"})

(stylefy/tag "#toolbtns" {:display "none"})
(stylefy/tag "#tools:hover > #toolbtns" {:display "block"})

;; HELPERS

;; Seq or vector?
(defn- seq-or-vector? [d]
  (or (seq? d) (vector? d)))

;; d3-scaling
(defn- d3-scale [domain range]
  (-> js.d3
      (i/ocall :scaleLinear)
      (i/ocall :domain (to-array domain))
      (i/ocall :range (to-array range))))

;; d3-tick values
(defn- d3-ticks
  ([scale no-of-ticks] (vec (i/ocall scale :ticks no-of-ticks)))
  ([scale] (d3-ticks scale 10)))

;; absolute value
(defn- abs [n] (max n (- n)))

(defn- round-off [num n]
  (let [f (i/ocall js/Math :pow 10 n)
        nnum (* f num)]
    (/ (i/ocall js/Math :round nnum) f)))

(defn- get-log10 [num]
  (/ (i/ocall js/Math :log num) (i/ocall js/Math :log 10)))

(defn- round-off-sig [num]
  (if (= 0 num) num
      (let [anum (abs num)
            l (- (get-log10 anum) 2)
            n (->> l (min 0) (abs) (i/ocall js/Math :round))]
        (round-off num n))))

;; get mouse relative position
(defn- mouse-pos [event bounds]
  (let [ev (or event js/window.event)
        rect-pos {:x (.-left bounds)
                  :y (.-top bounds)}
        pos (if (.-pageX ev)
              {:x (+ (.-pageX ev) js/window.pageXOffset)
               :y (+ (.-pageY ev) js/window.pageYOffset)}
              (if (.-clientX ev)
                {:x (+ (.-clientX ev) js/document.body.scrollLeft)
                 :y (+ (.-clientY ev) js/document.body.scrollTop)}))]
    (merge-with - pos rect-pos)))

;; scale series
(defn- scale-series
  [series x-scale y-scale px py filter-fn]
  (map #(update % :data
                (fn [data]
                  (filter filter-fn (map (fn [{:keys [x y]}]
                                           {:x (+ px (x-scale x))
                                            :y (+ py (y-scale y))
                                            :actual {:x x :y y}})
                                         data))))
       series))

;; find in series
(defn- find-in-series [series cx cy]
  (some (fn [s]
          (some #(if (and (-> % :x (- cx) abs (< 2))
                          (-> % :y (- cy) abs (< 2)))
                   (assoc % :desc (:sensor-desc s)) nil)
                (:data s))) series))

;; draw marker
(defn- draw-marker [type ctx x y color]
  (let [angle (* 2 js/Math.PI)]
    (.beginPath ctx)
    (case type
      :circle (.arc ctx x y 2 0 angle)
      :square (.rect ctx (- x 2) (- y 2) 3 3)
      :triangle (do (.moveTo ctx (- x 2) (+ y 2))
                    (.lineTo ctx x (- y 2))
                    (.lineTo ctx (+ x 2) (+ y 2))
                    (.closePath ctx))
      nil)
    (set! (.-fillStyle ctx) color)
    (.fill ctx)
    (set! (.-strokeStyle ctx) (hts/color-hex (keyword "amber") -50))
    ;remove border line of marker
    #_(.stroke ctx)))

;; draw series
(defn- draw-series
  ([series ctx]
   (doseq [s series]
     (let [{:keys [data color shape]} s]
       (doseq [{x :x, y :y} data]
         (draw-marker shape, ctx, x, y, color)))))
  ([series ctx x-off y-off]
   (doseq [s series]
     (let [{:keys [data color shape]} s]
       (doseq [{x :x, y :y} data]
         (draw-marker shape, ctx, (+ x-off x), (+ y-off y), color))))))

;; split text
(defn split-text [chars text]
  (if (<= (count text) chars)
    [text]
    (let [substr (.substr text 0 chars)
          lastblank (.lastIndexOf substr " ")]
      (if (< lastblank 0)
        [substr (.trim (.substr text chars))]
        [(.substr text 0 lastblank)
         (.trim (.substr text lastblank))]))))


;; CHART LAYOUT

;; create layout
(defn- chart-node
  [{:keys [height width x-as x-ae y-as y-ae]}]
  (let [xe (- width x-ae)
        ys (- height y-as)
        p-h (- ys y-ae)
        p-w (- xe x-as)
        ch-p-color (hts/colors :royal-blue)]
    {:tag  :g :class :root
     :attr {:fill "none", :stroke "none", :style "background-color: white"}
     :nodes
     [; defs
      {:tag   :defs :class :defs :data :plot
       :nodes [{:tag   :clipPath :class :clipPath
                :attr  {:id "plotview"}
                :nodes [{:tag  :rect :class :rectview
                         :attr {:x      x-as, :y y-ae
                                :height p-h, :width p-w}}]}
               {:tag :circle :class :sym-circle
                :attr {:cx 4 :cy -4 :r 4 :id "circle"}}
               {:tag :rect :class :sym-rect
                :attr {:x 0 :y -8 :height 8 :width 8 :id "square"}}
               {:tag :polygon :class :sym-triangle
                :attr {:x 0 :y 0 :points "0,0 8,0 4,-8" :id "triangle"}}]}

      ; background
      {:tag :rect, :class :background
       :attr {:height "100%" :width "100%" :fill "white"}}

        ; chart title
      {:tag  :text, :class :title, :text :title
       :attr {:x           (/ width 2) :y 25 :fill ch-p-color
              :style       "font-size: 13px; font-weight: bold"
              :text-anchor "middle"}}

       ; legends
      {:tag :g, :class :lgroup, :data :legends, :multi? true
       :nodes [{:tag :use :class :legend-sym
                :attr {:x :x :y :y :fill :color
                       ;remove legend marker color
                       #_(:stroke (hts/color-hex (keyword "amber") -50))
                       :href #(str "#" (:shape %))}}
               {:tag :g :class :legend-text, :multi? true, :data :sensor-texts
                :nodes [{:tag :text, :text :text :class :legend
                         :attr {:x :x :y :y :fill ch-p-color
                                :style       "font-size: 11px;"}}]}]}


       ; chart area border
      #_{:tag  :rect, :class :ch-area
         :attr {:x            x-as, :y y-ae
                :height       (- ys y-ae), :width (- xe x-as)
                :stroke       (hts/color-rgba :sky-blue 50)
                :stroke-width "1px"}}

       ; x-axis bands

       ; y-axis bands

       ; x title
      {:tag  :text, :class :x-title, :text :x-title
       :attr {:x           (/ width 2) :y (- height 10) :fill ch-p-color
              :style       "font-size: 12px;"
              :text-anchor "middle"}}

       ; x-axis line
      {:tag  :line, :class :x-a-line
       :attr {:x1     x-as, :y1 ys, :x2 xe, :y2 ys
              :stroke ch-p-color
              :stroke-width 1}}

       ; x-axis ticks

       ; x-axis labels
      {:tag   :g, :class :x-a, :data :x-axis
       :nodes [{:tag  :text, :class :x-a-label, :multi? true
                :text :label
                :attr {:x           :x, :y (+ ys 18)
                       :text-anchor "middle"
                       :style       (str "font-size: 11px; fill:"
                                         ch-p-color)}}
               {:tag  :line, :class :x-a-label-tick, :multi? true
                :attr {:x1     :x, :y1 ys, :x2 :x, :y2 (+ ys 4)
                       :stroke ch-p-color}}]}

       ; x-axis gridlines
      
      ; y-axis reference lines
            {:tag :g :class :y-ref-lines :data :y-refs
             :nodes [{:tag  :line, :class :y-ref-lines :multi? true
                      :attr {:x1     x-as, :y1 :y, :x2 xe, :y2 :y
                             :stroke :color
                             :stroke-width 2}}]}

       ; y1 title

      {:tag :g, :class :y1-titles, :data :y1-title
       :nodes [{:tag  :text, :class :y1-title, :text (fn [%] %) :multi? true
                :attr {:x           #(+ 15 (* %2 y-t-line-h)), :y (+ y-ae (/ p-h 2))
                       :text-anchor "middle", :alignment-baseline "middle"
                       :transform   #(str "rotate(270, " (+ 15 (* %2 y-t-line-h)) ", " (+ y-ae (/ p-h 2)) ")")
                       :fill        ch-p-color :style "font-size: 12px;"}}]}
      
       ; y1-axis line
      {:tag  :line, :class :y1-a-line
       :attr {:x1     x-as, :y1 ys, :x2 x-as, :y2 y-ae
              :stroke ch-p-color}}

       ; y1-axis ticks

       ; y1-axis labels
      {:tag   :g, :class :y1-a, :data :y1-axis
       :nodes [{:tag  :text, :class :y1-a-label, :multi? true
                :text :label
                :attr {:x           (- x-as 8), :y :y
                       :text-anchor "end", :alignment-baseline "middle"
                       :style       (str "font-size: 11px; fill:"
                                         ch-p-color)}}
               {:tag  :line, :class :y1-a-label-tick, :multi? true
                :attr {:x1     x-as, :y1 :y, :x2 (- x-as 4), :y2 :y
                       :stroke ch-p-color}}]}


       ; y1-axis gridlines

       ; y2 title
      {:tag :g, :class :y2-titles, :data :y2-title, :skip? #(not (seq-or-vector? %))
       :nodes [{:tag  :text, :class :y2-title, :text (fn [%] %) :multi? true
                :attr {:x           #(- width 15 (* %2 y-t-line-h)), :y (+ y-ae (/ p-h 2))
                       :text-anchor "middle", :alignment-baseline "middle"
                       :transform   #(str "rotate(270, " (- width 15 (* %2 y-t-line-h)) ", " (+ y-ae (/ p-h 2)) ")")
                       :fill        ch-p-color :style "font-size: 12px;"}}]}


       ; y2-axis line
      {:tag  :line, :class :y2-a-line
       :attr {:x1         xe, :y1 ys, :x2 xe, :y2 y-ae
              :visibility #(if (:y2-axis %) "visible" "hidden")
              :stroke     ch-p-color}}

      ; y2-axis ticks

      ; y2-axis labels
      {:tag   :g, :class :y2-a, :data :y2-axis
       :nodes [{:tag  :text, :class :y2-a-label, :multi? true
                :text :label
                :attr {:x           (+ xe 8), :y :y
                       :text-anchor "start" :alignment-baseline "middle"
                       :style       (str "font-size: 11px; fill:"
                                         ch-p-color)}}
               {:tag  :line, :class :y2-a-label-tick, :multi? true, :skip? #(not (seq? %))
                :attr {:x1     xe, :y1 :y, :x2 (+ xe 4), :y2 :y
                       :stroke ch-p-color}}]}]}))

(defn- chart-layout [height width node]
  {:width    width, :height height
   :view-box (str "0 0 " width " " height)
   :style    {:color "white", :font-size "32px"}
   :node node})

;; COMPONENTS

;; Toolbar btn
(defn tb-button [{:keys [icon-class text on-click active? rotate?]}]
  [:div
   [:div (assoc (stylefy/use-style (s-button active?))
                :onClick on-click)
    [:span (merge-with str {:class (str "fa " icon-class)}
                       {:style {:margin-right 12
                                :transform (if rotate?
                                             "rotate(45deg)" "")}})] text]
   [:div {:style {:clear "both"}}]])

;; Toolbar
(defn toolbar
  [{:keys [zoom, pan, fullscreen?, pinned?]}
   {:keys [on-zoom-clicked, on-pan-clicked, on-reset-clicked
           on-show-fullscreen, on-hide-fullscreen, on-export-clicked
           on-un-pin-chart, on-pin-chart]}]
  [:div
   [:div#tools (merge
                (stylefy/use-style s-ch-toolbar)
                {:style {:right 0 :top 0}})
    [:div#settingsbtn
     [:div (stylefy/use-style s-toolsbtn)
      [:span (merge-with str {:class (str "fa " "fa-gears")}
                         {:style {:margin-right 8
                                  :font-size "16px"}})]]
     [:div {:style {:clear "both"}}]]
    [:div#toolbtns
     (if fullscreen? [tb-button {:icon-class "fa-search-plus"
                                 :text "Zoom"
                                 :on-click on-zoom-clicked
                                 :active? zoom}])
     (if fullscreen? [tb-button {:icon-class "fa-hand-paper-o"
                                 :text "Pan"
                                 :on-click on-pan-clicked
                                 :active? pan}])
     (if fullscreen? [tb-button {:icon-class "fa-undo"
                                 :text "Reset"
                                 :on-click on-reset-clicked
                                 :active? false}])

     [tb-button {:icon-class "fa-camera"
                 :text "Snapshot"
                 :on-click on-export-clicked
                 :active? false}]

     (if fullscreen?
       [tb-button {:icon-class "fa-window-close-o"
                   :text "Close"
                   :on-click on-hide-fullscreen
                   :active? false}]
       [tb-button {:icon-class "fa-arrows-alt"
                   :text "Full screen"
                   :on-click on-show-fullscreen
                   :active? false}])
     (if (not fullscreen?)
       (if pinned?
         [tb-button {:icon-class "fa-thumb-tack"
                     :text "Unpin"
                     :on-click on-un-pin-chart
                     :active? false
                     :rotate? true}]
         [tb-button {:icon-class "fa-thumb-tack"
                     :text "Pin"
                     :on-click on-pin-chart
                     :active? false}]))]]])

;; Data label
(defn data-label [state]
  (if-let [{:keys [x y actual desc]} (:label @state)]
    [:div (assoc (stylefy/use-style s-ch-popup)
                 :style {:left (str x "px")
                         :top (str (- y 3) "px")})
     [:div [:span {:style {:font-weight "bold"}} "Runday: "] (:x actual)]
     [:div [:span {:style {:font-weight "bold"}} (str desc ": ")] (:y actual)]]))

;; Selected area
(defn selected-area [state]
  (if-let [{:keys [x y height width]} (:selected-area @state)]
    [:div {:style {:position "absolute"
                   :border "1px solid"
                   :border-style "dotted"
                   :border-color (hts/color-rgba :brown 50 0.5)
                   :background-color (hts/color-rgba :brown 50 0.05)
                   :left x
                   :top y
                   :height height
                   :width width}}]))

;; Canvas chart
(defn canvas-chart [{:keys [width height y1 y2]}]
  (let [state (r/atom {})
        draw-chart (fn [ctx y1 y2]
                     (.clearRect ctx 0, 0, width, height)
                     (draw-series y1 ctx)
                     (if y2 (draw-series y2 ctx)))]
    (r/create-class

     {:component-did-mount
      (fn [this]
        (swap! state assoc :ctx (.getContext
                                 (dom/dom-node this) "2d"))
        (draw-chart (:ctx @state) y1 y2))

      :component-did-update
      (fn [this _]
        (let [{:keys [y1 y2]} (r/props this)]
          (draw-chart (:ctx @state) y1 y2)))

      :reagent-render
      (fn [{:keys [top left width height]}]
        [:canvas {:width width :height height
                  :style {:position "absolute" :top top :left left}}])})))

;; CPE chart

(defn- make-calculations
  [state width height y1-title y2-title x-domain
   y2-axis? y1-domain y2-domain y1-series y2-series]
  (let [x-char-l (* 0.073 width) ; adjust factor - width to no of chars
        legends
        (map-indexed #(let [x (+ 40 (* (/ width 2) (mod %1 2)))
                            y (+ 50 (* 25 (quot %1 2)))
                            {:keys [color shape]} %2]
                        {:x x, :y y, :color color, :shape (name shape)
                         :sensor-texts
                         (map-indexed (fn [i t]
                                        {:x (+ 20 x) :y (+ y (* 12 i)) :text t})
                                      (split-text x-char-l
                                                  (:sensor-desc %2)))})
                     (concat y1-series y2-series))
                         ;y-axis stuff
        y-char-l (* 0.08 height) ; adjust factor - width to no of chars
        y-char-w 6
        y-as 45, y-ae 110
        ch-area-h (- height y-ae y-as)
        ya-filter #(<= 0 (:y %) ch-area-h)
        plot-h (get-in state [:plot :plot-h] ch-area-h)
        y-range [plot-h 0]
        y1-scale (d3-scale y1-domain y-range)
        y2-scale (if y2-axis? (d3-scale y2-domain y-range))
        y1-ticks (d3-ticks y1-scale
                           (* 5 (get-in state [:zoom-factor :y] 1)))
        y2-ticks (if y2-axis?
                   (d3-ticks y2-scale
                             (* 5 (get-in state [:zoom-factor :y] 1))))
        y1-chars (apply max (map #(count (str % "")) y1-ticks))
        y2-chars (apply max (map #(count (str % "")) y2-ticks))
        y1-titles (split-text y-char-l y1-title)
        y2-titles (if y2-axis? (reverse (split-text y-char-l y2-title)))
       
                         ;x-axis stuff
        x-as (+ 25 (* y-t-line-h (count y1-titles)) (* y-char-w y1-chars))
        x-ae (if y2-axis? (+ 25 (* y-t-line-h (count y2-titles)) (* y-char-w y2-chars)) 20)
       
        xdlul (- width dl-w-half)
        ch-area-w (- width x-ae x-as)
        xa-filter #(<= 0 (:x %) ch-area-w)
        xya-filter #(and (xa-filter %) (ya-filter %))
        {:keys [plot-w]} (or (:plot state)
                             {:plot-w ch-area-w
                              :plot-h ch-area-h})
        x-range [0 plot-w]
        x-scale (d3-scale x-domain x-range)
        x-ticks (d3-ticks x-scale
                          (* 10 (get-in state [:zoom-factor :x] 1)))
       
       
        {px :x, py :y} (get state :pan-pos 0)
       
        x-axis (->> x-ticks
                    (map (fn [x] {:x (+ px x-as (x-scale x)) :label x}))
                    (filter #(<= x-as (:x %) (- width x-ae))))
        y1-axis (->> y1-ticks
                     (map (fn [y] {:y (+ py y-ae (y1-scale y)) :label y}))
                     (filter #(<= y-ae (:y %) (- height y-as))))
        y2-axis (if y2-axis?
                  (->> y2-ticks
                       (map (fn [y] {:y (+ py y-ae (y2-scale y)) :label y}))
                       (filter #(<= y-ae (:y %) (- height y-as)))))
        
        y1-refs (filter #(and (:y %) (<= y-ae (:y %) (- height y-as))) 
                        (map (fn [{:keys [rfq color]}]
                       (let [y-ref (if rfq (js/Number rfq))
                             y-sc (if y-ref (+ py y-ae 
                                               (y1-scale (js/Number rfq))))]
                         {:y y-sc, :color color}))
                     y1-series))
        
        y2-refs (filter #(and (:y %) (<= y-ae (:y %) (- height y-as)))
                        (map (fn [{:keys [rfq color]}]
                               (let [y-ref (if rfq (js/Number rfq))
                                     y-sc (if y-ref (+ py y-ae
                                                       (y2-scale (js/Number rfq))))]
                                 {:y y-sc, :color color}))
                             y2-series))
        
        y-refs (concat y1-refs y2-refs)
        
        scaled-y1-series (scale-series y1-series x-scale y1-scale px py xya-filter)
        scaled-y2-series (scale-series y2-series x-scale y2-scale px py xya-filter)]
    {:x-ae x-ae, :x-as x-as, :y-ae y-ae, :y-as y-as
     :xdlul xdlul, :ch-area-w ch-area-w, :ch-area-h ch-area-h
     :legends legends, :y1-titles y1-titles, :y2-titles y2-titles
     :x-axis x-axis, :y1-axis y1-axis, :y2-axis y2-axis, :y-refs y-refs
     :scaled-y1-series scaled-y1-series, :scaled-y2-series scaled-y2-series}))

(defn- get-canvas-image
  [width height x-as y-ae scaled-y1-series scaled-y2-series node data]
  (let [svg-string (d3-svg-2-string {:width width
                                     :height height
                                     :view-box (str "0 0 " width " " height)
                                     :preserveAspectRatio true
                                     :style {:font-family "verdana"}
                                     :class :svg-class
                                     :node node
                                     :data data})
        canvas (js/document.createElement "canvas")]
    (set! (.-width canvas) (* 2 width))
    (set! (.-height canvas) (* 2 height))
    (let [ctx (.getContext canvas "2d")]
      (.scale ctx 2 2)
      (set! (.-fillStyle ctx) "white")
      (.fillRect ctx 0 0 width height)
      (js/canvg canvas svg-string, #js {:ignoreMouse true
                                        :ignoreAnimation true
                                        :ignoreDimensions true})
      (draw-series scaled-y1-series ctx x-as y-ae)
      (if scaled-y2-series
        (draw-series scaled-y2-series ctx x-as y-ae))
      canvas)))

(defn get-chart-blob [width height title x-title y1-title y2-title x-domain
                      y2-axis? y1-domain y2-domain y1-series y2-series]
  (let [{:keys [x-ae x-as y-ae y-as legends
                y1-titles y2-titles x-axis y1-axis y2-axis
                y-refs scaled-y1-series scaled-y2-series]}
        (make-calculations nil width height y1-title y2-title x-domain
                           y2-axis? y1-domain y2-domain y1-series y2-series)
        node (chart-node {:height height :width width
                          :x-as x-as, :x-ae x-ae, :y-as y-as, :y-ae y-ae})

        data {:title    title
              :legends  legends
              :x-title  x-title
              :y1-title y1-titles
              :y2-title (if y2-axis? y2-titles)
              :x-axis   x-axis
              :y1-axis  y1-axis
              :y-refs y-refs
              :y2-axis  (if y2-axis? y2-axis)}

        canvas (get-canvas-image width height x-as y-ae
                                 scaled-y1-series scaled-y2-series node data)]
    (.toDataURL canvas)))

(defn cpe-chart
  [{:keys [height width y2-axis? on-show-fullscreen on-hide-fullscreen
           pinned-chart? on-pin-chart on-un-pin-chart]}]

  (let
   [state (r/atom {:zoom-factor {:x 1 :y 1}
                   :pan-pos     {:x 0 :y 0}
                   :plot        nil})
    tb-state (r/atom {})
    label-state (r/atom {:timer nil, :label nil})
    temp-state (r/atom {:mousedown-pos nil
                        :selected-area nil})]

    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! temp-state assoc :this this))

      :reagent-render
      (fn [{:keys [title, x-title, y1-title, y2-title, full-chart? loading?
                   x-domain y1-domain y2-domain
                   y1-series y2-series]}]
        (let
         [{:keys [x-ae x-as y-ae y-as xdlul ch-area-w ch-area-h
                  legends y1-titles y2-titles x-axis y1-axis y2-axis
                  y-refs scaled-y1-series scaled-y2-series]}
          (make-calculations (select-keys @state [:plot :zoom-factor :pan-pos])
                             width height y1-title y2-title x-domain
                             y2-axis? y1-domain y2-domain y1-series y2-series)

          ;; toolbar events
          on-zoom-clicked
          (fn [e]
            (swap! tb-state update :zoom #(not %))
            (swap! tb-state assoc :pan false))

          on-pan-clicked
          (fn [e]
            (swap! tb-state update :pan #(not %))
            (swap! tb-state assoc :zoom false))

          on-reset-clicked
          (fn [e]
            (swap! state assoc
                   :pan-pos {:x 0 :y 0}
                   :plot {:plot-w (- width x-ae x-as)
                          :plot-h (- height y-ae y-as)}
                   :zoom-factor {:x 1 :y 1})
            (swap! temp-state assoc
                   :mousedown-pos nil
                   :last-pos nil))

          set-data-label-fn
          (fn [cx cy]
            #(if-let [data (find-in-series
                            (concat scaled-y1-series
                                    scaled-y2-series)
                            (- cx x-as) (- cy y-ae))]
               (swap! label-state assoc :label
                      (merge-with
                       (fn [%1 %2]
                         (min %1 (max dl-w-half %2)))
                       {:x xdlul}
                       (merge-with + {:x x-as :y y-ae}
                                   (update-in data [:actual :y] round-off-sig))))))

          ;; mouse events
          on-mouse-down
          (fn [ev]
            (swap! temp-state assoc
                   :mousedown-pos
                   (mouse-pos ev
                              (.getBoundingClientRect (dom/dom-node (:this @temp-state))))))

          on-mouse-move
          (fn [ev]
            (let [{cx :x cy :y}
                  (mouse-pos ev
                             (.getBoundingClientRect (dom/dom-node (:this @temp-state))))
                  {lx :x, ly :y} (:last-pos @temp-state)]
              (if-let [ip (:mousedown-pos @temp-state)]
                (let [{x0 :x, y0 :y} ip]
                  (if (:pan @tb-state)
                    (swap! state assoc :pan-pos
                           {:x (+ lx (- cx x0))
                            :y (+ ly (- cy y0))}))
                  (if (:zoom @tb-state)
                    (swap! temp-state assoc :selected-area
                           {:x (min cx x0)
                            :y (min cy y0)
                            :height (abs (- y0 cy))
                            :width (abs (- cx x0))})))
                (if (not (or (:zoom @tb-state) (:pan @tb-state)))
                  (do
                    (js/clearTimeout (:timer @label-state))
                    (swap! label-state assoc
                           :label nil
                           :timer (js/setTimeout (set-data-label-fn cx cy) 20)))))))

          on-mouse-up
          (fn [_]
            (if-let [{zx :x, zy :y, zh :height, zw :width}
                     (:selected-area @temp-state)]
              (let [{pw :plot-w ph :plot-h}
                    (or (:plot @state) {:plot-w (- width x-ae x-as)
                                        :plot-h (- height y-ae y-as)})
                    ; Don't allow zoom-in below 1%
                    xf (if (< (/ pw zw) 100) (/ ch-area-w zw)
                           (* 100 (/ ch-area-w pw)))
                    yf (if (< (/ ph zh) 100) (/ ch-area-h zh)
                           (* 100 (/ ch-area-h ph)))
                    nw (* pw xf), nh (* ph yf)
                    {lpx :x lpy :y} (get @temp-state :last-pos {:x 0 :y 0})]
                (if (and (> zh 0) (> zw 0))
                  (swap! state assoc
                         :plot {:plot-h nh, :plot-w nw}
                         :zoom-factor
                         (merge-with * (:zoom-factor @state) {:x xf :y yf})
                         :pan-pos
                         {:x (* -1 xf (- zx x-as lpx))
                          :y (* -1 yf (- zy y-ae lpy))}))))

            (swap! temp-state assoc
                   :last-pos (:pan-pos @state)
                   :mousedown-pos nil
                   :selected-area nil))

          on-mouse-leave
          (fn [_]
            (swap! temp-state assoc :mousedown-pos nil)
            (swap! temp-state assoc
                   :selected-area nil
                   :last-pos (:pan-pos @state)))

          node
          (chart-node {:height height :width width
                       :x-as x-as, :x-ae x-ae, :y-as y-as, :y-ae y-ae})

          config (chart-layout height width node)

          data {:title    title
                :legends  legends
                :x-title  x-title
                :y1-title y1-titles
                :y2-title (if y2-axis? y2-titles)
                :x-axis   x-axis
                :y1-axis  y1-axis
                :y2-axis  (if y2-axis? y2-axis)
                :y-refs   y-refs}

          on-export-clicked
          (fn [_]
            (let [canvas (get-canvas-image width height x-as y-ae
                                           scaled-y1-series scaled-y2-series
                                           node data)]
              (set! (.-canvasobj js/window) canvas)
              (.toBlob canvas #(js/saveAs % (str title ".png")))))]

          [:div (merge (stylefy/use-style s-ch-container)
                       {:onMouseMove on-mouse-move
                        :onMouseLeave on-mouse-leave
                        :onMouseDown on-mouse-down
                        :onMouseUp on-mouse-up})
           [selected-area temp-state]
           [d3-svg (assoc config :data data)]
           [canvas-chart {:left x-as, :top y-ae
                          :width ch-area-w
                          :height ch-area-h
                          :y1 scaled-y1-series
                          :y2 scaled-y2-series}]
           [data-label label-state]
           [toolbar (merge @tb-state {:fullscreen? full-chart?
                                      :pinned? pinned-chart?})
            {:on-zoom-clicked on-zoom-clicked
             :on-pan-clicked on-pan-clicked
             :on-reset-clicked on-reset-clicked
             :on-show-fullscreen on-show-fullscreen
             :on-hide-fullscreen on-hide-fullscreen
             :on-pin-chart on-pin-chart
             :on-export-clicked on-export-clicked
             :on-un-pin-chart on-un-pin-chart}]
           (if loading?
             [:div (stylefy/use-style (s-ch-loader (/ width 2) (/ height 2)))
              [ui/circular-progress]])]))})))