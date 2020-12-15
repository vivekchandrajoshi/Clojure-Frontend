(ns cpe.app.fx
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [ht.app.event :as ht-event]
            [cpe.util.service :as svc]
            [cpe.app.event :as event]
						[ht.util.interop :as i]
            [cpe.dialog.choose-client.event :as cc-event]))

(rf/reg-fx
 :service/fetch-user
 (fn [user-id]
   (svc/fetch-user
    {:user-id user-id
     :evt-success [::event/fetch-user-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/save-user
 (fn [{:keys [user new? evt-success evt-failure]}]
   (svc/save-user
    {:user user
     :new? new?
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-client
 (fn [client-id]
   (svc/fetch-client
    {:client-id client-id
     :evt-success [::event/fetch-client-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/search-clients
 (fn [{:keys [query evt-success evt-failure]}]
   (svc/search-clients
    {:query query
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-plant
 (fn [{:keys [client-id plant-id]}]
   (svc/fetch-plant
    {:client-id client-id
     :plant-id plant-id
     :evt-success [::event/fetch-plant-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/save-client
 (fn [{:keys [client new? evt-success evt-failure]}]
   (svc/save-client
    {:client client
     :new? new?
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/create-plant
 (fn [{:keys [client-id plant evt-success evt-failure]}]
   (svc/create-plant
    {:client-id client-id
     :plant plant
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-plant-config
 (fn [{:keys [client-id plant-id change-id config
             evt-success evt-failure]}]
   (svc/update-plant-config
    {:client-id client-id
     :plant-id plant-id
     :update-config {:change-id change-id
                     :config config}
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-plant-settings
 (fn [{:keys [client-id plant-id change-id settings
             evt-success evt-failure]}]
   (svc/update-plant-settings
    {:client-id client-id
     :plant-id plant-id
     :update-settings {:settings settings}
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-dataset
 (fn [{:keys [client-id plant-id dataset-id
             evt-success evt-failure]}]
   (svc/fetch-dataset
    {:client-id client-id
     :plant-id plant-id
     :dataset-id dataset-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/search-dataset
 (fn [{:keys [client-id plant-id from-data to-date]}]
   ))

(rf/reg-fx
 :service/fetch-latest-dataset
 (fn [{:keys [client-id plant-id evt-success evt-failure]}]
   (svc/fetch-latest-dataset
    {:client-id client-id
     :plant-id plant-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/create-dataset
 (fn [{:keys [dataset client-id plant-id evt-success evt-failure]}]
   (svc/create-dataset
    {:client-id client-id
     :plant-id plant-id
     :dataset dataset
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

;;;;;;;;;;;;;;;-cpe;;;;;;;;;;;;;;;;;;

(rf/reg-fx
	:service/create-comment
	(fn [{:keys [comment plant-id evt-success evt-failure]}]
		(svc/create-comment
			{:comment     comment
			 :plant-id    plant-id
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-comment
 (fn [{:keys [plant-id evt-success evt-failure]}]
   (svc/fetch-comment
    {:plant-id    plant-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-comments-with-replies
 (fn [{:keys [plant-id evt-success evt-failure]}]
   (svc/fetch-comments-with-replies
    {:plant-id    plant-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
	:service/fetch-comment-id
	(fn [{:keys [plant-id comment-id evt-success evt-failure]}]
		(svc/fetch-comment-id
			{:plant-id plant-id
			 :comment-id  comment-id
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))

  (fn [{:keys [plant-id evt-success evt-failure]}]
    (svc/fetch-comment
      {:plant-id    plant-id
       :evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])}))

(rf/reg-fx
  :service/fetch-chart
  (fn [{:keys [evt-success evt-failure]}]
    (svc/fetch-chart
      {:evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
  :service/fetch-misc
  (fn [{:keys [evt-success evt-failure]}]
    (svc/fetch-misc
      {:evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
  :service/fetch-section
  (fn [{:keys [evt-success evt-failure]}]
    (svc/fetch-section
      {:evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
  :service/fetch-sensor
  (fn [{:keys [evt-success evt-failure]}]
    (svc/fetch-sensor
      {:evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
	:service/fetch-reply
	(fn [{:keys [plant-id comment-id evt-success evt-failure]}]
		(svc/fetch-reply
			{:plant-id plant-id
			 :comment-id  comment-id
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
	:service/add-reply
	(fn [{:keys [plant-id comment-id comment evt-success evt-failure]}]
		(svc/add-reply
			{:plant-id plant-id
			 :comment-id comment-id
			 :comment comment
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
	:service/read
	(fn [{:keys [plant-id comment-id  evt-success evt-failure]}]
		(svc/read
			{:plant-id plant-id
			 :comment-id comment-id
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/include-in-report-permanently
 (fn [{:keys [plant-id comment-id include-in-report-permanently evt-success evt-failure]}]
   (svc/include-in-report-permanently
    {:plant-id plant-id
     :comment-id comment-id
     :include-in-report-permanently include-in-report-permanently
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-report-date
 (fn [{:keys [date evt-success evt-failure]}]
   (svc/update-report-date
    {:date date
     :evt-success  evt-success
     :evt-failure  (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-report-settings
 (fn [{:keys [data evt-success evt-failure]}]
   (svc/update-report-settings
    {:data data
     :evt-success  evt-success
     :evt-failure  (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
	:service/fetch-reply-by-time
	(fn [{:keys [plant-id comment-id comment-time evt-success evt-failure]}]
		(svc/fetch-reply-by-time
			{:plant-id plant-id
			 :comment-id comment-id
			 :comment-time comment-time
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))




(rf/reg-fx
  :service/fetch-sql-data
  (fn [{:keys [plant-id start-date name evt-success evt-failure]}]
      (svc/fetch-sql-data
        {:plant-id plant-id
         :start-date start-date
         :name name
         :evt-success evt-success
         :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
	:service/fetch-export-excel-data
	(fn [{:keys [plant-id start-date name evt-success evt-failure]}]
		(svc/fetch-export-excel-data
			{:plant-id plant-id
			 :start-date start-date
			 :name name
			 :evt-success evt-success
			 :evt-failure (or evt-failure [::ht-event/service-failure false])})))


(rf/reg-fx
  :service/fetch-uom
  (fn [{:keys [evt-success evt-failure]}]
    (svc/fetch-uom
      {:evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
  :service/fetch-unit-system
  (fn [{:keys [evt-success evt-failure]}]
    (svc/fetch-unit-system
      {:evt-success evt-success
       :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-summary
 (fn [{:keys [plant-id evt-success evt-failure]}]
   (svc/fetch-summary
    {:plant-id    plant-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/create-summary
 (fn [{:keys [subject plant-id evt-success evt-failure]}]
   (svc/create-summary
    {:summary-data {:subject subject}
     :plant-id     plant-id
     :evt-success  evt-success
     :evt-failure  (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-summary
 (fn [{:keys [summary-id subject plant-id evt-success evt-failure]}]
   (svc/update-summary
    {:summary-data {:subject subject}
     :summary-id summary-id
     :plant-id     plant-id
     :evt-success  evt-success
     :evt-failure  (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/publish-summary
 (fn [{:keys [summary-id plant-id evt-success evt-failure]}]
   (svc/publish-summary
    {:summary-id summary-id
     :plant-id     plant-id
     :evt-success  evt-success
     :evt-failure  (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/delete-summary
 (fn [{:keys [summary-id plant-id evt-success evt-failure]}]
   (svc/delete-summary
    {:summary-id summary-id
     :plant-id     plant-id
     :evt-success  evt-success
     :evt-failure  (or evt-failure [::ht-event/service-failure false])})))

;;;;;;;;;;;;;;;;;;;;;;;
;; open link new tab ;;
;;;;;;;;;;;;;;;;;;;;;;;
(rf/reg-fx
	:service/new-link-tab
	(fn [link]
		(i/ocall js/window :open link "_blank")))