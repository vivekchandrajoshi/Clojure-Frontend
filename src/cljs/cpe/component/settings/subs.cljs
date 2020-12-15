;; subscriptions for component settings
(ns cpe.component.settings.subs
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
  ::component
  (fn [db _] (get-in db [:component :settings])))

(rf/reg-sub
  ::src-data
  :<- [::app-subs/plant]
  (fn [plant _] (dissoc (:settings plant) :created-by :date-created :modified-by :date-modified)))

(rf/reg-sub
  ::data
  :<- [::component]
  :<- [::src-data]
  (fn [[component src-data]]
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

(defn validate-data [d1 d2]
  (let [r (some true? (map (fn [[k v]]
                             (let [comp-fn (fn [v1 v2]
                                             (let [v1-nil? (nil? v1)
                                                   v2-nil? (nil? v2)]
                                               (cond
                                                 (and v1-nil? v2-nil?) false
                                                 (or v1-nil? v2-nil?) true
                                                 :else (let [err (- v1 v2)
                                                             abserr (js/Math.abs err)
                                                             abserrf (/ abserr v1)
                                                             res (> abserrf
                                                                    0.0001)]
                                                         res))))
                                   data2 (get-in d2 [:settings :charts-config k])
                                   d2-min (get data2 :min)
                                   d2-max (get data2 :max)
                                   d2-rfq (get data2 :rfq)
                                   d1-min (:min v)
                                   d1-max (:max v)
                                   d1-rfq (:rfq v)]
                               (or (comp-fn d1-min d2-min)
                                   (comp-fn d1-max d2-max)
                                   (comp-fn d1-rfq d2-rfq))))d1))]
    r))

(rf/reg-sub
  ::dirty?
  :<- [::data]
  :<- [::src-data]
  :<- [::app-subs/plant]
  (fn [[data src-data plant] _]
    (let [n-data (:pinned-charts data)
          n-src-data (:pinned-charts src-data)
          n-data-cc (:charts-config data)
          n-src-data-cc (:charts-config src-data)
          n-d-pc (:pinned-charts data)
          n-s-d-pc (:pinned-charts src-data)
          n-d-cc (:validate-charts-config data)
          n-s-d-cc (:charts-config src-data)]
      (or (not= n-data n-src-data)
          (not= (set n-d-pc) (set n-s-d-pc))
          (validate-data n-d-cc plant)
          ))))

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
  :<- [::app-subs/config?]
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[config? dirty? valid?] _]
    (if config?
      (or dirty? (not valid?)))))

(rf/reg-sub
  ::show-error? ;; used for hiding errors until first click on submit
  :<- [::component]
  (fn [component _] (:show-error? component)))

(rf/reg-sub
  ::chart
  (fn [db _]
    (get-in db [:chart])))

(rf/reg-sub
  ::sensor
  (fn [db _]
    (get-in db [:sensor])))

(rf/reg-sub
  ::settings-active-tab
  (fn [db _] (:settings-active-tab db)))

(rf/reg-sub
  ::charts-config
  :<- [::app-subs/plant]
  (fn [plant _]
    (get-in plant [:settings :charts-config])))
