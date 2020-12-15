;; events for dialog add_comment
(ns cpe.dialog.add-comment.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [cpe.app.event :as app-event]
            [cpe.dialog.add-comment.subs :as subs]
            [cpe.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-decimal set-field-number
                                            validate-field parse-value]]
            [cpe.app.subs :as app-subs]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(defonce ^:const dlg-path [:dialog :add-comment])
(defonce ^:const data-path (conj dlg-path :data))
(defonce ^:const form-path (conj dlg-path :form))

(rf/reg-event-fx
  ::open
  (fn [{:keys [db]}]

    {:dispatch [::validate-comment]
     :db       (assoc-in db dlg-path
                 {:open? true})}))

(rf/reg-event-db
  ::validate-comment
  (fn [db []]
    (assoc-in db (conj form-path :subject) (missing-field))))

(rf/reg-event-db
  ::close
  (fn [db [_ options]]
    (update-in db [:dialog :add-comment] merge options {:open?       false
                                                        :data        {}
                                                        :form        {}
                                                        :show-error? false})
    ))

(rf/reg-event-fx
  ::set-field
  [(inject-cofx ::inject/sub [::subs/data])]
  (fn [{:keys [db ::subs/data]} [_ path value required?]]
    {:db (set-field db path value data data-path form-path required?)}))


(rf/reg-event-db
  ::set-data
  (fn [db [_ data]]
    (assoc-in db [:dialog :add-comment :data] data)))

(rf/reg-event-db
  ::set-options
  (fn [db [_ options]]
    (update-in db [:dialog :add-comment] merge options)))

(rf/reg-event-fx
  ::comment
  [(inject-cofx ::inject/sub [::subs/data])]
  (fn [{:keys [db ::subs/data]} [_ value]]
    {:db (set-field-text db [:subject]
           value data data-path form-path true)}))

(rf/reg-event-fx
  ::set-run-day
  [(inject-cofx ::inject/sub [::subs/data])]
  (fn [{:keys [db ::subs/data]} [_ path value required?]]
    {:db (set-field-number db path
           value data data-path form-path false)}))

(rf/reg-event-fx
  ::fetch-success
  (fn [{:keys [db]} [_ comment]]
    (if comment
      {:dispatch-n (list
                     [:cpe.dialog.add-comment.event/close]
                     [:cpe.dialog.add-comment.event/fetch-comments (:new-id comment)]
                     [::ht-event/set-busy? false])
       :db         (-> db
                     (assoc-in data-path nil)
                     (assoc-in form-path nil))})))

(rf/reg-event-fx
  ::fetch-comments-success
  (fn [{:keys [db]} [_ result]]
    (if (first result)
      (let [db-data           (get-in db [:component :comment :comments])
            new-data          (merge result {:collapse? true})
            merged-data       (if new-data
                                (assoc db-data (:id new-data) new-data)
                                db-data)
            data-to-sort      (mapv (fn [[_ v]] v) merged-data)
            sorted-data       (app-event/sort-data data-to-sort)
            sorted-comment-id (mapv (fn [data]
                                      (data :id))
                                sorted-data)
            comments (app-event/prepare-comments-data sorted-data)]
        {:db (-> db
               (assoc-in [:component :comment :comments] comments)
               (assoc-in [:component :comment :sorted-comments-id] sorted-comment-id))}))))

(rf/reg-event-fx
  ::save
  [(inject-cofx ::inject/sub [::subs/can-submit?])
   (inject-cofx ::inject/sub [::subs/data])
   (inject-cofx ::inject/sub [::app-subs/plant])]
  (fn [{:keys [db
               ::subs/can-submit?
               ::subs/data
               ::app-subs/plant]} _]
    (if can-submit?
      {:dispatch [::ht-event/set-busy? true]
       :service/create-comment
                 {:comment     data
                  :plant-id    (plant :id)
                  :evt-success [::fetch-success]}}
      {:db (update-in db dlg-path assoc :show-error? true)})))

(rf/reg-event-fx
  ::fetch-comments
  [(inject-cofx ::inject/sub [::app-subs/plant])]
  (fn [{:keys [db ::app-subs/plant]} [_ comment-id]]
    {:service/fetch-comment-id
     {:plant-id    (:id plant)
      :comment-id  comment-id
      :evt-success [::fetch-comments-success]}}))
