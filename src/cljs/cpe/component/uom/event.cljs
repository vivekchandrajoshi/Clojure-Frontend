;; events for component uom
(ns cpe.component.uom.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [cpe.component.settings.event :as settings]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]
            [cpe.app.event :as app-event]
            [cpe.app.subs :as app-subs]
            [cpe.component.uom.subs :as subs]))

(defonce comp-path [:component :uom])
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [{:keys [default-unit-system view-edit-unit-system
                 user-uoms plant-uoms]} @(rf/subscribe [::subs/data])]
     {:dispatch-n
      (list [::set-field [:default-unit-system] default-unit-system true]
            [::set-field [:view-edit-unit-system] view-edit-unit-system false]
            [::set-field [:plant-uoms] plant-uoms true]
            [::set-field [:user-uoms] user-uoms true])})))

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field db path value data data-path form-path required?))))

(rf/reg-event-fx
 ::update-default-unit-system
 (fn [{:keys [db]} [_ sus]]    ;; sus = selected unit system value in drop drown
   (let [data @(rf/subscribe [::subs/data])]
     {:db (assoc-in db data-path (assoc data :default-unit-system (:name sus)))
      :dispatch-n (list [::set-field [:default-unit-system] (:name sus) true]
                        [::upload-default-unit-system]
                        [::app-event/update-active-unit-system sus]
                        [::set-uom-open? :uom false]
                        [::settings/init])})))

(rf/reg-event-fx
 ::update-view-edit-unit-system
 [(inject-cofx ::inject/sub [::subs/disable-save-view-edit-unit-system?])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/disable-save-view-edit-unit-system? ::subs/data]} [_ sus]]
   ;; sus = selected unit system value in drop drown
   (let [show-warning? (not disable-save-view-edit-unit-system?)]
     (if show-warning?
       {:dispatch (app-event/wrap-warning [::discard-changes sus])}
       {:db (assoc-in db data-path (assoc data :view-edit-unit-system (:name sus)))
        :dispatch-n (list [::set-field [:view-edit-unit-system] (:name sus) false])}))))

(rf/reg-event-fx
 ::update-uom
 [(inject-cofx ::inject/sub [::subs/unit-system])
  (inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/field [:view-edit-unit-system]])]
 (fn [{:keys [db ::subs/unit-system ::subs/data ::subs/field]}
     [_ uom-id selected-unit-data]]
   (let [unit-id (:id selected-unit-data)
         us (get-in unit-system ["US" :uoms])
         diff? (if (=  (get-in us [uom-id :unit-id]) unit-id)
                 false true)
         selected-unit-system (:value field)
         target-uom (case selected-unit-system
                      "PLANT" :plant-uoms
                      "MY" :user-uoms
                      nil)
         target-uom-value (get-in data [target-uom])
         new-target-uom-value (if diff?
                                (-> target-uom-value
                                    (dissoc uom-id)
                                    (assoc uom-id {:uom-id uom-id :unit-id unit-id}))
                                (-> target-uom-value
                                    (dissoc uom-id)))]
     (if target-uom
       {:db (assoc-in db data-path (assoc data target-uom new-target-uom-value))
        :dispatch-n (list [::set-field [target-uom] new-target-uom-value true])}))))

(rf/reg-event-fx
 ::discard-changes
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/src-data])]
 (fn [{:keys [db ::subs/data ::subs/src-data]} [_ sus]]
   (let [user-uoms (:user-uoms src-data)
         plant-uoms (:plant-uoms src-data)
         new-data (-> data
                      (assoc :user-uoms user-uoms)
                      (assoc :plant-uoms plant-uoms)
                      (assoc :view-edit-unit-system (:name sus)))]
     {:db (assoc-in db data-path new-data)
      :dispatch-n (list [::set-field [:user-uoms] user-uoms true]
                        [::set-field [:plant-uoms] plant-uoms true]
                        [::set-field [:view-edit-unit-system] (:name sus) false])})))

(rf/reg-event-fx
 ::upload-default-unit-system
 [(inject-cofx ::inject/sub [::app-subs/user])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::app-subs/user ::subs/data]} _]
   (let [sus (:default-unit-system data)
         new? (nil? user)
         user-uoms (mapv (fn [[_ v]] v) (:uoms user))
         user (-> user
                  (assoc :unit-system sus)
                  (assoc :uoms user-uoms))]
     {:forward-events {:register ::sync-user-after-save
                       :events #{::app-event/fetch-user-success
                                 ::ht-event/service-failure}
                       :dispatch-to [::sync-user-after-save :default-unit-system false]}
      :dispatch [::ht-event/set-busy? true]
      :service/save-user
      {:user user
       :new? new?
       :evt-success [::app-event/fetch-user (:id user)]}})))

(rf/reg-event-fx
 ::upload-unit-system
 [(inject-cofx ::inject/sub [::subs/field [:view-edit-unit-system]])
  (inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::app-subs/user])
  (inject-cofx ::inject/sub [::app-subs/client])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::subs/field ::subs/data ::app-subs/user,
             ::app-subs/client, ::app-subs/plant]} _]
   (case (:value field)
     "MY" (let [new? (nil? user)
                  user-uoms (mapv (fn [[_ v]] v) (:user-uoms data))
                  user (assoc user :uoms user-uoms)]
              {:forward-events {:register ::sync-user-after-save
                                :events #{::app-event/fetch-user-success
                                          ::ht-event/service-failure}
                                :dispatch-to [::sync-user-after-save :user-uoms false]}
               :dispatch [::ht-event/set-busy? true]
               :service/save-user
               {:user user
                :new? new?
                :evt-success [::app-event/fetch-user (:id user)]}})
     "PLANT" (let [plant-uoms (mapv (fn [[_ v]] v) (:plant-uoms data))]
               {:forward-events {:register ::sync-plant-after-save
                                 :events #{::app-event/fetch-plant-success
                                           ::ht-event/service-failure}
                                 :dispatch-to [::sync-plant-after-save false]}
                :dispatch [::ht-event/set-busy? true]
                :service/update-plant-settings
                {:client-id (:id client)
                 :plant-id (:id plant)
                 :settings {:uoms plant-uoms}
                 :evt-success [::app-event/fetch-plant (:id client) (:id plant)]}})
     nil)))

(rf/reg-event-fx
 ::sync-user-after-save
 [(inject-cofx ::inject/sub [::app-subs/user])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::app-subs/user ::subs/data]} [_ sync-key close? [eid]]]
   (let [success? (= eid ::app-event/fetch-user-success)
         sync-data (case sync-key
                     :default-unit-system (:unit-system user)
                     :user-uoms (:uoms user)
                     nil)]
     (cond-> {:forward-events {:unregister ::sync-user-after-save}}
       ;; sync data on success
       success?
       (assoc :db (assoc-in db data-path (assoc data sync-key sync-data)))
       ;; leave if asked for
       (and success? close?)
       (assoc :dispatch [:cpe.component.root.event/activate-content :section nil "home"])))))

(rf/reg-event-fx
 ::sync-plant-after-save
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::subs/data ::app-subs/plant]} [_ close? [eid]]]
   (let [success? (= eid ::app-event/fetch-plant-success)]
     (cond-> {:forward-events {:unregister ::sync-plant-after-save}}
       ;; sync data on success
       success?
       (assoc :db (assoc-in db data-path
                            (assoc data :plant-uoms
                                   (get-in plant [:settings :uoms]))))
       ;; leave if asked for
       (and success? close?)
       (assoc :dispatch [:cpe.component.root.event/activate-content :section nil "home"])))))

;used for change header uom popover open/close
(rf/reg-event-db
  ::set-uom-open?
  (fn [db [_ id open?]]
    (assoc-in db [:component :root :uom-menu id :open?] open?)))