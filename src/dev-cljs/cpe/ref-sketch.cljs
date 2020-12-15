(ns cpe.ref-sketch
  (:require [cljs.core.async :refer [<! put!]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.util.common :as u :refer [dev-log]]
            [cpe.app.d3 :refer [d3-svg d3-svg-2-string]]
    ;[cpe.component.reformer-dwg.view :as dwg]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def reformer-data-tf
  (r/atom {:name          "Reformer",
           :version       4,
           :firing        "top",
           :date-modified "2018-01-04T04:49:22.949Z",
           :modified-by   "bhka@topsoe.com",
           :tf-config
           {:wall-names       {:north "North"
                               :east  "East"
                               :west  "West"
                               :south "South"}
            :burner-first?    true
            :tube-row-count   6,
            :tube-rows        (repeat 6 {:tube-count 10
                                         :start-tube 1
                                         :end-tube   10})
            :burner-row-count 7
            :burner-rows      (repeat 7 {:burner-count 5
                                         :start-burner 1
                                         :end-burner   5})
            :section-count    2,
            :sections         [{:tube-count   5
                                :burner-count 3}
                               {:tube-count   5
                                :burner-count 2}]
            :measure-levels   {:top?    true
                               :middle? true
                               :bottom? true}}}))


(def my-state (r/atom {:data {:classes "ab cd"
                              :name "abc"
                              :list [{:x 0, :y 0, :w 10, :h 10}
                                     {:x 10, :y 10, :w 10, :h 10}]}
                       :chart {:points []}}))

(def my-sketch
  {:width "200px", :height "300px"
   :view-box "0 0 200 300"
   :style {:color "white"
           :font-size "32px"}
   :node {:tag :g
          :attr {:fill "none"
                 :stroke "none"}
          :class :root
          :classes :classes
          :nodes [{:tag :rect, :class :back
                   :attr {:x 0, :y 0, :width 200, :height 300
                           :fill "aliceblue"}}
                  {:tag :text, :class :label
                   :attr {:x 20, :y 20, :fill "indigo"}
                   :text :name
                   :data #(select-keys % [:name])
                   :did-update #(js/console.log "updated label")}
                  {:tag :text, :class :sub-lable
                   :attr {:x 20, :y 50, :fill "indigo"}
                   :text :name
                   :data #(select-keys % [:name])
                   :skip? :name}
                  {:tag :rect, :class :child
                   :multi? true
                   :data [:list]
                   :attr {:x :x, :y :y, :width :w, :height :h
                          :fill "red"}
                   :on {:click #(js/console.log "click: " [js/d3.event.pageX
                                                           js/d3.event.pageY])}
                   :did-update #(js/console.log "updated rect")}]}})

(def my-chart
  {:width "300px", :height "200px"
   :view-box "0 0 300 200"
   :style {:color "white"
           :font-size "32px"}
   :node {:tag :g
          :attr {:fill "none", :stroke "none"}
          :class :root
          :nodes [{:tag :rect, :class :point
                   :attr {:width 3, :height 3
                          :x :x, :y :y
                          :fill "aliceblue"}
                   :multi? true
                   :data :points}]}})

(defn d3-sketch []
  #_[d3-svg (assoc my-sketch
                   :data (:data @my-state))]
  [d3-svg (assoc my-chart
                 :data (:chart @my-state))])

(defn save-image []
  (let [svg-string (d3-svg-2-string (-> my-sketch
                                        (assoc-in [:style :font-family] "open_sans")
                                        (assoc :data (:data @my-state))))]
    ;; (dev-log svg-string)
    #_(dev-log (str "data:image/svg+xml;base64,"
                  (js/btoa (js/unescape (js/encodeURIComponent svg-string)))))
    (go
      (let [res (<! (u/save-svg-to-file "my-sketch.png" svg-string 200 300 60))]
        (dev-log "save image status: " (name res))))))

(defn ref-sketch []
  [:div {:style {:padding "50px"
                 :background "lightblue"}}
   [d3-sketch {}]
   [ui/flat-button {:label "Save"
                    :on-click save-image}]
   #_[dwg/reformer-dwg {:width "600px" :height "500px"
                      :preserve-aspect-ratio "none"
                      :config @reformer-data-tf}]
   #_[ui/flat-button {:label "Save"
                    :on-click #(dwg/save-image {:config @reformer-data-tf})}]])

(defn gen-chart [n]
  (let [xi 0, xe 300
        yi 0, ye 200
        n (or n 100)
        x (random-sample 0.5 (range n))
        y (repeatedly (count x) #(rand-int ye))
        f (fn [i e x] (+ i (* x (/ (- e i) n))))]
    (mapv (fn [x y]
            {:x (f xi xe x)
             :y y})
          x y)))
