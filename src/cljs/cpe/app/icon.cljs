(ns cpe.app.icon
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cpe.app.style :as style]))

(defn icon [props & children]
  (into [:svg (-> (merge {:view-box "0 0 24 24"} props)
                  (update :class str " ht-ic-icon"))]
        children))

(defn plant
  ([] (plant nil))
  ([props]
   [icon props
    [:path {:d "M1,1 h6 v14 l9,-8 v8 l7,-8 v16 h-22z"}]]))

(defn my-apps
  ([] (my-apps nil))
  ([props]
   [icon props
    [:path {:d "M3,3 h6 v6 h-6 z", :class "ht-ic-fill"}]
    [:path {:d "M15,3 h6 v6 h-6 z"}]
    [:path {:d "M3,15 h6 v6 h-6 z"}]
    [:path {:d "M15,15 h6 v6 h-6 z"}]]))

(defn logout
  ([] (logout nil))
  ([props]
   [icon props
    [:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "M9,7 a 6,6 0 1 0 6,0"}]
    [:path {:d "M12,13 v-8"}]]))

(defn camera
  ([] (camera nil))
  ([props]
   [icon props
    [:path {:d "M4,9 l1,-1 h14 l1,1 v8 l-1,1 h-14 l-1,-1 z"}]
    [:circle {:cx "12" :cy "13" :r "3"}]
    [:path {:d "M6,6 l2,-1 h2 l2,1"}]]))

(defn plus
  ([] (plus nil))
  ([props]
   [icon props
    [:path {:d "M12,5 v14"}]
    [:path {:d "M5,12 h14"}]]))

(defn minus
  ([] (minus nil))
  ([props]
   [icon props
    [:path {:d "M5,12 h14"}]]))

(defn dropdown
  ([] (dropdown nil))
  ([props]
   [icon props
    [:path {:d "M6,10 l6,6 l6,-6"}]]))

(defn nav-right
  ([] (nav-right nil))
  ([props]
   [icon props
    [:path {:d "M10,6 l6,6 l-6,6"}]]))

(defn nav-left
  ([] (nav-left nil))
  ([props]
   [icon props
    [:path {:d "M14,6 l-6,6 l6,6"}]]))

(defn pyrometer+
  ([] (pyrometer+ nil))
  ([props]
   [icon props
    [:path {:d "M4,6 h16 v4 l-14,3 z"}]
    [:path {:d "M9,13 l-2,6 m-1,0 h6 m-1,0 l2,-6"}]
    [:path {:d "M18,13 v6 m-3,-3 h6"}]]))

(defn emissivity+
  ([] (emissivity+ nil))
  ([props]
   [icon props
    [:path {:d "M12,9 c-3,-2 -5,-2 -6,1 c0,3 5,3 5,3 m0,0 c0,0 -5,0 -5,3 c0,3 4,3 7,0"}]
    [:path {:d "M18,10 v6 m-3,-3 h6"}]]))

(defn mark-tube
  ([] (mark-tube nil))
  ([props]
   [icon props
    [:path {:d "M2,6 c 2,-4 2,-2 7,0 c -3,3 -5,4 -7,0 v18 m7,-18 v5
m0,13 v-10 c 3,-3 3,-3 7,0 c-3,4 -3,4 -7,0 m7,0 v10
m6,0 v-22 c -2,8 -5,3 -8,0 c 3,-3 3,-3 8,0 m-8,0 v8"}]]))

(defn save
  ([] (save nil))
  ([props]
   [icon props
    [:path {:d "M12,6 v8 l-3,-3 M12,14 l3,-3"}]
    [:path {:d "M5,12 v6 h14 v-6"}]]))

(defn upload
  ([] (upload nil))
  ([props]
   [icon props
    [:path {:d "M12,6 v8 M12,6 l-3,3 M12,6 l3,3"}]
    [:path {:d "M5,12 v6 h14 v-6"}]]))

(defn cancel
  ([] (cancel nil))
  ([props]
   [icon props
    [:path {:d "M6,6 l12,12 m0,-12 l-12,12"}]]))

(defn accept
  ([] (accept nil))
  ([props]
   [icon props
    [:path {:d "M3,12 l6,6 l12-12"}]]))

(defn delete
  ([] (delete nil))
  ([props]
   [icon props
    [:path {:d "M6,7 h12.5 m-5,0 v-2 h-2.5 v2"
            :fill "currentColor"}]
    [:path {:d "M7.5,6 v11 l1,1 h8 l1,-1 v-11"}]
    [:path {:d "M10,9 v6 m2.5,0 v-6 m2.5,0 v6"}]]))

(defn dataset
  ([] (dataset nil))
  ([props]
   [icon props
    [:rect {:x 5, :y 6 :width 14, :height 12}]
    [:path {:d "M6,7 h12 m0,3.3 h-12 m0,3.3 h12 m0,3.4 h-12
m4,0 v-12 m4,0 v12"}]]))

(defn report
  ([] (dataset nil))
  ([props]
   [icon props
    [:path {:d "M7,9 h3 v-3 h7 v12 h-10 v-9 l3,-3"}]
    [:path {:d "M9,11 h6 m0,2 h-6 m0,2 h6"}]]))

(defn datasheet
  ([] (dataset nil))
  ([props]
   [icon props
    [:path {:d "M7,9 h3 v-3 h7 v12 h-10 v-9 l3,-3"}]
    [:path {:d "M10,11 l5,5 m0,-5 l-5,5"}]]))

(defn license
  ([] (license nil))
  ([props]
   [icon props
    [:path {:d "M3,3 h15 v3 M3,3 v18 h8"}]
    [:path {:d "M5,10 h5 m-5,3 h5 m-5,3 h5"}]
    [:circle {:cx "17" :cy "12" :r "5"}]
    [:path {:d "M17,12 m-3,4 l-1,7 l4,-3 l4,3 l-1,-7"}]]))

(defn menu
  ([] (menu nil))
  ([props]
   [icon props
    [:path {:d "M6,8 h12 m0,4 h-12 m0,4 h12"}]]))

(defn reset
  ([] (reset nil))
  ([props]
   [icon props
    [:path {:d "M6,9 a 7,7 0 1 1 0,6"}]
    [:path {:d "M6,3 v6 h6"}]]))

(defn pencil
  ([] (pencil nil))
  ([props]
   [icon props
    [:path {:d "M6,18 l2,-5 l8,-8 l3,3 l-8,8 z M8,13 l3,3"}]]))



;; more complex icons ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fixed view-box     ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn icon-complex [props view-box & children]
  (into [:svg (-> (assoc props :view-box view-box)
                  (update :class str " ht-ic-icon"))]
        children))

(defn gear
  ([] (gear nil))
  ([props]
   [icon-complex props "-64 -64 640 640"
    ;; original view-box "0 0 512 512" ;; source font-awesome - cog
    [:path {:d "M444.788 291.1l42.616 24.599c4.867 2.809 7.126 8.618 5.459 13.985-11.07 35.642-29.97 67.842-54.689 94.586a12.016 12.016 0 0 1-14.832 2.254l-42.584-24.595a191.577 191.577 0 0 1-60.759 35.13v49.182a12.01 12.01 0 0 1-9.377 11.718c-34.956 7.85-72.499 8.256-109.219.007-5.49-1.233-9.403-6.096-9.403-11.723v-49.184a191.555 191.555 0 0 1-60.759-35.13l-42.584 24.595a12.016 12.016 0 0 1-14.832-2.254c-24.718-26.744-43.619-58.944-54.689-94.586-1.667-5.366.592-11.175 5.459-13.985L67.212 291.1a193.48 193.48 0 0 1 0-70.199l-42.616-24.599c-4.867-2.809-7.126-8.618-5.459-13.985 11.07-35.642 29.97-67.842 54.689-94.586a12.016 12.016 0 0 1 14.832-2.254l42.584 24.595a191.577 191.577 0 0 1 60.759-35.13V25.759a12.01 12.01 0 0 1 9.377-11.718c34.956-7.85 72.499-8.256 109.219-.007 5.49 1.233 9.403 6.096 9.403 11.723v49.184a191.555 191.555 0 0 1 60.759 35.13l42.584-24.595a12.016 12.016 0 0 1 14.832 2.254c24.718 26.744 43.619 58.944 54.689 94.586 1.667 5.366-.592 11.175-5.459 13.985L444.788 220.9a193.485 193.485 0 0 1 0 70.2zM336 256c0-44.112-35.888-80-80-80s-80 35.888-80 80 35.888 80 80 80 80-35.888 80-80z"
            :stroke-width "25"}]]))

(defn magnify
  ([] (magnify nil))
  ([props]
   [icon-complex props "-64 -64 640 640"
    ;; original view-box "0 0 512 512" ;; source font-awesome - search
    [:path {:d "M505 442.7L405.3 343c-4.5-4.5-10.6-7-17-7H372c27.6-35.3 44-79.7 44-128C416 93.1 322.9 0 208 0S0 93.1 0 208s93.1 208 208 208c48.3 0 92.7-16.4 128-44v16.3c0 6.4 2.5 12.5 7 17l99.7 99.7c9.4 9.4 24.6 9.4 33.9 0l28.3-28.3 c9.4-9.4 9.4-24.6.1-34z"
            :stroke-width "25"}]]))

(defn star
  ([] (star nil))
  ([props]
   [icon-complex props "-10 -10 52 52"
    [:path {:d "M20.388,10.918L32,12.118l-8.735,7.749L25.914,
             31.4l-9.893-6.088L6.127,31.4l2.695-11.533L0,
             12.118l11.547-1.2L16.026,0.6L20.388,10.918z"
            :stroke-width "2"}]]))

(defn bell
  ([] (bell nil))
  ([props]
   [icon-complex props "-10 -10 52 52"
    [:path {:d "M16.286 30.286q0-0.286-0.286-0.286-1.054 0-1.813-0.759t-0.759-1.813q0-0.286-0.286-0.286t-0.286 0.286q0 1.304 0.92 2.223t2.223 0.92q0.286 0 0.286-0.286zM4.393 25.143h23.214q-4.75-5.357-4.75-14.857 0-0.911-0.429-1.875t-1.232-1.839-2.17-1.438-3.027-0.563-3.027 0.563-2.17 1.438-1.232 1.839-0.429 1.875q0 9.5-4.75 14.857zM30.857 25.143q0 0.929-0.679 1.607t-1.607 0.679h-8q0 1.893-1.339 3.232t-3.232 1.339-3.232-1.339-1.339-3.232h-8q-0.929 0-1.607-0.679t-0.679-1.607q0.893-0.75 1.625-1.571t1.518-2.134 1.33-2.83 0.893-3.679 0.348-4.643q0-2.714 2.089-5.045t5.482-2.83q-0.143-0.339-0.143-0.696 0-0.714 0.5-1.214t1.214-0.5 1.214 0.5 0.5 1.214q0 0.357-0.143 0.696 3.393 0.5 5.482 2.83t2.089 5.045q0 2.482 0.348 4.643t0.893 3.679 1.33 2.83 1.518 2.134 1.625 1.571z"
            :stroke-width "2"}]]))

(defn unsubscribe-bell
  ([] (unsubscribe-bell nil))
  ([props]
   [icon-complex props "-8 -10 52 52"
    [:path {:d "M18.571 30.286q0-0.286-0.286-0.286-1.054 0-1.813-0.759t-0.759-1.813q0-0.286-0.286-0.286t-0.286 0.286q0 1.304 0.92 2.223t2.223 0.92q0.286 0 0.286-0.286zM8.982 21.804l15.661-13.571q-0.75-1.571-2.366-2.616t-3.991-1.045q-1.661 0-3.027 0.563t-2.17 1.438-1.232 1.839-0.429 1.875q0 6.857-2.446 11.518zM33.143 25.143q0 0.929-0.679 1.607t-1.607 0.679h-8q0 1.893-1.339 3.232t-3.232 1.339-3.223-1.33-1.348-3.223l2.661-2.304h13.518q-2.964-3.339-4.054-8.196l1.982-1.732q1.089 6.357 5.321 9.929zM34.679 0.286l1.5 1.714q0.143 0.179 0.134 0.42t-0.188 0.402l-33.429 28.964q-0.179 0.143-0.42 0.125t-0.384-0.196l-1.5-1.714q-0.143-0.179-0.134-0.42t0.188-0.384l3.321-2.875q-0.339-0.571-0.339-1.179 0.893-0.75 1.625-1.571t1.518-2.134 1.33-2.83 0.893-3.679 0.348-4.643q0-2.714 2.089-5.045t5.482-2.83q-0.143-0.339-0.143-0.696 0-0.714 0.5-1.214t1.214-0.5 1.214 0.5 0.5 1.214q0 0.357-0.143 0.696 2.214 0.321 3.911 1.473t2.643 2.813l7.464-6.482q0.179-0.143 0.42-0.125t0.384 0.196z"
            :stroke-width "2"}]]))
(defn balance-scale
  ([] (balance-scale nil))
  ([props]
   [icon-complex props "-6 -10 52 52"
    [:path {:d "M30.857 8l-6.857 12.571h13.714zM8 8l-6.857 12.571h13.714zM22.661 4.571q-0.25 0.714-0.813 1.277t-1.277 0.813v23.054h10.857q0.25 0 0.411 0.161t0.161 0.411v1.143q0 0.25-0.161 0.411t-0.411 0.161h-24q-0.25 0-0.411-0.161t-0.161-0.411v-1.143q0-0.25 0.161-0.411t0.411-0.161h10.857v-23.054q-0.714-0.25-1.277-0.813t-0.813-1.277h-8.768q-0.25 0-0.411-0.161t-0.161-0.411v-1.143q0-0.25 0.161-0.411t0.411-0.161h8.768q0.375-1.018 1.25-1.652t1.982-0.634 1.982 0.634 1.25 1.652h8.768q0.25 0 0.411 0.161t0.161 0.411v1.143q0 0.25-0.161 0.411t-0.411 0.161h-8.768zM19.429 4.857q0.589 0 1.009-0.42t0.42-1.009-0.42-1.009-1.009-0.42-1.009 0.42-0.42 1.009 0.42 1.009 1.009 0.42zM38.857 20.571q0 1.304-0.83 2.339t-2.098 1.625-2.58 0.884-2.491 0.295-2.491-0.295-2.58-0.884-2.098-1.625-0.83-2.339q0-0.196 0.625-1.446t1.643-3.116 1.911-3.491 1.821-3.286 1-1.786q0.321-0.589 1-0.589t1 0.589q0.071 0.125 1 1.786t1.821 3.286 1.911 3.491 1.643 3.116 0.625 1.446zM16 20.571q0 1.304-0.83 2.339t-2.098 1.625-2.58 0.884-2.491 0.295-2.491-0.295-2.58-0.884-2.098-1.625-0.83-2.339q0-0.196 0.625-1.446t1.643-3.116 1.911-3.491 1.821-3.286 1-1.786q0.321-0.589 1-0.589t1 0.589q0.071 0.125 1 1.786t1.821 3.286 1.911 3.491 1.643 3.116 0.625 1.446z"
            :stroke-width "2"}]]))

