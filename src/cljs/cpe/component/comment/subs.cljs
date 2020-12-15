;; subscriptions for component home
(ns cpe.component.comment.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.app.subs :as app-subs]
            [cpe.util.auth :as auth]
            [cpe.util.common :as au]
            [ht.util.auth :as ht-auth]
            [cpe.info :as info]))

(rf/reg-sub
  ::dirty?
  (fn [db [_ id]]
    (let [data  (get-in db [:component :comment :comments id :new-comment :data])
          src-data (get-in db [:component :comment :comments id :new-comment :src-data])]
      ;(print id "id-comment" data "data" src-data "src-data")
      (not= data src-data))))

(rf/reg-sub
  ::valid?
  (fn [db  [_ id]]
    (let [form   (get-in db [:component :comment :comments id :new-comment :form :subject :valid?])]
     form)))

;
;(rf/reg-sub
;  ::can-submit?
;  :<- [::dirty?]
;  :<- [::valid?]
;  (fn [[dirty? valid?] _] (and dirty? valid?)))


(fn has-access [claims op]
  (if (ht-auth/allow-operation? claims op info/operations)
    :enabled
    :disabled))



(rf/reg-sub
 ::access-rules
 :<- [::ht-subs/auth-claims]
 ;; valid values -  :enabled :disabled :hidden
 (fn [claims _]
   (let [f (fn [[id pub?]]
             [id (if (or pub? (:topsoe? claims))
                   (if (auth/allow-root-content? claims id)
                     :enabled
                     :disabled)
                   :hidden)])]
     {:card
      (->>
       {:dataset        true
        :trendline      true
        :settings       true
        :gold-cup       false
        :config-history false
        :config         false
        :logs           true}
       (map f)
       (into {}))
      :button
      (->> {:data-entry     true
            :import-logger  true
            :print-logsheet true}
           (map f)
           (into {}))})))

(rf/reg-sub
  ;"get all comment"
  ::comment
  (fn [db ]
    (get-in db [:component :comment])))

(rf/reg-sub
  ;"get reply by id"
  ::reply
  (fn [db [_ id]]
    (get-in db [:component :comment :comments id :new-comment :data :reply ])))

(rf/reg-sub
  ;"get email sub"
  ::email-sub
  :<- [::app-subs/user]
  (fn [user _]
    (get user :email-alert?)))