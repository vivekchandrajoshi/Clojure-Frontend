;; subscriptions for component uom
(ns cpe.component.uom.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.app.subs :as app-subs]
            [cpe.util.auth :as auth]
            [cpe.util.common :as au]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)

;; primary signals
(rf/reg-sub
 ::uom
 (fn [db _]
   (get-in db [:component :uom])))

;; derived signals/subscriptions


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
 (fn [db _] (get-in db [:component :uom])))

(rf/reg-sub
 ::src-data
 :<- [::app-subs/plant]
 :<- [::app-subs/user]
 (fn [[plant user] _]
   {:default-unit-system (get-in user [:unit-system] "PLANT")
    :view-edit-unit-system (get-in user [:unit-system] "PLANT")
    :user-uoms (get-in user [:uoms] {})
    :plant-uoms (get-in plant [:settings :uoms] {})}))

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

(rf/reg-sub
 ::dirty?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _]
   (let [n-data (dissoc data :view-edit-unit-system)
         n-src-data (dissoc src-data :view-edit-unit-system)]
     (not= n-data n-src-data))))

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
 (fn [[dirty? valid?] _]
   (or dirty? (not valid?))))

(rf/reg-sub
 ::show-error? ;; used for hiding errors until first click on submit
 :<- [::component]
 (fn [component _] (:show-error? component)))

(rf/reg-sub
 ::unit-system
 (fn [db _](get db :unit-system)))

(rf/reg-sub
 ::all-unit-system
 :<- [::unit-system]
 :<- [::field [:plant-uoms]]
 :<- [::field [:user-uoms]]
 :<- [::ht-subs/topsoe?]
 (fn [[unit-system plant-uoms user-uoms topsoe?] _]
   (let [us unit-system
         us1 (if (:valid? plant-uoms)
               (assoc us "PLANT" {:id "PLANT" :name "PLANT" :uoms (:value plant-uoms)})
               (assoc us "PLANT" {:id "Plant" :name "PLANT" :uoms {}}))
         us2 (if (:valid? user-uoms)
               (assoc us1 "MY" {:id "MY" :name "MY" :uoms (:value
                                                               user-uoms)})
               (assoc us1 "MY" {:id "MY" :name "MY" :uoms {}}))]
		 (if topsoe?
			 us2
			 (dissoc us2 "TOPSOE")))))

(rf/reg-sub
 ::uom-data
 (fn [db _](get db :uom)))

(rf/reg-sub
 ::units
 :<- [::uom-data]
 :<- [::all-unit-system]
 :<- [::app-subs/active-unit-system]
 (fn [[uom-data all-unit-system active-unit-system] [_ specific-unit-system]]
   (let [target-unit-system (if specific-unit-system
                              specific-unit-system
                              active-unit-system)
         base-std (get-in all-unit-system ["US" :uoms])
         selected-std (get-in all-unit-system [target-unit-system :uoms])
         selected-uoms
         (reduce (fn [coll [k v]]
                   (let [base-unit (get-in base-std [k :unit-id])
                         selected-unit (get-in selected-std [k :unit-id])]
                     (if selected-unit
                       (assoc coll k (assoc v :selected-unit-id selected-unit))
                       (assoc coll k (assoc v :selected-unit-id base-unit)))))
                 {} uom-data)]
     selected-uoms)))

(rf/reg-sub
 ::editable?
 :<- [::field [:view-edit-unit-system]]
 (fn [view-edit-unit-system _]
   (let [sus (:value view-edit-unit-system)]
     (or (= sus "US")
         (= sus "SI")
         (= sus "TOPSOE")))))

(rf/reg-sub
 ::disable-save-default-unit-system?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _]
   (= (:default-unit-system data) (:default-unit-system src-data))))

(rf/reg-sub
 ::disable-save-view-edit-unit-system?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _]
   (let [n-data (-> data
                    (dissoc :default-unit-system)
                    (dissoc :view-edit-unit-system))
         n-src-data (-> src-data
                        (dissoc :default-unit-system)
                        (dissoc :view-edit-unit-system))]
     (= n-data n-src-data))))
