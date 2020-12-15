;; subscriptions for dialog user-agreement
(ns cpe.dialog.user-agreement.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.app.subs :as app-subs]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :user-agreement])))


;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
  (fn [dialog]
    (:open? dialog))) 

(rf/reg-sub
 ::data
 :<- [::dialog]
 (fn [dialog _]
   (:data dialog)))

(rf/reg-sub
 ::field
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:field id])))
