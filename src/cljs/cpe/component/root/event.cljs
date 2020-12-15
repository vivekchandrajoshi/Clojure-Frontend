(ns cpe.component.root.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.config :refer [config]]
            [cpe.app.event :as app-event]
            [cpe.component.root.subs :as subs]
            [cpe.util.report :refer [download]]))

(def all-contents
  {:settings {:event/init [:cpe.component.settings.event/init]
              :subs/warn-on-close? [:cpe.component.settings.subs/warn-on-close?]
              :event/close [:cpe.component.settings.event/close]}
   :uom {:event/init [:cpe.component.uom.event/init]
         :subs/warn-on-close? [:cpe.component.uom.subs/warn-on-close?]
         :event/close [:cpe.component.uom.event/close]}
   :section {:event/init [:cpe.component.section.event/init]
             :event/close [:cpe.component.section.event/close]}
   :report-settings {:event/init [:cpe.component.report-settings.event/init]
                     :subs/warn-on-close? [:cpe.component.report-settings.subs/warn-on-close?]
                     :event/close [:cpe.component.report-settings.event/close]}
   :config-history {}
   :logs {}})

(rf/reg-event-fx
 ::with-warning-for-unsaved-changes
 (fn [_ [_ next-event]]
   (let [warn? (as-> @(rf/subscribe [::subs/active-content]) $
                 (get-in all-contents [$ :subs/warn-on-close?])
                 (if $ @(rf/subscribe $)))]
     {:dispatch (cond-> next-event
                  warn? app-event/wrap-warning)})))

(rf/reg-event-fx
 ::activate-content
 (fn [_ [_ id params section-id]]
   (let [cid @(rf/subscribe [::subs/active-content])]
     {:dispatch [::with-warning-for-unsaved-changes
                 [::close-and-init cid id params section-id]]})))

(rf/reg-event-fx
 ::close-and-init ;; close the current, init and open the next
 (fn [{:keys [db]} [_ close-id init-id init-params section-id]]
   (let [{:keys [event/close]} (get all-contents close-id)
         {:keys [event/init]} (if-let [e (get all-contents init-id)]
                                (update e :event/init conj init-params))]
     {:db (-> db
              (assoc-in [:component :root :content :id] section-id)
              (assoc-in [:component :root :content :active] init-id))
      :dispatch-n (list init close)})))

(rf/reg-event-db
 ::set-menu-open?
 (fn [db [_ id open?]]
   (assoc-in db [:component :root :menu id :open?] open?)))

(rf/reg-event-fx
 ::choose-plant
 (fn [_ _]
   {:dispatch [::with-warning-for-unsaved-changes [::choose-plant-continue]]}))

(rf/reg-event-fx
 ::choose-plant-continue
 (fn [{:keys [db]} _]
   (let [cid @(rf/subscribe [::subs/active-content])
				 active-content-id   @(rf/subscribe
																 [::subs/active-content-id])
				 active-content   @(rf/subscribe
															[::subs/active-content])]
     {:dispatch-n (list [::close-and-init cid active-content nil
												 active-content-id]
                        [:cpe.dialog.choose-plant.event/open])
      :db (assoc db :old-plant (get-in db [:plant :id]  ))})))

(rf/reg-event-fx
 ::logout
 (fn [_ _]
   {:dispatch [::with-warning-for-unsaved-changes [::ht-event/logout]]}))

(rf/reg-event-fx
 ::my-apps
 (fn [_ _]
   {:dispatch [::with-warning-for-unsaved-changes [::ht-event/exit]]}))


(rf/reg-event-fx
  ::download-report
  (fn [{:keys [_]} [_ ]]
    {:dispatch-n (list
                   [::ht-event/set-busy? true]
                   [:cpe.component.report-settings.event/init]
                   [:cpe.component.section.event/fetch-sql-data nil :report])}))

(rf/reg-event-fx
  ::export-excel
  (fn [{:keys [_]} [_ ]]
    {:dispatch-n (list
                   [::ht-event/set-busy? true]
                   [:cpe.component.section.event/excel-export])}))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; help app in new tab ;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(rf/reg-event-fx
  ::open-help-app
  (fn [_ _]
    (let [link (str (:portal-uri @config) "/apps/ha/")]
      (js/console.log "NOTE: Enable the blocked popup to see the helpApp")
      {:service/new-link-tab link})))
