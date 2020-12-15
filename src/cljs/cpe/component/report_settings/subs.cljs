;; subscriptions for component report-settings
(ns cpe.component.report-settings.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.app.subs :as app-subs]
            [cpe.util.auth :as auth]
            [cpe.util.common :as au]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

;; primary signals
(rf/reg-sub
 ::report-settings
 (fn [db _]
   (get-in db [:component :report-settings])))

(rf/reg-sub
 ::component
 (fn [db _] (get-in db [:component :report-settings])))

(rf/reg-sub
 ::src-data
 :<- [::app-subs/report-date-range]
 :<- [::app-subs/comments]
 (fn [[report-date-range comments] _]
   (let [included-in-report
         (reduce (fn [col [k v]]
                   (assoc col
                          (:id v)
                          {:id (:id v)
                           :include-in-report (:include-in-report v)}))
                 {}
                 comments)]
     {:report-date-range report-date-range
      :comments-to-include included-in-report})))

(rf/reg-sub
 ::data
 :<- [::component]
 :<- [::src-data]
 (fn [[component src-data] _]
   (or (:data component) src-data)))

(rf/reg-sub
 ::form
 :<- [::component]
 (fn [component _] (:form component)))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]] (get-field path form data)))

(rf/reg-sub
 ::dirty?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _]
   (not= src-data data)))

(rf/reg-sub
 ::valid?
 :<- [::form]
 (fn [form _]
   (not (au/some-invalid form))))

(rf/reg-sub
 ::can-submit?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _] (and dirty? valid?)))

(rf/reg-sub
 ::warn-on-close?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _]
   (or dirty? (not valid?))))

(rf/reg-sub
 ::show-error? ;; used for hiding errors until first click on submit
 :<- [::component]
 (fn [component _] (:show-error? component)))

;; derived signals/subscriptions
(rf/reg-sub
 ::sorted-comments-id
 (fn [db _] (get-in db [:component :comment :sorted-comments-id])))

(rf/reg-sub
 ::sorted-comments-by-date
 :<- [::app-subs/comments]
 :<- [::sorted-comments-id]
 (fn [[comments sorted-comments-id] [_ date-range]]
   (let [sorted-comments (reduce (fn [col id]
                                (conj col (get comments id)))
                                 [] sorted-comments-id)]
     ;;(println "date-range:" date-range)
     (if date-range
       (reduce (fn [col comment]
                 (if (< date-range (:last-post comment))
                   (conj col comment)
                   col))
               [] sorted-comments)
       sorted-comments))))

(defn filter-comments-by-chart
  ([chart-id]
   (filter-comments-by-chart chart-id nil))
  ([chart-id date-range]
   (let [comments @(rf/subscribe [::sorted-comments-by-date date-range])]
     (reduce (fn [col comment-data]
               (let [comment-chart-id (get-in comment-data [:chart-info :chart-id :id])]
                 (if (= chart-id comment-chart-id)
                   (conj col comment-data)
                   col)))
             []
             comments))))

(rf/reg-sub
 ::sorted-comments-by-chart
 :<- [::app-subs/section]
 :<- [::app-subs/chart]
 :<- [::data]
 (fn [[section chart data] [_ specific-date-range]]
   (let [format-report-date-range (js/Date. (:report-date-range data))
         date-range (if specific-date-range
                      specific-date-range
                      format-report-date-range)]
     (reduce (fn [col [k v]]
               (let [chart-ids (:charts v)
                     chart-data (mapv (fn [ch-id]
                                        (let [chd (get chart ch-id)
                                              comment-data (filter-comments-by-chart ch-id date-range)]
                                          (-> {}
                                              (assoc :chart-id (:id chd))
                                              (assoc :chart-name (:name chd))
                                              (assoc :chart-title (:title chd))
                                              (assoc :comments comment-data)
                                              )))
                                      chart-ids)]
                 (conj col {:section-name (:name v)
                            :charts chart-data})))
             []
             section))))
