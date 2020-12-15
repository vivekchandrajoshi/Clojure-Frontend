;; subscriptions for dialog add_comment
(ns cpe.dialog.add-comment.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.util.common :as au]
            [cpe.app.subs :as app-subs]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :add-comment])))

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::data
 :<- [::dialog]
 (fn [dialog _]
   (:data dialog)))

;(rf/reg-sub
; ::field
; :<- [::dialog]
; (fn [dialog [_ id]]
;   (get-in dialog [:field id])))

(rf/reg-sub
  ::form
  :<- [::dialog]
  (fn [dialog _] (:form dialog)))

(rf/reg-sub
  ::field
  :<- [::form]
  :<- [::data]
  (fn [[form data] [_ path]]
    (get-field path form data)))


(rf/reg-sub
  ;"get all chart name"
  ::date-of-sor
  :<- [::dialog]
  (fn  [dialog [_ path]]
    (get-in dialog path)
   ))



(rf/reg-sub
  ;"get all chart name"
  ::chart-name
  (fn  [db _]
    (-> (map (fn [chart]
               (let [chart-data (second  chart)]
                 {:id (get-in chart-data [:id]) :name (get-in chart-data [:name])}))
          (get db :chart))
      (conj {:id "0" :name "Select Chart"}))))



(rf/reg-sub
  ;"get all status name"
  ::status
  (fn  [db _]
    (mapv (fn [status]
            (let [status-data (second  status)]
              {:id (get-in status [:id]) :name (get-in status [:name])}))
          (get-in db [:misc  "comment-status" :data]))))

(rf/reg-sub
  ; "get test"
  ::selected-data
  :<- [::dialog]
  (fn [dialog [_ path]]
    (get-in dialog path)))

(rf/reg-sub
  ::src-data
  :<- [::dialog]
  (fn [dialog _] (:src-data dialog)))

(rf/reg-sub
  ::dirty?
  :<- [::data]
  :<- [::src-data]
  (fn [[data src-data] _] (not= data src-data)))

(rf/reg-sub
  ::valid?
  :<- [::form]
  (fn [form _] (not (au/some-invalid form))))

(rf/reg-sub
  ::can-submit?
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[dirty? valid?] _] (and dirty? valid?)))

(rf/reg-sub
  ::warn-on-close?
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[dirty? valid?] _] (or dirty? (not valid?))))

(rf/reg-sub
  ::show-error? ;; used for hiding errors until first click on submit
  :<- [::dialog]
  (fn [dialog _] (:show-error? dialog)))

(rf/reg-sub
  ; "get selected chart"
  ::selected-chart
  (fn [db _]
    (get-in db [:dialog :add-comment :data :chart-info :chart-id])))

(rf/reg-sub
  ; "get selected chart"
  ::selected-status
  (fn [db _]
    (get-in db [:dialog :add-comment :data :type-id ])))




