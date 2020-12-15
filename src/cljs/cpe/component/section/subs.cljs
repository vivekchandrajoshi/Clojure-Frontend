;; subscriptions for component section
(ns cpe.component.section.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.app.subs :as app-subs]
            [cpe.util.auth :as auth]
            [cpe.component.uom.subs :as uom-subs]
            [cpe.util.common :as au]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)


;; derived signals/subscriptions
(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

;; primary signals
(rf/reg-sub
 ::section
 (fn [db _]
   (get-in db [:component :section])))

(rf/reg-sub
 ::component
 (fn [db _] (get-in db [:component :section])))

(rf/reg-sub
 ::src-data
 :<- [::app-subs/summary]
 (fn [summary _]
   {:summary-history summary}))

(rf/reg-sub
 ::data
 :<- [::src-data]
 (fn [src-data _]
   ;(or (:data component) src-data)
   src-data))

(rf/reg-sub
  ::comp-data
  :<- [::component]
  (fn [component _]
    ;(or (:data component) src-data)
    (:data component)))

(rf/reg-sub
 ::form
 :<- [::component]
 (fn [component _] (:form component)))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::comp-data]
 (fn [[form comp-data] [_ path]] (get-field path form comp-data)))

(rf/reg-sub
 ::dirty?
 :<- [::comp-data]
 :<- [::src-data]
 (fn [[comp-data src-data] _]
   (let [n-data {:summary-history (:summary-history comp-data)}]
     (if (or (not (empty? (:summary comp-data))) (not= src-data n-data))
       true
       false))))

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





(rf/reg-sub
  ::get-section-chart
  :<- [::app-subs/section]
  :<- [::app-subs/plant]
  (fn [[section plant] [_ id]]
    (if (= id "home")
      (get-in plant [:settings :pinned-charts])
      (get-in section [id :charts]))))

(rf/reg-sub
  ::get-charts-data
  (fn [db [_ id]]
    (get-in db [:chart id])))

(rf/reg-sub
  ::charts-sql-data
  (fn [db _]
    (get-in db [:component :chart-sql-data])))

(rf/reg-sub
  ::charts-sql-data-id
  :<- [::charts-sql-data]
  (fn [charts-sql-data [_ sensor-name]]
    (mapv (fn [name]
               (get-in charts-sql-data [name]))
          sensor-name)))

(rf/reg-sub
  ::show-full-chart
  (fn [db [_]]
    (get db :show-chart-dialog)))

(rf/reg-sub
  ::sensor-data
  (fn [db _]
    (get db :sensor)))

(rf/reg-sub
  ::get-sensor-desc
  :<- [::sensor-data]
  (fn [sensor-data [_ sensor-name]]
    (get-in sensor-data [sensor-name :description])))

(rf/reg-sub
  ::get-pin-chart
  :<- [::app-subs/plant]
  (fn [plant [_ id]]
    (let [index (-> (get-in plant [:settings :pinned-charts])
                  (-indexOf id))]
      (if (>= index 0)
        true
        false))))

(rf/reg-sub
  ::get-sensor-config
  :<- [::app-subs/plant]
  (fn [plant]
    (get-in plant [:settings :charts-config ])))


(rf/reg-sub
  ::get-axis-lable-uom
  :<- [::uom-subs/units]
  :<- [::sensor-data]
  (fn [[units  sensor-data] [_ sensor-name]]
    (let [sensor-uom-id      (get-in sensor-data [sensor-name :uom-id])
          selected-uom       (get-in units [sensor-uom-id :selected-unit-id])
          selected-uom-units (get-in units [sensor-uom-id :units])
          selected-unit-val  (some (fn [d]
                                     (if (= (d :id) selected-uom)
                                       d)) selected-uom-units)
          label (get selected-unit-val :name)]
      label)))

(rf/reg-sub
 ::categorised-summary
 :<- [::comp-data]
 (fn [data _]

   (reduce (fn [{:keys [draft published]} [_ v]]
             (let [status (:status v)
                   col-draft (if (= status "draft")
                               (vec (conj draft v))
                               draft)
                   col-published (if (not= status "draft")
                                   (vec (conj published v))
                                   published)]
               {:draft col-draft, :published col-published}))
           {:draft [], :published []}
           (:summary-history data))))

(rf/reg-sub
 ::sorted-summary-list
 :<- [::categorised-summary]
 :<- [::ht-subs/topsoe?]
 (fn [[categorised-summary topsoe?] _]
   (let [sorted-draft-summary (sort-by :date-created >
                                       (:draft categorised-summary))
         sorted-published-summary (sort-by :date-published >
                                           (:published categorised-summary))]
     (if topsoe?
       (apply concat [sorted-draft-summary sorted-published-summary])
       sorted-published-summary))))

(rf/reg-sub
 ::summary-to-publish
 :<- [::categorised-summary]
 (fn [categorised-summary]
   (let [sorted (sort-by :date-published > (:published categorised-summary))]
     (first sorted))))
