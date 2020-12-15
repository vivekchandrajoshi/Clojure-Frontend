(ns cpe.app.subs
  (:require [re-frame.core :as rf]
            [cpe.util.service :as svc]
            [cpe.app.event :as event]
            [ht.app.event :as ht-event]
            [reagent.ratom :as rr]))

;;;;;;;;;;;;;;;;;;;;;
;; Primary signals ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::user
 (fn [db _] (:user db)))

(rf/reg-sub
 ::client
 (fn [db _] (:client db)))

(rf/reg-sub
 ::plant
 (fn [db _] (:plant db)))

(rf/reg-sub-raw
 ::countries
 (fn [dba [_]]
   (let [f #(:countries @dba)]
     (if (empty? (f))
       (svc/fetch-search-options
        {:evt-success [::event/set-search-options]
         :evt-failure [::ht-event/service-failure true]}))
     (rr/make-reaction f))))

(rf/reg-sub
 ::active-unit-system
 (fn [db _] (:active-unit-system db)))

(rf/reg-sub
 ::section-active-tab
 (fn [db _] (:section-active-tab db)))

(rf/reg-sub
 ::section
 (fn [db _] (:section db)))

(rf/reg-sub
 ::summary
 (fn [db _] (:summary db)))

(rf/reg-sub
 ::chart
 (fn [db _] (:chart db)))

(rf/reg-sub
 ::report-date-range
 (fn [db _] (get-in db [:misc "report-date-range" :data] (js/Date.))))

(rf/reg-sub
 ::comments
 (fn [db _] (get-in db [:component :comment :comments])))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Derived signals/subscriptions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::temp-unit
 :<- [::plant]
 (fn [plant _] (get-in plant [:settings :temp-unit])))

(rf/reg-sub
 ::config?
 :<- [::plant]
 (fn [plant _]
   (let [{:keys [config]} plant]
     (and (some? (inst? (:date-sor config)))
          (some? (not-empty (:section config)))))))

(rf/reg-sub
 ::misc
 (fn [db _]
    (get-in db [:misc "unitList"])))
