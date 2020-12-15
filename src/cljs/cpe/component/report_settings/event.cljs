;; events for component report-settings
(ns cpe.component.report-settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [vimsical.re-frame.cofx.inject :as inject]
            [cpe.component.report-settings.subs :as subs]
            [ht.app.event :as ht-event]
            [ht.util.schema :as u]
            [cpe.app.event :as app-event]
            [cpe.app.subs :as app-subs]
            [cpe.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(defonce comp-path [:component :report-settings])
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [{:keys [report-date-range comments-to-include]}
         @(rf/subscribe [::subs/data])]
     {:dispatch-n
      (list                                       ;[::ht-event/set-busy? true]
            [::app-event/comments-with-replies]
            [::set-field [:report-date-range] report-date-range false]
            [::set-field [:comments-to-include] comments-to-include false])})))

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field db path value data data-path form-path required?))))

(rf/reg-event-fx
 ::update-include-comments-from-date
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ date]]
   (let [year (:year date)
         month (:month date)
         day (:day date)
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ;; Setting hours, minute and second to fix because:         ;;
         ;; to validate dirty? check, if user changes the date and   ;;
         ;; then choose the same previous date as stored in database ;;
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         hour "00";;(:hour date)
         minute "01";;(:minute date)
         second "00";;(:second date)
         combine-date (str year "/" month  "/" day " " hour ":" minute ":" second)
         formated-date (u/format-date (js/Date. combine-date))]
     {:db (assoc-in db data-path (assoc data :report-date-range formated-date))})))

(rf/reg-event-fx
 ::update-include-comment-in-report
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ comment-id include-in-report]]
   {:db (assoc-in db data-path (assoc data
                                      :comments-to-include
                                      (assoc (:comments-to-include data)
                                             comment-id
                                             {:id comment-id
                                              :include-in-report
                                              include-in-report})))}))

(rf/reg-event-fx
 ::upload-report-settings
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/src-data])]
 (fn [{:keys [db ::subs/data ::subs/src-data]} _]
   (let [updated-date? (if (= (:report-date-range src-data)
                              (:report-date-range data))
                         false true)
         updated-comments? (if (= (:comments-to-include src-data)
                                  (:comments-to-include data))
                             false true)
         modified-comments (reduce (fn [col [k v]]
                                     (let [src-value (get-in src-data
                                                             [:comments-to-include
                                                              k
                                                              :include-in-report])
                                           data-value (get-in data
                                                              [:comments-to-include
                                                               k
                                                               :include-in-report])]
                                       (if (not= src-value data-value)
                                         (conj col v)
                                         col)))
                                   []
                                   (:comments-to-include data))]
     (if updated-comments?
       {:forward-events {:register ::sync-report-settings-after-save
                         :events #{::app-event/fetch-comments-success
                                   ::ht-event/service-failure}
                         :dispatch-to [::sync-report-settings-after-save
                                       :comments-to-include
                                       false]}
        :dispatch [::ht-event/set-busy? true]
        :service/update-report-settings
        {:data modified-comments
         :evt-success [::app-event/comment true]}}
       (if updated-date?
         {:forward-events {:register ::sync-report-date-after-save
                           :events #{::app-event/fetch-misc-success-only
                                     ::ht-event/service-failure}
                           :dispatch-to [::sync-report-date-after-save
                                         :report-date-range
                                         false
                                         modified-comments]}
          :dispatch [::ht-event/set-busy? true]
          :service/update-report-date
          {:date (:report-date-range data)
           :evt-success [::app-event/misc-only]}}
         {:dispatch [::ht-event/set-busy? false]})))))

(rf/reg-event-fx
 ::sync-report-date-after-save
 [(inject-cofx ::inject/sub [::app-subs/report-date-range])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::app-subs/report-date-range ::subs/data]} [_ sync-key close? [eid]]]
   (let [success? (= eid ::app-event/fetch-misc-success-only)
         sync-data report-date-range]
     (cond-> {:forward-events {:unregister ::sync-report-date-after-save}}
       ;; sync data on success
       success?
       (assoc :db (assoc-in db data-path (assoc data sync-key sync-data)))
       ;; leave if asked for
       (and success? close?)
       (assoc :dispatch [:cpe.component.root.event/activate-content :section nil "home"])))))

(rf/reg-event-fx
 ::sync-report-settings-after-save
 [(inject-cofx ::inject/sub [::app-subs/comments])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::app-subs/comments ::subs/data]} [_ sync-key close? [eid]]]
   (let [success? (= eid ::app-event/fetch-comments-success)
         sync-data (reduce (fn [col [k v]]
                             (assoc col (:id v)
                                    {:id (:id v)
                                     :include-in-report (:include-in-report v)}))
                           {}
                           comments)]
     (cond-> {:forward-events {:unregister ::sync-report-settings-after-save}}
       ;; sync data on success
       success?
       (->
        (assoc :db (assoc-in db data-path (assoc data sync-key sync-data)))
        (assoc :dispatch [::upload-report-settings]))
       ;; leave if asked for
       (and success? close?)
       (assoc :dispatch [:cpe.component.root.event/activate-content :section nil "home"])))))
