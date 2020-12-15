;; view elements component report-settings
(ns cpe.component.report-settings.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.util.common :refer [to-date-time-map from-date-time-map]]
            [cpe.app.style :as app-style]
            [cpe.app.subs :as app-subs]
            [cpe.app.event :as app-event]
            [cpe.app.view :as app-view]
            [cpe.app.scroll :as scroll]
            [cpe.app.comp :as app-comp]
            [cpe.app.calendar :refer [date-picker]]
            [cpe.app.icon :as ic]
            [cpe.component.comment.view :refer [comment-created-by format-date]]
            [cpe.component.root.event :as root-event]
            [cpe.component.report-settings.style :as style]
            [cpe.component.report-settings.subs :as subs]
            [cpe.component.report-settings.event :as event]))

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

;; context menu to be shown in settings menu
(defn context-menu []
  [{:id       :settings
    :icon     (as-left-icon ic/gear)
    :label-fn #(translate [:root :menu :my-apps] "Configure plant")
    :event-id ::app-event/open-settings}
   {:id       :uom
    :icon     (as-left-icon ic/dataset)
    :label-fn #(translate [:root :menu :my-apps] "UOM")
    :event-id ::app-event/open-uom}])

(def bg-index (atom 0))

(defn row [bg-index topsoe? section-name chart-name included? comment-data]
  (let [title (:title comment-data)
        subject (:subject comment-data)
        collapse? (:collapse? comment-data)
        created-by (:created-by comment-data)
        start-of-run-day (get-in comment-data [:chart-info :start-of-run-day])
        end-of-run-day (get-in comment-data [:chart-info :end-of-run-day])
        date-created (:date-created comment-data)
        chart-id (get-in comment-data [:chart-info :chart-id])
        include-permanently (:include-in-report-permanently comment-data)
        disable? (if include-permanently true false)]
    ;;(println "\n\n" comment-data)

    [:div (use-style (style/row bg-index))
     ;;COL1:: Drawing the chart name
     [:div (use-sub-style (style/row bg-index) :col1) chart-name]

     ;;COL2:: Drawing the comment box
     [:div (use-sub-style (style/row bg-index) :col2)
      ;;Drawing the heading/title for the comment
      [:div (use-sub-style (style/row bg-index) :comment-title)
       [:span (use-sub-style (style/row bg-index) :comment-item)
        (comment-created-by created-by topsoe?)]
       [:span (use-sub-style (style/row bg-index) :comment-item)
        (str (if start-of-run-day
               (str " : Run days " start-of-run-day))
             (if end-of-run-day
               (str " To " end-of-run-day)))]
       (if chart-id
         (if-not (= (:id chart-id) "0")
           [:span (use-sub-style (style/row bg-index) :comment-item)
            (str " : " (:name chart-id))]))]
      ;;Drawing the subject/content of the comment
      [:div (use-sub-style (style/row bg-index) :comment-subject)
       subject]
      ;;Drawing the date and Read more button
      [:div (use-sub-style (style/row bg-index) :commented-on)
       (format-date date-created)]]

     ;;COL3:: Drawing the toggle button
     [:div (use-sub-style (style/row bg-index) :col3)
      [:div
       (if disable?
         ""
         [app-comp/toggle {:value included?
                           :disable? disable?
                           :on-toggle #(rf/dispatch
                                       [::event/update-include-comment-in-report
                                        (:id comment-data) %])}])]]]))

(defn body [{:keys [width height]}]
  (let [w (* (- width 85) 0.6)
        h (- height 40)
        style (style/body width height)
        topsoe? @(rf/subscribe [::ht-subs/topsoe?])
        date (to-date-time-map (js/Date.))
        comments-data @(rf/subscribe [::subs/sorted-comments-by-chart])
        data @(rf/subscribe [::subs/data])
        report-date-range (js/Date. (:report-date-range data))
        comments-to-include (:comments-to-include data)
        total-comments (apply + (map (fn [section]
                                               (apply + (map (fn [chart]
                                                               (count (:comments chart)))
                                                             (:charts section))))
                                             comments-data))]
    ;;(println "\n\nData:" data)

    (reset! bg-index 0)
    [:div (use-style style)
     [scroll/scroll-box (use-sub-style style :form-scroll)
      [:div (use-sub-style style :form-scroll)
       [:div (use-sub-style style :unit-system-container)

        [:div (use-sub-style style :section)
         [:div (use-sub-style style :section-header) "Select date to include comments from:"]
         [:div (use-sub-style style :section-content)
          [date-picker {:date (to-date-time-map report-date-range)
                        :valid? (if report-date-range true false)
                        :max (to-date-time-map (js/Date.))
                        :on-change
                        #(rf/dispatch
                          [::event/update-include-comments-from-date
                           (merge date %)])}]]]
        [:div (use-sub-style style :table)
         [:div (use-sub-style style :table-header-col1) "Chart name"]
         [:div (use-sub-style style :table-header-col2) "Comment"]
         [:div (use-sub-style style :table-header-col3) "Include in report"]
         ;;Table Rows
         (if (> total-comments 0)
           (map-indexed
            (fn [sh-index section]
              (map-indexed
               (fn [ch-index chart]
                 (map-indexed
                  (fn [c-index comment]
                    (let [section-name (if (and (= ch-index 0) (= c-index 0))
                                         (:section-name section)
                                         " ")
                          chart-name (if (= c-index 0)
                                       (:chart-name chart)
                                       " ")
                          included? (get-in comments-to-include
                                            [(:id comment) :include-in-report]
                                            false)]
                      (swap! bg-index inc)
                      ^{:key (str sh-index ch-index c-index)}
                      [row (mod @bg-index 2)
                       topsoe?
                       section-name
                       chart-name
                       included?
                       comment]))
                  (:comments chart)))
               (:charts section)))
            comments-data)
           [:div {:style {:padding "0px 10px"}} "No comments."])]]]]]))

(defn no-config [{:keys [width height]}]
  [:div {:style {:width width, :height height}}
   [:div (use-style style/no-config)
    "Missing configuration!"]])

(defn show-error? [] @(rf/subscribe [::subs/show-error?]))

(defn report-settings [props]
  (let [config? @(rf/subscribe [::app-subs/config?])
        can-submit? @(rf/subscribe [::subs/can-submit?])]
    [app-view/layout-main
     (translate [:settings :title :text] "Report settings")
     (translate [:settings :title :sub-text] "Comments to include in report")
     [[app-comp/button {:disabled? (not can-submit?)
                        :icon ic/upload
                        :label (translate [:action :upload :label] "Save")
                        :on-click #(rf/dispatch [::event/upload-report-settings])}]
      [app-comp/button {:icon  ic/cancel
                        :label (translate [:action :cancel :label] "Cancel")
                        :on-click #(rf/dispatch [::root-event/activate-content :section nil "home"])}]]
     (if config?
       body
       no-config)]))
