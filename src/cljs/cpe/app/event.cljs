(ns cpe.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.util.common :refer [dev-log]]
            [ht.app.event :as ht-event]
            [ht.app.subs :refer [translate]]
            [cpe.util.auth :as auth]
                                        ;[cpe.util.common :as au :refer [missing-field]]
            ))

;;;;;;;;;;;;;;
;; app init ;;
;;;;;;;;;;;;;;

(rf/reg-event-fx
 :app/init
 (fn [{:keys [db]} _]
   (let [claims (get-in db [:auth :claims])]
     (if (auth/allow-app? claims)
       {:dispatch-n (list
                     [::fetch-user (:id claims)]
                     [:cpe.app.event/chart]
                     [:cpe.app.event/misc]
                     [:cpe.app.event/section]
                     [:cpe.app.event/sensor])}))))

;;;;;;;;;;
;; user ;;
;;;;;;;;;;

(rf/reg-event-fx
 ::fetch-user
 (fn [_ [_ user-id]]
   (dev-log "fetch user: " user-id)
   {:service/fetch-user user-id
    :dispatch [::ht-event/set-busy? true]}))

(rf/reg-event-fx
 ::fetch-user-success
 (fn [_ [_ user]]
   {:dispatch-n (list [::set-user user]
                      [::ht-event/set-busy? false]
                      [::load-client])}))

(rf/reg-event-db
 ::set-user
 (fn [db [_ user]]
   (let [uoms     (get-in user [:uoms] [])
         new-uoms (reduce (fn [coll data]
                            (assoc coll (:uom-id data) data)) nil uoms)
         new-user (conj (if new-uoms
                          (assoc-in user [:uoms] new-uoms)
                          (dissoc user :uoms))
                        ) #_"force update user agreement"
         aus (:active-unit-system db)
         aus-nil? (nil? aus)]
     (if aus-nil?
       (-> db
           (assoc :user new-user)
           (assoc :active-unit-system (get-in user [:unit-system] "PLANT")))
       (assoc db :user new-user)))))

(rf/reg-event-fx
  ::set-user->db
  (fn [{:keys [db]} [_ new] ]
    (let [user-id (get-in db [:auth :claims :id])
          email-alert? (get-in db [:user :email-alert?] )
          plant-id  (get-in db [:user :plant-id])
          client-id (get-in db [:client :id])
          plants    (get-in db [:client :plants])
          plant-id  (or plant-id (if (= 1 (count plants))
                                   (:id (first plants))))
          user-data {:id user-id
                :client-id client-id
                :plant-id plant-id
                :agreed? true
                :email-alert? email-alert?}]
      {:service/save-user {:user user-data, :new? new
                           :evt-success [::save-user-success user-data]
                           :evt-failure [::ht-event/service-failure true]}
       :dispatch-n (list [::ht-event/set-busy? false]
                   [::fetch-plant (:client-id user-data) (:plant-id user-data)]) })))


(rf/reg-event-fx
  ::save-user-success
  (fn [{:keys [db]} [_ {:keys [user] }]]
    (let [{:keys [on-accept on-decline]} (get-in db [:dialog :user-agreement :then])]
      {:dispatch-n (list [::ht-event/set-busy? false])})))

;;;;;;;;;;;;;;;
;; agreement ;;
;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::check-agreement
 (fn [{:keys [db]} _]
   {:dispatch (if (or (:agreed? (:user db))
                      (:topsoe? @(rf/subscribe [:ht.app.subs/auth-claims])))
                [::load-client]
                [:cpe.dialog.user-agreement.event/open
                 {:then {:on-accept  [::load-client]
                         :on-decline [::ht-event/exit]}}])}))

;;;;;;;;;;;;
;; client ;;
;;;;;;;;;;;;

(rf/reg-event-fx
 ::load-client
 (fn [{:keys [db]} _]
   {:dispatch (if-let [client-id (or (get-in db [:auth :claims :client-id])
                                     (get-in db [:user :client-id]))]
                [::fetch-client client-id]
                [:cpe.dialog.choose-client.event/open])}))

(rf/reg-event-fx
 ::fetch-client
 (fn [_ [_ client-id]]
   (dev-log "fetch-client:" client-id)
   {:service/fetch-client client-id
    :dispatch             [::ht-event/set-busy? true]}))

(rf/reg-event-fx
 ::fetch-client-success
 (fn [_ [_ client]]
   {:dispatch-n (list [::set-client client]
                      [::ht-event/set-busy? false]
                      [::load-plant]
                      [::fetch-summary])}))

(rf/reg-event-db
 ::set-client
 (fn [db [_ client]]
   (assoc db :client client)))

;;;;;;;;;;;
;; plant ;;
;;;;;;;;;;;

(rf/reg-event-fx
  ::load-plant
  (fn [{:keys [db]} _]
    (let [user-id (get-in db [:auth :claims :id])
          email-alert? (get-in db [:user :email-alert?])
          plant-id  (get-in db [:user :plant-id])
          client-id (get-in db [:client :id])
          plants    (get-in db [:client :plants])
          plant-id  (or plant-id (if (= 1 (count plants))
                                   (:id (first plants))))
          new?    (nil? (get db :user) )]
      {:dispatch (if plant-id
                   [::set-user->db new?]
                   [:cpe.dialog.choose-plant.event/open])
       :db (-> db
               (assoc :user {:id user-id
                             :client-id client-id
                             :plant-id plant-id
                             :agreed? true
                             :email-alert? email-alert?}))})))

(rf/reg-event-fx
 ::fetch-plant
 (fn [_ [_ client-id plant-id]]
   (dev-log "fetch-plant:" client-id plant-id)
   {:service/fetch-plant {:client-id client-id
                          :plant-id  plant-id}
    :dispatch [::ht-event/set-busy? true]}))

(rf/reg-event-fx
 ::fetch-plant-success
 (fn [{:keys [db]} [_ plant]]
	 {:dispatch-n (list [::ht-event/set-busy? false]
											[::set-plant plant]
                      [:cpe.app.event/comment]
											[:cpe.app.event/fetch-summary])}))

(rf/reg-event-db
 ::set-plant
 (fn [db [_ plant]]
   (let [uoms (get-in plant [:settings :uoms] [])
         nu   (reduce (fn [coll data]
                        (assoc coll (:uom-id data) data)) {} uoms)
         np   (assoc-in plant [:settings :uoms] nu)]
     (assoc db :plant np))))

;;;;;;;;;;;;;;;
;; countries ;;
;;;;;;;;;;;;;;;

(rf/reg-event-db
 ::set-search-options
 (fn [db [_ options]]
   (assoc db :countries (:country options))))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CPE master data load ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::fetch-success
 (fn [{:keys [db]} [_ path result]]
   (if result
     (let [data (->> (map (fn [data]
                            (if (or (= path :misc) (= path :sensor) (= path :unit-system))
                              {(:name data) data}
                              {(:id data) data}
                              )
                            ) result)
                     (into {}))]
       {:db (-> db
                (assoc path data))}))))

(rf/reg-event-fx
  ::fetch-miss-success
  (fn [{:keys [db]} [_ result]]
    (if result
      (let [data (->> (map (fn [data]
                             {(:name data) data}
                             ) result)
                      (into {}))]
        {:dispatch-n (list [:cpe.app.event/uom]
                           [:cpe.app.event/unit-system])
         :db (-> db
                 (assoc :misc data))
        }))))

(defn sort-data [data]
  (let [sorted-data (sort-by :last-post > data)]
    sorted-data))

(defn prepare-comments-data [sorted-comments-data]
  (reduce (fn [col data]
            (let [id (:id data)
                  include-permanently (get data :include-in-report-permanently false)
                  include-in-report (get data :include-in-report false)
                  subject (:subject data)
                  title (if (> (count subject) 80)
                          (str (first (re-seq #".{1,80}" subject)) "...")
                          subject)
                  prepared-data (-> data
                                    (merge {:title title})
                                    (merge {:include-in-report-permanently
                                            include-permanently})
                                    (merge {:include-in-report include-in-report}))
                  new-data (-> prepared-data
                               (assoc-in [:new-comment :form :subject :value] nil)
                               (assoc-in [:new-comment :form :subject :valid?] false))]
              (assoc col id (merge new-data {:collapse? true}))))
          {}
          sorted-comments-data))

(rf/reg-event-fx
 ::fetch-comments-success
 (fn [{:keys [db]} [_ result]]
   (if (first result)
     (let [;;db-data (get-in db [:component :comment])
           ;;merged-data (into new-data db-data)
           new-data result
           sorted-data (sort-data new-data)
           sorted-comment-id (mapv (fn [data]
                                     (data :id))
                                   sorted-data)
           comments (prepare-comments-data sorted-data)]
       {:db (-> db
                (assoc-in [:component :comment :comments] comments)
                (assoc-in [:component :comment :sorted-comments-id] sorted-comment-id))}))))

(rf/reg-event-fx
 ::chart
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-chart
    {:evt-success [::fetch-success :chart]}}))

(rf/reg-event-fx
 ::misc
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-misc
    {:evt-success [::fetch-miss-success ]}}))

(rf/reg-event-fx
 ::misc-only
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-misc
    {:evt-success [::fetch-misc-success-only]}}))

(rf/reg-event-fx
 ::fetch-misc-success-only
 (fn [{:keys [db]} [_ result]]
   (if result
     (let [data (into {} (map (fn [data]
                                {(:name data) data}
                                ) result))]
       {:db (assoc db :misc data)}))))

(rf/reg-event-fx
 ::section
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-section
    {:evt-success [::fetch-success :section]}}))

(rf/reg-event-fx
 ::sensor
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-sensor
    {:evt-success [::fetch-success :sensor]}}))

(rf/reg-event-fx
 ::comment
 (fn [{:keys [db]} [_ set-busy?]]
   {:dispatch [::ht-event/set-busy? (if set-busy?
                                      set-busy?
                                      false)]
    :service/fetch-comment
    {:plant-id    (get-in db [:user :plant-id])
     :evt-success [::fetch-comments-success]}}))

(rf/reg-event-fx
 ::comments-with-replies
 (fn [{:keys [db]} [_ set-busy?]]
   {
    ;:dispatch [::ht-event/set-busy? (if set-busy?
    ;                                  set-busy?
    ;                                  false)]
    :service/fetch-comments-with-replies
    {:plant-id    (get-in db [:user :plant-id])
     :evt-success [::fetch-comments-success]}}))

(rf/reg-event-fx
 ::uom
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-uom
    {:evt-success [::fetch-uom-success]}}))

(rf/reg-event-fx
 ::unit-system
 (fn [_ _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-unit-system
    {:evt-success [::fetch-unit-system-success]}}))

(rf/reg-event-fx
 ::fetch-unit-system-success
 (fn [{:keys [db]} [_ result]]
   (if result
     (let [unit-list (get-in db [:misc "unitList" :data])
           data      (->> (map (fn [data]
                                 (let [uoms (get data :uoms)
                                       nu   (into {} (map (fn [d]
                                                            {(:uom-id d) d}) uoms))
                                       fnu  (reduce (fn [coll id]
                                                      (if (get nu id)
                                                        (assoc coll id (get nu id))
                                                        coll
                                                        )) {} unit-list)
                                       nd   (assoc data :uoms fnu)]
                                   {(:name data) nd}
                                   )
                                 ) result)
                          (into {}))]
       {:db (-> db
                (assoc :unit-system data))}))))

(rf/reg-event-fx
 ::fetch-uom-success
 (fn [{:keys [db]} [_ result]]
   (if result
     (let [data         (->> (map (fn [data]
                                    {(:id data) data}) result)
                             (into {}))
           unit-list    (get-in db [:misc "unitList" :data])
           filtered-uom (reduce (fn [coll id]
                                  (assoc coll id (get data id))
                                  ) {} unit-list)]
       {:db (-> db
                (assoc :uom filtered-uom))}))
   ))

(rf/reg-event-fx
 ::fetch-summary
 (fn [{:keys [db]} _]
   {:dispatch [::ht-event/set-busy? false]
    :service/fetch-summary
    {:plant-id    (get-in db [:user :plant-id])
     :evt-success [::fetch-summary-success]}}))

(rf/reg-event-fx
 ::fetch-summary-success
 (fn [{:keys [db]} [_ result]]
   (if result
     (let [data (->> (map (fn [data]
                            {(:id data) data})
                          result)
                     (into {}))]
       {:db (-> db
                (assoc  :summary data)
                (assoc-in [:component :section :data :summary-history]
                          data))}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Common events accross app ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::update-active-unit-system
 (fn [{:keys [db]} [_ sus]] ;; sus = selected unit system data
   {:db (assoc db :active-unit-system (:name sus))}))

(rf/reg-event-fx
 ::update-section-active-tab
 (fn [{:keys [db]} [_ tab-index]]
   {:db (assoc db :section-active-tab tab-index)}))

;;;;;;;;;;;;;;;
;; utilities ;;
;;;;;;;;;;;;;;;


(defn wrap-warning
  "Wraps the given event and returns a new event to show a warning
  dialog (unsaved changes!) before executing the given event."
  [next-event]
  [::ht-event/show-message-box
   {:message      (translate [:warning :unsaved :message]
                             "Unsaved changes will be lost!")
    :title        (translate [:warning :unsaved :title]
                             "Discard current changes?")
    :level        :warning
    :label-ok     (translate [:action :discard :label] "Discard")
    :event-ok     next-event
    :label-cancel (translate [:action :cancel :label] "Cancel")}])

(rf/reg-event-fx
 ::warning-for-delete
 (fn [_ [_ next-event]]
   {:dispatch [::ht-event/show-message-box
               {:message      "Are you sure, you want to delete?"
                :title        "Alert!"
                :level        :warning
                :label-ok     "Delete"
                :event-ok     next-event
                :label-cancel (translate [:action :cancel :label] "Cancel")}]}))

(rf/reg-event-fx
 ::open-uom
 (fn [_ _]
   {:dispatch [:cpe.component.root.event/activate-content :uom]}))

(rf/reg-event-fx
 ::open-settings
 (fn [_ _]
   {:dispatch [:cpe.component.root.event/activate-content :settings]}))

(rf/reg-event-fx
 ::open-report-settings
 (fn [_ _]
   {:dispatch [:cpe.component.root.event/activate-content :report-settings]}))
