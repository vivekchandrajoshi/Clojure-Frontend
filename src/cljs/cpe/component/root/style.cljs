(ns cpe.component.root.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [cpe.app.style :as app-style]))

(def header
  (let [{h :head-row-height} ht/root-layout
        logo-h 18
        logo-s (/ (- h logo-h) 2)]
    {:background           (:blue-spot-light ht/gradients)
     :height               (px h)
     :display              "flex"
     :flex-direction       "row"
     ::stylefy/vendors     vendors
     ::stylefy/auto-prefix #{:flex-direction}
     ;; children styles
     ::stylefy/sub-styles
     {:left       {:background-image  "url('images/ht_logo_white.png')"
                   :height            (px logo-h)
                   :background-repeat "no-repeat"
                   :background-size   "contain"
                   :margin-top        (px logo-s)
                   :margin-left       (px logo-s)
                   :width             "200px"}
      :middle     {:flex                 1
                   ::stylefy/vendors     vendors
                   ::stylefy/auto-prefix #{:flex}}
      :right      {:font-size "12px"
                   :padding   "24px 15px 0 12px"}
      :link       {:text-decoration "none"
                   :cursor          "pointer"
                   :display         "inline-block"
                   :height          "24px"
                   :padding         "0 15px 0 0"
                   :margin-left     "15px"}
      :link-label {:color          (color :white)
                   :user-select    "none"
                   :font-size      "12px"
                   :line-height    "18px"
                   :display        "inline-block"
                   :vertical-align "top"}}}))

(def sub-header
  (let [{h :sub-head-row-height} ht/root-layout
        col {:display              "flex"
             :flex-direction       "row"
             ::stylefy/auto-prefix #{:flex-direction :flex}
             ::stylefy/vendors     vendors}]
    (merge col
           {:background-color (color :alumina-grey 50)
            :height           (px h)
            ;;sub-styles
            ::stylefy/sub-styles
            {:left   (assoc col :flex 3)
             :right  (assoc col :flex 1
                            :background-color (color :alumina-grey 30))
             :logo   {:min-width "310px"
                      :height       (px h)
                      :padding      (px (/ (- h 26) 2))
                      :padding-left "20px"
                      :color        (color :royal-blue)}
             :spacer (assoc col :flex 1)}})))

(defn hot-links [view-size]
  (let [w (:width view-size)
        {h :sub-head-row-height} ht/root-layout
        link {:text-decoration "none"
              :user-select     "none"
              :margin-left     "30px"
              ;:margin-left "10px"
              :margin-right "10px"
              :font-size       "12px"
              :display "inline-block"
              :color           (color :royal-blue)}]
    {:height  (px h)
     :padding (if (< w 1472)
                "0px"
                "9px 0 9px 0px")
     :background-color (color :alumina-grey 50)
     :border "#b8bbbb solid 1px"
     ;; children styles
     ::stylefy/sub-styles
     {:link        link
      :active-link (merge link {:font-weight 700})}}))

(def messages
  ;; TODO: define style for warning and comment
  {})

(def info
  (let [{h :sub-head-row-height} ht/root-layout]
    {:height               (px h)
     :padding              "8px 0px 10px 30px"
     :overflow             "hidden"
     :color                (color :slate-grey)
     :font-size            "12px"
     :flex                 1
     ::stylefy/auto-prefix #{:flex}
     ::stylefy/vendors     vendors
     ;; children styles
     ::stylefy/sub-styles
     {:p    {:margin 0}
      :head {:font-weight 300
             :user-select "none"
             :font-size   "10px"
             :display     "block"}
      :body {:line-height "12px"
             :display     "block"}}}))

(def no-access
  {:padding "10% 15%"
   ::stylefy/sub-styles
   {:p {:font-size   "18px"
        :font-weight 700
        :margin      0
        :color       (color :red)}}})

(def content {
              :background      "url('images/background.jpg')"
              :background-size "cover"
              :display         "inline-flex"
              :font-size       "12px"
              :width           "100%"
              :position        "absolute"
              })

(def root {:background-color (color :white)})

(defn left-container   [h w]
  {:margin "20px 0 20px 20px"
   :width  (- w (+ (/ w 4) 20))
   :height h})

(def download-report
  {:background (color :sky-blue)
   :border     "none "
   :cursor     "pointer"
   :font-size  "20px"
   :color      "#fff"
   :padding    "5px 0"
   :width      "70px"})

(def change-uom
  {:background (color :sky-blue)
   :border     "none "
   :cursor     "pointer"
   :font-size  "10px"
   :color      "#fff"
   :padding    "2px 5px 2px 5px"
   :margin     "0px 2px 0 0"
   :width      "70px"
   ;:border-radius "20px"
   ::stylefy/vendors     vendors
   ::stylefy/auto-prefix #{:flex-direction}
   ;; children styles
   ::stylefy/sub-styles
   {:link       {:text-decoration "none"
                 :cursor          "pointer"
                 :display         "inline-block"
                 :height          "12px"
                 :padding         "0 15px 0 0"
                 :margin-left     "15px"}
    :link-label {:color          (color :white)
                 :user-select    "none"
                 :font-size      "10px"
                 :line-height    "10px"
                 :display        "block"
                 :vertical-align "none"}}})
