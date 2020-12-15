;; view elements component uom
(ns cpe.component.uom.view
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
            [cpe.app.event :as app-event]
            [cpe.app.scroll :as scroll]
            [cpe.app.comp :as app-comp]
            [cpe.app.icon :as ic]
            [cpe.component.root.event :as root-event]
            [cpe.component.uom.subs :as root-subs]
            [cpe.component.uom.style :as style]
            [cpe.component.uom.subs :as subs]
            [cpe.component.uom.event :as event]))

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

;; context menu to be shown in settings menu
(defn context-menu []
  (let [topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
    (vec (cond-> [{:id       :settings
                   :icon     (as-left-icon ic/gear)
                   :label-fn #(translate [:root :menu :my-apps] "Configure plant")
                   :event-id ::app-event/open-settings}]
           topsoe? (conj {:id       :report-settings
                          :icon     (as-left-icon ic/gear)
                          :label-fn #(translate [:root :menu :my-apps] "Report settings")
                          :event-id ::app-event/open-report-settings})))))

(defn find-selected-unit [unit-id unit-options]
  (first (remove nil? (map (fn [data]
                             (if (= unit-id (:id data))
                               data
                               nil)) unit-options))))

(defn row [bg-index data disable?]
  ;;(println "Data:" data)
  (let [uom-id (:id data)
        unit-options (:units data)
        unit-name (:name data)
        selected-unit (find-selected-unit (:selected-unit-id data) unit-options)]
    [:div (use-style (style/row bg-index))
     [:div (use-sub-style (style/row bg-index) :left) unit-name]
     [:div (use-sub-style (style/row bg-index) :right)
      (if disable?
        (:name selected-unit)
        [app-comp/dropdown-selector {:width      150
                                     :item-width 250
                                     :selected   selected-unit
                                     :items      unit-options
                                     :scroll?    true
                                     :value-fn   :id
                                     :disabled?  disable?
                                     :on-select  #(rf/dispatch [::event/update-uom uom-id %])
                                     :label-fn   :name}])]]))

(defn body [{:keys [width height]}]
  ;; [:div "This is the uom settings content are.."]
  (let [w (* (- width 85) 0.6)
        h (- height 40)
        style (style/body width height)
        disable-save-default-unit-system?
        @(rf/subscribe [::subs/disable-save-default-unit-system?])
        disable-save-view-edit-unit-system?
        @(rf/subscribe [::subs/disable-save-view-edit-unit-system?])
        disable? @(rf/subscribe [::subs/editable?])
        default-unit-system @(rf/subscribe [::subs/field [:default-unit-system]])
        active-unit-system @(rf/subscribe [::app-subs/active-unit-system])
        view-edit-unit-system @(rf/subscribe [::subs/field [:view-edit-unit-system]])
        units @(rf/subscribe [::subs/units (:value view-edit-unit-system)])
        all-unit-system @(rf/subscribe [::subs/all-unit-system])
        unit-system-options (reduce (fn [col unit-name]
                                      (conj col {:name unit-name}))
                                    [] (keys all-unit-system))]
    [:div (use-style style)
     [scroll/scroll-box (use-sub-style style :form-scroll)
      [:div (use-sub-style style :form-scroll)
       [:div (use-sub-style style :unit-system-container)
        ;;Section to view/edit unit system
        [:div (use-sub-style style :section)
         [:div (use-sub-style style :section-header) "View/edit unit system:"]
         [:div (use-sub-style style :section-content)
          [app-comp/dropdown-selector {:width      150
                                       :item-width 250
                                       :selected   {:name (:value view-edit-unit-system)}
                                       :items      unit-system-options
                                       :scroll?    true
                                       :value-fn   :id
                                       :disabled?  false
                                       :on-select  #(rf/dispatch [::event/update-view-edit-unit-system %])
                                       :label-fn   :name}]

          ;;Drawing the unit system table
          [:div (use-sub-style style :table)
           ;;Table Header
           [:div (use-sub-style style :table-header)
            [:div (use-sub-style style :table-header-left) "Unit name"]
            [:div (use-sub-style style :table-header-right) "Selected unit"]]
           ;;Table Rows
           (map-indexed (fn [index [k v]]
                          ^{:key index}
                          [row (mod index 2) v disable?]
                          ) units)]]]]]]]))

(defn no-config [{:keys [width height]}]
  [:div {:style {:width width, :height height}}
   [:div (use-style style/no-config)
    "Missing configuration!"]])

(defn show-error? [] @(rf/subscribe [::subs/show-error?]))

(defn uom [props]
  (let [config? @(rf/subscribe [::app-subs/config?])
        disable-save-view-edit-unit-system?
        @(rf/subscribe [::subs/disable-save-view-edit-unit-system?])]
    [app-view/layout-main
     (translate [:settings :title :text] "Settings")
     (translate [:settings :title :sub-text] "Units of measurement")
     [ [app-comp/button {:disabled? disable-save-view-edit-unit-system?
                         :icon ic/upload
                         :label (translate [:action :upload :label] "Save")
                         :on-click #(rf/dispatch [::event/upload-unit-system])}]
      [app-comp/button {:icon  ic/cancel
                        :label (translate [:action :cancel :label] "Cancel")
                        :on-click #(rf/dispatch [::root-event/activate-content :section nil "home"])}]]
     (if config?
       body
       no-config)]))
