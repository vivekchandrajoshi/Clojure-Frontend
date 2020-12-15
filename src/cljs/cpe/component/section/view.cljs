;; view elements component section
(ns cpe.component.section.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [cpe.app.icon :as ic]
            [cpe.app.view :as app-view :refer [tab-layout]]
            [cpe.app.style :as app-style]
            [cpe.app.comp :as app-comp]
            [cpe.app.subs :as app-subs]
            [cpe.app.event :as app-event]
            [cpe.component.section.style :as style]
            [cpe.component.section.subs :as subs]
            [cpe.component.section.event :as event]
            [cpe.app.scroll :refer [scroll-box]]
            [cpe.app.charts :refer [cpe-chart]]
            [cpe.component.comment.event :as comment-event]
            [cpe.component.comment.subs :as comment-subs]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cpe.component.comment.view :refer [comment-data]]))

(def date-formatter (tf/formatter "yyyy-MM-dd"))

(defn format-date [date] (tf/unparse date-formatter (t/date-time date)))

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

;; context menu to be shown in settings menu
(defn context-menu []
  (let [topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
    (vec (cond-> [{:id       :settings
                   :icon     (as-left-icon ic/gear)
                   :label-fn #(translate [:root :menu :my-apps] "Configure plant")
                   :event-id ::app-event/open-settings}
                  {:id       :uom
                   :icon     (as-left-icon ic/dataset)
                   :label-fn #(translate [:root :menu :my-apps] "UOM")
                   :event-id ::app-event/open-uom}]
           topsoe? (conj {:id       :report-settings
                          :icon     (as-left-icon ic/gear)
                          :label-fn #(translate [:root :menu :my-apps] "Report settings")
                          :event-id ::app-event/open-report-settings})))))

(defn comments [width height]
  (let [view-size {:width width, :height height};;@(rf/subscribe [::ht-subs/view-size])
        plant     @(rf/subscribe [::app-subs/plant])]
    [comment-data view-size plant]))

(defn chart-block [chart-data]
  (let [{:keys [y1-axis-series y2-axis-series
                y1-color-shape y2-color-shape
                y1-axis y2-axis
                x-axis y1-sensor-name
                y2-sensor-name merged-sensor
                set-sql-value height width
                full-chart? chart-id title active-unit]} chart-data]
    (fn []
      [:div {:style {:height (if height
                               (- height 20)
                               350) :width  (- (* width 0.49) 20)
                     :background-color "#FFF"
                     :margin "20px 0 0 20px"
                     :display "inline-block"
                     :overflow "hidden"}}
       [(let [pin-chart     @(rf/subscribe [::subs/get-pin-chart chart-id])
              get-sql-value @(rf/subscribe [::subs/charts-sql-data])
              y1-min        (if y1-sensor-name
                              (apply min (mapv (fn [d]
                                                 (get-in get-sql-value [d :y-min]))
                                               y1-sensor-name)))
              y1-max        (if y1-sensor-name
                              (apply max (mapv (fn [d]
                                                 (let [y-max (get-in get-sql-value [d :y-max])
                                                       y-min (get-in get-sql-value [d :y-min])
                                                       y-dif (- y-max y-min)
                                                       y-per (* (/ y-dif 100)
                                                                10)]
                                                   (+ y-max y-per)))
                                               y1-sensor-name)))
              y2-min        (if y2-sensor-name
                              (apply min (mapv (fn [d]
                                                 (get-in get-sql-value [d :y-min]))
                                               y2-sensor-name)))
              y2-max        (if y2-sensor-name
                              (apply max (mapv (fn [d]
                                                 (let [y-max (get-in get-sql-value [d :y-max])
                                                       y-min (get-in get-sql-value [d :y-min])
                                                       y-dif (- y-max y-min)
                                                       y-per (* (/ y-dif 100)
                                                                10) ]
                                                   (+ y-max y-per)))
                                               y2-sensor-name)))
              y1-data       (if y1-sensor-name
                              (into [] (map (fn [d]
                                              (let [sensor-desc @(rf/subscribe [::subs/get-sensor-desc d])]
                                               (merge {:rfq (get-in get-sql-value [d :rfq])
                                                        :name        d
                                                        :sensor-desc sensor-desc
                                                        :data        (map (fn [d]
                                                                            {:x (:x d)
                                                                             :y (:y d)}) (get-in get-sql-value [d :sensor-value]))} (get y1-color-shape d))))
                                            y1-sensor-name)))

              y2-data       (if y2-sensor-name
                              (into [] (map (fn [d]
                                              (let [sensor-desc @(rf/subscribe [::subs/get-sensor-desc d])]
                                                (merge {:rfq (get-in get-sql-value [d :rfq])
                                                        :name d
                                                        :sensor-desc sensor-desc
                                                      :data (map (fn [d]
                                                                   {:x (:x d)
                                                                    :y (:y d)}) (get-in get-sql-value [d :sensor-value]))} (get y2-color-shape d))))
                                            y2-sensor-name)))
              x-min         (if merged-sensor
                              (apply min (mapv (fn [d]
                                                 (get-in get-sql-value [d :x-min]))
                                               merged-sensor)))
              x-max         (if merged-sensor
                              (apply max (mapv (fn [d]
                                                 (let [x-max (get-in
                                                               get-sql-value [d :x-max])
                                                       x-min (get-in
                                                               get-sql-value [d :x-min])
                                                       x-dif (- x-max x-min)
                                                       x-per (* (/ x-dif 100)
                                                                1)]
                                                   (+ x-max x-per)))
                                               merged-sensor)))
              y1-axis-label  (if (and y1-axis @(rf/subscribe [::subs/get-axis-lable-uom ((first y1-axis-series) :name)]))
                               (str (y1-axis :label) " " "[" @(rf/subscribe [::subs/get-axis-lable-uom ((first y1-axis-series) :name)]) "]")
                               (y1-axis :label))
              y2-axis-label  (if y2-axis
                               (str (y2-axis :label) " " "[" @(rf/subscribe [::subs/get-axis-lable-uom ((first y2-axis-series) :name)]) "]")
                               ;(y2-axis :label)
                               nil)
              x-axis-label   (if x-axis
                               (x-axis :label)
                               nil)]

          (fn []
            [cpe-chart
             {:height             (if height
                                    (- height 25)
                                    350)
              :width              (- (* width 0.49) 20)
              :on-show-fullscreen #(rf/dispatch [::event/show-full-chart chart-id])
              :on-hide-fullscreen #(rf/dispatch [::event/hide-full-chart])
              :y2-axis?           (if y2-axis-label true)
              :x-domain           [x-min x-max]
              :y1-domain          [y1-min y1-max]
              :y2-domain          [y2-min y2-max]
              :y1-series          y1-data
              :loading?           (not (and x-min x-max))
              :full-chart?        full-chart?
              :pinned-chart?      pin-chart
              :on-pin-chart       #(rf/dispatch [::event/pin-chart chart-id])
              :on-un-pin-chart    #(rf/dispatch [::event/un-pin-chart chart-id])
              :y2-series          y2-data
              :title              title
              :x-title            x-axis-label
              :y1-title           y1-axis-label
              :y2-title           y2-axis-label}]))]])))

(defn prepare-chart-data [plant-id chart-id active-unit set-sql-value width
                          height full-chart?]
  (let [{:keys [name title x-axis y1-axis y2-axis]} @(rf/subscribe [::subs/get-charts-data chart-id])

        y1-axis-series (if y1-axis
                         (y1-axis :series)
                         nil)
        y2-axis-series (if y2-axis
                         (y2-axis :series)
                         nil)
        y1-color-shape (if y1-axis-series
                         (into {} (map (fn [d]
                                         {(:name d) {:color (:color d)
                                                     :shape :square
                                                     ;(:shape d)
                                                     }})y1-axis-series)))
        y2-color-shape (if y1-axis-series
                         (into {} (map (fn [d]
                                         {(:name d) {:color (:color d)
                                                     ;(:color d)
                                                     :shape :circle
                                                     ;(:shape d)
                                                     }})y2-axis-series)))
        y1-sensor-name (if y1-axis-series
                         (mapv (fn [d]
                                 (d :name))
                               y1-axis-series)
                         nil)
        y2-sensor-name (if y2-axis-series
                         (mapv (fn [d]
                                 (d :name))
                               y2-axis-series)
                         nil)
        merged-sensor  (into (if y1-sensor-name
                               y1-sensor-name
                               [])
                             (if y2-sensor-name
                               y2-sensor-name
                               []))
        set-sql-value  set-sql-value]

    [chart-block {:y1-axis-series y1-axis-series
                  :y2-axis-series y2-axis-series
                  :y1-color-shape y1-color-shape
                  :y2-color-shape y2-color-shape
                  :y1-axis        y1-axis
                  :y2-axis        y2-axis
                  :x-axis         x-axis
                  :y1-sensor-name y1-sensor-name
                  :y2-sensor-name y2-sensor-name
                  :merged-sensor  merged-sensor
                  :set-sql-value  set-sql-value
                  :width          width
                  :height         height
                  :full-chart?    full-chart?
                  :chart-id       chart-id
                  :title          title
									:active-unit active-unit}]))

(defn chart-container [section-id width height plant-id]
  (let [section-chart @(rf/subscribe [::subs/get-section-chart section-id])
        show-dialog   @(rf/subscribe [::subs/show-full-chart])
        active-unit @(rf/subscribe [::app-subs/active-unit-system])
        merged-sensor (reduce-kv (fn [col indexed d]
                                   (let [chart @(rf/subscribe [::subs/get-charts-data
                                                               d])
                                         y1-series (get-in chart
                                                           [:y1-axis
                                                            :series])
                                         y2-series (get-in chart
                                                           [:y2-axis
                                                            :series])
                                         merged-sensor (into
                                                         y1-series y2-series)]
                                     (-> col
                                         (into merged-sensor)
                                         )))
                                 [] section-chart)
        sensor-name (map (fn [d]
                           (:name d)
                           )
                         merged-sensor)
        set-sql-value  (rf/dispatch [::event/fetch-sql-data sensor-name])]
    [:div {:style {:padding-bottom "10px"}}
     (if show-dialog
       [:div (use-sub-style style/chart-style :full-screen)
        [prepare-chart-data plant-id (:chart-id show-dialog) active-unit set-sql-value
         (* (/ width 0.75) 2)
         height true]])
     (map (fn [chart]
            ^{:key chart}
            [prepare-chart-data plant-id chart active-unit set-sql-value width])
          section-chart)]))

(defn summary-history-block [edit-mode? edit-id topsoe?
                             {:keys [id status subject
                                     created-by date-created
                                     date-published published-by]}
                             publish-summary-id
                             width]
  [:div {:style {:background-color "#f3f3f3"
                 :padding "10px"
                 :margin "10px 0px"
                 :margin-right "10px"
                 :width (str width "px")}}
   ;;Header of the summary-history block
   [:div (use-sub-style style/summary :header)
    ;;Left side of the header in history block
    [:div (use-sub-style style/summary :header-left)
     (if (= status "draft")
       [:div
        [:div created-by]
        [:div (use-sub-style style/summary :header-date)
         (format-date date-created)]]
       [:div
        [:div published-by]
        [:div (use-sub-style style/summary :header-date)
         (format-date date-published)]])]

    ;;Right side of the header in history block
    [:div (use-sub-style style/summary :header-right)
     (if (= status "draft")
       [:div
        [app-comp/icon-button {:disabled? edit-mode?
                               :icon ic/upload
                               :on-click #(rf/dispatch [::event/publish-summary id])}]
        [app-comp/icon-button-s {:disabled? edit-mode?
                                 :icon ic/pencil
                                 :on-click #(rf/dispatch [::event/edit-summary id])}]
        [app-comp/icon-button-s {:disabled? edit-mode?
                                 :icon ic/delete
                                 :on-click #(rf/dispatch
                                             [::app-event/warning-for-delete
                                              [::event/delete-summary id]])}]]
       ;;Adding the star icon on summary to be published
       [:div {:style {:margin-right "10px", :margin-top "10px"}}
        (if (= id publish-summary-id)
          [ic/star]
          "")])]]
   ;;Body of the history block
   (if (and edit-mode? (= id edit-id))
     [:div
      [app-comp/text-area {:height 100
                           :value  subject
                           :width  (- width 20)
                           :on-change #(rf/dispatch [::event/update-edit-summary %])}]
      [app-comp/button {:disabled? (empty? subject)
                        :icon ic/upload
                        :label "Save"
                        :on-click #(rf/dispatch [::event/upload-edit-summary])}]
      [app-comp/button {:disabled? false
                        :icon ic/cancel
                        :label "Cancel"
                        :on-click #(rf/dispatch [::event/cancel-edit-summary])}]]
     [:div subject])])

(defn summary [width height]
  (let [topsoe? @(rf/subscribe [::ht-subs/topsoe?])
        scroll-height (if topsoe? (- height 240) (- height 70))
        scroll-width (- width 8)
        data @(rf/subscribe [::subs/data])
        comp-data @(rf/subscribe [::subs/comp-data])
        can-submit? @(rf/subscribe [::subs/can-submit?])
        edit-mode? (not (nil? (:edit-id comp-data)))
        summary-to-publish @(rf/subscribe [::subs/summary-to-publish])
        sorted-summary-list @(rf/subscribe [::subs/sorted-summary-list])]
    [:div
     (if topsoe?
       [:div (use-sub-style style/summary :add-summary)
        [:div "Add new summary:"]
        [:div {:style {:margin-left "-12px"}}
         [app-comp/text-area {:disabled? edit-mode?
                              :height 100
                              :value  (:summary comp-data)
                              :width  (+ width 12)
                              :on-change #(rf/dispatch [::event/update-summary %])}]
         [app-comp/button {:disabled? (or (not can-submit?) edit-mode?)
                           :icon ic/plus
                           :label "Add"
                           :on-click #(rf/dispatch [::event/upload-summary])}]]])
     [scroll-box {:style {:height scroll-height, :width scroll-width}}
      (reduce (fn [col summary-data]
                (conj col [summary-history-block
                           (or (not (empty? (:summary comp-data)))
                                                     edit-mode?)
                           (:edit-id comp-data)
                           topsoe?
                           summary-data
                           (:id summary-to-publish)
                           (- scroll-width 10)]))
              [:div]
              sorted-summary-list)
      ]
     ]))

(defn section [props section-id plant-id]
  (let [view-size        (:size props)
        comment-width (/ (:width view-size) 4)
        content-height   (:height view-size)
        scroll-box-width (* (:width view-size) 0.75)
        style            (style/chart-container view-size)
        tab-index @(rf/subscribe [::app-subs/section-active-tab])
        email-sub @(rf/subscribe [::comment-subs/email-sub])]
    [:div {:style {:display "inline-flex"}}
     [:div {:style (style :container)}
      [scroll-box {:style (style :scroll-box)}
       [chart-container section-id scroll-box-width content-height plant-id]]]
     [tab-layout {:top-tabs {:selected tab-index
                             :on-select #(rf/dispatch [::app-event/update-section-active-tab %])
                             :labels [{:name     "Comments"
                                       :has-icon true
                                       :icons    [[:span
                                                   (merge
                                                     (use-style
                                                       (style/tab-hader-icon :icon-button-left))
                                                     {:on-click #(rf/dispatch [::comment-event/add-comment])})
                                                   [ic/plus (use-style
                                                              (style/tab-hader-icon :icon-style))]]
                                                  (if email-sub
                                                    [:span (merge
                                                       (use-style
                                                         (style/tab-hader-icon :icon-button-right))
                                                       {:style {:left
                                                                (- (/
                                                                     comment-width 2) 28)}
                                                        :on-click #(rf/dispatch [::comment-event/email-sub false])})
                                                     [ic/unsubscribe-bell
                                                      (merge  (use-style
                                                          (style/tab-hader-icon :icon-style)))]]
                                                    [:span (merge (use-style
                                                         (style/tab-hader-icon :icon-button-right))
                                                       {:on-click
                                                        #(rf/dispatch [::comment-event/email-sub true])
                                                        :style
                                                        {:left (- (/
                                                                    comment-width 2) 28)}})
                                                     [ic/bell  (use-style
                                                                  (style/tab-hader-icon :icon-style))]])]}
                                      {:name "Summary"}]}
                  :width comment-width, :height content-height
                  :content (fn [width height selected]
                             (if (= 1 tab-index)
                               [summary comment-width (- content-height 30)]
                               [comments (- comment-width 40)
                                (- content-height 40)]))}]]))
