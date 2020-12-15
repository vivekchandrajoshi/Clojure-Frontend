(ns cpe.util.service
  (:require [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [ht.config :refer [config]]
            [ht.util.service :refer [add-to-api-map run]]
            [cpe.schema.model :refer [from-api to-api]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn init []
  (add-to-api-map
   {:root (:service-uri @config)
    :api {:fetch-user "/api/user/:user-id"
          :create-user "/api/user"
          :update-user "/api/user/:user-id"
          :fetch-client-search-options "/api/client/search-options"
          :fetch-client "/api/client/:client-id"
          :search-clients "/api/client"
          :fetch-plant "/api/client/:client-id/plant/:plant-id"
          :fetch-client-plants "/api/client/:client-id/plant"
          :create-client "/api/client"
          :update-client "/api/client/:client-id"
          :create-plant "/api/client/:client-id/plant"
          :update-plant-config "/api/client/:client-id/plant/:plant-id/config"
          :update-plant-settings "/api/client/:client-id/plant/:plant-id/settings"
          :fetch-latest-dataset "/api/client/:client-id/plant/:plant-id/latest-dataset"
          :fetch-dataset "/api/client/:client-id/plant/:plant-id/dataset/:dataset-id"
          :create-dataset "/api/client/:client-id/plant/:plant-id/dataset"
          :update-dataset "/api/client/:client-id/plant/:plant-id/dataset/:dataset-id"
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;; cpe ;;;;;;;;;;;;;;;;;;;;
          :misc-report-date-range      "/api/misc/report-date-range"
          :comment                     "/api/plant/:plant-id/comment"
          :comment-with-replies        "/api/plant/:plant-id/comment/withReply"
          :comment-report-settings     "/api/plant/:plant-id/comment/includeInReportMulti"
          :comment-commentId           "/api/plant/:plant-id/comment/:comment-id"
          :reply                       "/api/plant/:plant-id/comment/:comment-id/reply"
          :include-in-report           "/api/plant/:plant-id/comment/:comment-id/includeInReport"
          :reply-commentTime           "/api/plant/:plant-id/comment/:comment-id/reply/commentTime"
					:read                        "/api/plant/:plant-id/comment/:comment-id/read"
          :fetch-chart                 "/api/chart"
          :fetch-misc                  "/api/misc"
          :fetch-section               "/api/section"
          :fetch-sensor                "/api/sensor"
          :fetch-sensor-values         "/api/sensor/values"
					:export-excel									"/api/sensor/export-excel"
          :fetch-uom                   "/api/uom"
          :fetch-unit-system           "/api/unitSystem"
          :summary                     "/api/plant/:plant-id/summary"
          :summary-id                  "/api/plant/:plant-id/summary/:summary-id"
          :publish-summary             "/api/plant/:plant-id/summary/:summary-id/publish"}}))


(defn dispatch-one [evt entity-key]
  #(rf/dispatch (conj evt (from-api entity-key (:result %)))))

(defn dispatch-many [evt entity-key]
  (fn [res]
    (rf/dispatch (conj evt (map #(from-api entity-key %)
                                (:result res))))))

(defn fetch-user [{:keys [user-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-user
        :api-params {:user-id user-id}
        :on-success (dispatch-one evt-success :user)
        :evt-failure evt-failure}))

(defn save-user [{:keys [user new? evt-success evt-failure]}]
  (run (merge {:data {:json-params (to-api :user user)}
               :evt-failure evt-failure}
              (if new?
                {:method http/post
                 :api-key :create-user
                 :on-success (dispatch-one evt-success :res/create)}
                {:method http/put
                 :api-key :update-user
                 :api-params {:user-id (:id user)}
                 :on-success (dispatch-one evt-success :res/update)}))))

(defn fetch-search-options [{:keys [evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-client-search-options
        :on-success (dispatch-one evt-success :client/search-options)
        :evt-failure evt-failure}))

(defn fetch-client [{:keys [client-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-client
        :api-params {:client-id client-id}
        :on-success (dispatch-one evt-success :client)
        :evt-failure evt-failure}))

(defn search-clients [{:keys [query evt-success evt-failure]}]
  (run {:method http/get
        :api-key :search-clients
        :data {:query-params (to-api :client/query query)}
        :on-success (dispatch-many evt-success :sap-client)
        :evt-failure evt-failure}))

(defn fetch-plant [{:keys [client-id plant-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-plant
        :api-params {:client-id client-id, :plant-id plant-id}
        :on-success (dispatch-one evt-success :plant)
        :evt-failure evt-failure}))

(defn fetch-client-plants [{:keys [client-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-client-plants
        :api-params {:client-id client-id}
        :on-success (dispatch-many evt-success :sap-plant)
        :evt-failure evt-failure}))

(defn save-client [{:keys [client new?
                           evt-success evt-failure]}]
  (run (merge {:data {:json-params (to-api :client client)}
               :evt-failure evt-failure}
              (if new?
                {:method http/post
                 :api-key :create-client
                 :on-success (dispatch-one evt-success :res/create)}
                {:method http/put
                 :api-key :update-client
                 :api-params {:client-id (:id client)}
                 :on-success (dispatch-one evt-success :res/update)}))))

(defn create-plant [{:keys [plant
                            client-id
                            evt-success evt-failure]}]
  (run {:method http/post
        :api-key :create-plant
        :api-params {:client-id client-id}
        :data {:json-params (to-api :plant plant)}
        :on-success (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))

(defn update-plant-config [{:keys [update-config
                                   client-id plant-id
                                   evt-success evt-failure]}]
  (run {:method http/put
        :api-key :update-plant-config
        :api-params {:client-id client-id, :plant-id plant-id}
        :data {:json-params (to-api :plant/update-config update-config)}
        :on-success (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn update-plant-settings [{:keys [update-settings
                                     client-id plant-id
                                     evt-success evt-failure]}]
  (run {:method http/put
        :api-key :update-plant-settings
        :api-params {:client-id client-id, :plant-id plant-id}
        :data {:json-params (to-api :plant/update-settings update-settings)}
        :on-success (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn fetch-latest-dataset [{:keys [client-id plant-id
                                    evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-latest-dataset
        :api-params {:client-id client-id, :plant-id plant-id }
        :on-success (dispatch-one evt-success :dataset)
        :evt-failure evt-failure}))

(defn fetch-dataset [{:keys [client-id plant-id dataset-id
                             evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-dataset
        :api-params {:client-id client-id
                     :plant-id plant-id
                     :dataset-id dataset-id}
        :on-success (dispatch-one evt-success :datset)
        :evt-failure evt-failure}))

(defn create-dataset [{:keys [dataset client-id plant-id evt-success evt-failure]}]
  (run {:method http/post
        :api-key :create-dataset
        :data {:json-params (to-api :dataset dataset)}
        :on-success (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))

;;;;;;;;;;;;;;;-cpe -;;;;;;;;;;;

(defn fetch-uom [{:keys [evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :fetch-uom
        :on-success  (dispatch-many evt-success :uom)
        :evt-failure evt-failure}))

(defn fetch-unit-system [{:keys [evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :fetch-unit-system
        :on-success  (dispatch-many evt-success :unit-system)
        :evt-failure evt-failure}))

(defn create-comment [{:keys [comment plant-id evt-success evt-failure]}]
	(run {:method      http/post
				:api-key     :comment
				:api-params  {:plant-id plant-id}
				:data        {:json-params (to-api :comment comment)}
				:on-success  (dispatch-one evt-success :res/create)
				:evt-failure evt-failure}))

(defn fetch-comment-id [{:keys [plant-id comment-id evt-success evt-failure]}]
	(run {:method      http/get
				:api-key     :comment-commentId
				:api-params  {:comment-id comment-id, :plant-id plant-id}
				:on-success  (dispatch-one evt-success :comment)
				:evt-failure evt-failure}))

(defn fetch-comment [{:keys [plant-id evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :comment
        :api-params  {:plant-id plant-id}
        :on-success  (dispatch-many evt-success :comment)
        :evt-failure evt-failure}))

(defn fetch-comments-with-replies [{:keys [plant-id evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :comment-with-replies
        :api-params  {:plant-id plant-id}
        :on-success  (dispatch-many evt-success :comment)
        :evt-failure evt-failure}))

(defn fetch-reply [{:keys [plant-id comment-id evt-success evt-failure]}]
	(run {:method      http/get
				:api-key     :reply
				:api-params  {:comment-id comment-id :plant-id plant-id}
				:on-success  (dispatch-many  evt-success :comment/reply)
				:evt-failure evt-failure}))

(defn add-reply [{:keys [comment-id plant-id comment evt-success evt-failure]}]
	(run {:method      http/put
				:api-key     :reply
				:data        {:json-params (to-api :comment/reply comment)}
				:api-params  {:plant-id   plant-id
											:comment-id comment-id}
				:on-success  (dispatch-one evt-success :res/update)
              :evt-failure evt-failure}))

(defn read [{:keys [comment-id plant-id  evt-success evt-failure]}]
	(run {:method      http/put
				:api-key     :read
				;:data        {:json-params (to-api :comment/reply comment)}
				:api-params  {:plant-id   plant-id
											:comment-id comment-id}
				:on-success  (dispatch-one evt-success :res/update)
				:evt-failure evt-failure}))

(defn include-in-report-permanently [{:keys [comment-id plant-id
                                             include-in-report-permanently evt-success
                                             evt-failure]}]
  (run {:method      http/put
        :api-key     :include-in-report
        :data        {:json-params (to-api :comment
                                           {:include-in-report-permanently
                                            include-in-report-permanently})}
        :api-params  {:comment-id comment-id :plant-id plant-id}
        :on-success  (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn update-report-date [{:keys [date evt-success evt-failure]}]
  (run {:method      http/put
        :api-key     :misc-report-date-range
        :data        {:json-params (to-api :misc
                                           {:data date})}
        :api-params  {}
        :on-success  (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn update-report-settings [{:keys [data evt-success evt-failure]}]
  (run {:method      http/put
        :api-key     :comment-report-settings
        :data        {:json-params (to-api :comment
                                           {:include-comments-in-report data})}
        :api-params  {}
        :on-success  (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn fetch-chart [{:keys [evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :fetch-chart
        :on-success  (dispatch-many evt-success :chart)
        :evt-failure evt-failure}))

(defn fetch-misc [{:keys [evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :fetch-misc
        :on-success  (dispatch-many evt-success :misc)
        :evt-failure evt-failure}))

(defn fetch-section [{:keys [evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :fetch-section
        :on-success  (dispatch-many evt-success :section)
        :evt-failure evt-failure}))

(defn fetch-sensor [{:keys [name evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :fetch-sensor
        :on-success  (dispatch-many evt-success :sensor)
        :evt-failure evt-failure}))

(defn fetch-reply-by-time [{:keys [plant-id comment-id comment-time evt-success evt-failure]}]
  (run {:method      http/get
				:api-key     :reply-commentTime
				:api-params  {:plant-id   plant-id
											:comment-id comment-id}
				 :data {:query-params (to-api :comment/query {:last-comment-date comment-time})}
				:on-success  (dispatch-one evt-success :comment)
				:evt-failure evt-failure}))



(defn fetch-sql-data[{:keys [plant-id start-date name evt-success evt-failure]}]
      (run {:method      http/get
            :api-key     :fetch-sensor-values
            :data    {:query-params (to-api  :sensor/query  {:plant-id plant-id
                                                          :start-date start-date
                                                          :name name})}
            :on-success  (dispatch-many evt-success :sensor-value)
            :evt-failure evt-failure}))

(defn fetch-export-excel-data[{:keys [plant-id start-date name evt-success evt-failure]}]
	(run {:method      http/get
				:api-key     :export-excel
				:data    {:query-params (to-api  :sensor/query  {:plant-id plant-id
																												 :start-date start-date
																												 :name name})}
				:on-success  (dispatch-many evt-success :export-data)
				:evt-failure evt-failure}))



;;;;;;;;;;;;;
;; summary ;;
;;;;;;;;;;;;;
(defn create-summary [{:keys [summary-data plant-id evt-success evt-failure]}]
  (run {:method      http/post
        :api-key     :summary
        :api-params  {:plant-id plant-id}
        :data        {:json-params (to-api :summary summary-data)}
        :on-success  (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))

(defn update-summary [{:keys [summary-id summary-data plant-id evt-success evt-failure]}]
  (run {:method      http/put
        :api-key     :summary-id
        :api-params  {:plant-id plant-id, :summary-id summary-id}
        :data        {:json-params (to-api :summary summary-data)}
        :on-success  (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))

(defn fetch-summary [{:keys [plant-id evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :summary
        :api-params  {:plant-id plant-id}
        :on-success  (dispatch-many evt-success :summary)
        :evt-failure evt-failure}))

(defn publish-summary [{:keys [summary-id plant-id evt-success evt-failure]}]
  (run {:method      http/put
        :api-key     :publish-summary
        :api-params  {:plant-id plant-id :summary-id summary-id}
        :data        {}
        :on-success  (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))

(defn delete-summary [{:keys [summary-id plant-id evt-success evt-failure]}]
  (run {:method      http/delete
        :api-key     :summary-id
        :api-params  {:plant-id plant-id :summary-id summary-id}
        :data        {}
        :on-success  (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))
