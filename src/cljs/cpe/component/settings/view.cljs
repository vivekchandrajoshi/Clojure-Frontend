;; view elements component settings
(ns cpe.component.settings.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [cpe.app.view :as app-view]
            [cpe.app.style :as app-style]
            [cpe.app.subs :as app-subs]
            [cpe.component.section.subs :as section]
            [cpe.app.event :as app-event]
            [cpe.app.scroll :as scroll]
            [cpe.app.comp :as app-comp]
            [ht.util.interop :as i]
            [cpe.app.icon :as ic]
            [cpe.component.root.event :as root-event]
            [cpe.component.settings.style :as style]
            [cpe.component.settings.subs :as subs]
            [cpe.component.settings.event :as event]
            [cpe.component.root.subs :as root-subs]))

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

(defn toggle [{:keys [disabled? value on-toggle]}]
  (let [on? value
        style (style/toggle on? disabled?)]
    [:span (-> (use-style style)
               (assoc :on-click
                      (if-not disabled?
                        #(on-toggle (not on?)))))
     [:div (use-sub-style style :main)
      [:span (use-sub-style style :label)
       (if on?
         (translate [:ht-comp :toggle :on] "on")
         (translate [:ht-comp :toggle :off] "off"))]
      [:div (use-sub-style style :circle)]]]))

;; context menu to be shown in settings menu
(defn context-menu []
  (let [topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
    (vec (cond-> [{:id       :uom
                   :icon     (as-left-icon ic/dataset)
                   :label-fn #(translate [:root :menu :my-apps] "UOM")
                   :event-id ::app-event/open-uom}]
                 topsoe? (conj {:id       :report-settings
                                :icon     (as-left-icon ic/gear)
                                :label-fn #(translate [:root :menu :my-apps] "Report settings")
                                :event-id ::app-event/open-report-settings})))))

(defn no-config [{:keys [width height]}]
  [:div {:style {:width width, :height height}}
   [:div (use-style style/no-config)
    "Missing configuration!"]])

(defn show-error? [] @(rf/subscribe [::subs/show-error?]))

;; *x48
(defn text-input [{:keys [read-only? valid? align width value on-change]
                   :or {valid? true, width 96, align "left"}}]
  (let [style (style/text-input read-only? valid?)]
    [:span (use-style style)
     [:input (-> (use-sub-style style :main)
                 (update :style assoc
                         :width width
                         :text-align align)
                 (merge {:type "text"
                         :value (or value "")
                         :on-change #(on-change (i/oget-in % [:target :value]))
                         :read-only read-only?}))]]))

(defn chart-config-row [w bg-index s as c chart-data p disable-min-max]
  [:div (use-style (style/chart-config-row bg-index w))
   [:div (use-sub-style (style/chart-config-row bg-index w) :left) s]
   [:div (use-sub-style (style/chart-config-row bg-index w) :left) (:name c)]
   [:div (use-sub-style (style/chart-config-row-fixed bg-index w) :left)
    (if (or (= p true) (= p false) (= p nil))
      [toggle
       {:value p :on-toggle #(rf/dispatch [::event/update-pinned-charts (:id c)
                                           %])}]
      p)]
   [:div (use-sub-style (style/chart-config-row bg-index w) :left)
    (:description as)]

   [:div (use-sub-style (style/chart-config-row-fixed bg-index w) :left)
    (if @(rf/subscribe [::section/get-axis-lable-uom (:name as)])
      @(rf/subscribe [::section/get-axis-lable-uom (:name as)])
      "Null")
    ]
   [:div (use-sub-style (style/chart-config-row bg-index w) :middle)
    [text-input {:value     (:min chart-data)
                 :valid?    true
                 :on-change #(rf/dispatch [::event/set-chart-config
                                           (:name as) :min
                                           %])
                 :read-only? disable-min-max}]]
   [:div (use-sub-style (style/chart-config-row bg-index w) :middle)
    [text-input {:value     (:max chart-data)
                 :valid?    true
                 :on-change #(rf/dispatch [::event/set-chart-config
                                           (:name as) :max
                                           %])
                 :read-only? disable-min-max}]]
   [:div (use-sub-style (style/chart-config-row bg-index w) :right)
    [text-input {:value     (:rfq chart-data)
                 :valid?    true
                 :on-change #(rf/dispatch [::event/set-chart-config
                                           (:name as) :rfq %])}]]])

(defn chart-config [{:keys [width height]}]
  (let [style (style/chart-config  width  (- height 30))
        section-list @(rf/subscribe [::root-subs/section-list])
        chart @(rf/subscribe [::subs/chart])
        sensor @(rf/subscribe [::subs/sensor])
        charts-config @(rf/subscribe [::subs/field [:charts-config :value]])
        pinned-charts @(rf/subscribe [::subs/field [:pinned-charts :value]])]
    [:div (use-style style)
     [scroll/scroll-box (use-sub-style style :form-scroll)
      [:div  (use-sub-style style :form-scroll)
       ;;Drawing the table
       [:div (use-sub-style style :table)
        ;;Table Header
        [:div (use-sub-style style :table-header)
         [:div (use-sub-style style :table-header-row) "Section"]
         [:div (use-sub-style style :table-header-row) "Chart"]
         [:div (use-sub-style style :table-header-row-fixed) "Pin Chart"]
         [:div (use-sub-style style :table-header-row) "Sensor"]
         [:div (use-sub-style style :table-header-row-fixed) "UOM"]
         [:div (use-sub-style style :table-header-row) "y-min"]
         [:div (use-sub-style style :table-header-row) "y-max"]
         [:div (use-sub-style style :table-header-right) "RFQ"]]
        ;;Table Rows
        (map-indexed (fn [ind s]
                       (let [{:keys [id charts]} s
                             section-name (:name s)
                             section-index ind]
                         ^{:key (str ind) }
                         [:div {:class "section"}
                          (map-indexed (fn [index chart-id]
                                         (let [chart-data (get-in chart [chart-id])
                                               y1-sensor (->> (map-indexed
                                                                (fn [i d]
                                                                  (let [name (:name d)
                                                                        description (get sensor name)
                                                                        sensor-description (:description description)]
                                                                    {:name name
                                                                     :description sensor-description
                                                                     :index i}))
                                                                (get-in chart-data [:y1-axis :series]))
                                                              (mapv (fn [v]
                                                                      v)))
                                               y2-sensor (->> (map-indexed (fn [i d]
                                                                             (let [name (:name d)
                                                                                   description (get sensor name)
                                                                                   sensor-description (:description description)]
                                                                               {:name name
                                                                                :description sensor-description
                                                                                :index i}))
                                                                           (get-in chart-data [:y2-axis :series]))
                                                              (mapv (fn [v]
                                                                      v)))
                                               axis-sensor (into y1-sensor y2-sensor)
                                               {:keys [id name]} chart-data
                                               row (mod index 2)
                                               pinned? (some (partial = chart-id) pinned-charts)]
                                           (map-indexed (fn [i d]
                                                          ^{:key (str i index)}
                                                          (let
                                                            [disable-min-max
                                                             (if (= (:index d) 0)
                                                               false
                                                               true)]
                                                            [chart-config-row
                                                             (- width 32)
                                                             (mod index 2)
                                                             (if (and (= i 0)
                                                                      (= index 0))
                                                               section-name
                                                               " ")
                                                             d
                                                             (if (= i 0)
                                                               chart-data
                                                               {:name " "})
                                                             (charts-config
                                                               (keyword (:name d)))
                                                             (if (= i 0)
                                                               pinned?
                                                               " ")
                                                             disable-min-max
                                                             ])

                                                          )
                                                        axis-sensor)
                                           )) charts)]
                         )) section-list)]]]]))

(defn settings [props]
  (let [config? @(rf/subscribe [::app-subs/config?])
        ]
    [app-view/layout-main
     (translate [:settings :title :text] "Plant Configuration")
     (translate [:settings :title :sub-text] "")
     [(if config?
        [app-comp/button {:disabled? (if (show-error?)
                                       (not @(rf/subscribe [::subs/can-submit?]))
                                       (not @(rf/subscribe [::subs/dirty?])))
                          :icon ic/upload
                          :label (translate [:action :upload :label] "Save")
                          :on-click #(rf/dispatch [::event/upload])
                          }])
      [app-comp/button {:icon  ic/cancel
                        :label (translate [:action :cancel :label] "Cancel")
                        :on-click #(rf/dispatch [::root-event/activate-content :section nil "home"])}]]
     (if config?
       chart-config
       no-config)]))




