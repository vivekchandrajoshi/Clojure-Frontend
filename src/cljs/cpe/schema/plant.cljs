(ns cpe.schema.plant
  (:require [cpe.schema.sap-plant :as sap-plant]
            [cpe.schema.uoms :as uoms]
            [ht.util.schema :as u]))

(def schema
  (let [settings {:uoms           {:name   "uoms"
                                  :schema uoms/schema
                                  :array? true}
                  :pinned-charts  "pinnedCharts"
                  :charts-config  "chartsConfig"
                  :raw-tags   "rawTags"
                  :calculated-tags "calculatedTags"
                  :constant       "constant"
                  :created-by     "createdBy"
                  :date-created   (u/date-field "dateCreated")
                  :modified-by    "modifiedBy"
                  :date-modified  (u/date-field "dateModified")}

        config {:sql-plant-id  "sqlPlantId"
                :section       "section"
                :constant       "constant"
                :date-sor      (u/date-field "dateSOR")
                :date-eor      (u/date-field "dateEOR")
                :history-sor   "historySOR"
                :created-by    "createdBy"
                :date-created  (u/date-field "dateCreated")
                :modified-by   "modifiedBy"
                :date-modified (u/date-field "dateModified")}]

    {:plant (merge (into {}
                         (map (fn [[k f]]
                                [k (if (map? f)
                                     (assoc f :scope #{:api})
                                     {:name f, :scope #{:api}})])
                              (:sap-plant sap-plant/schema)))
                   {:id            u/id-field
                    :client-id     "clientId"
                    :name          "name"
                    :settings      {:name   "settings"
                                    :schema settings}
                    :config        {:name   "config"
                                    :schema config}
                    :created-by    "createdBy"
                    :date-created  (u/date-field "dateCreated")
                    :modified-by   "modifiedBy"
                    :date-modified (u/date-field "dateModified")})
     :plant/update-settings ^:api {:modified-by   "modifiedBy"
                                   :date-modified (u/date-field "dateModified")
                                   :settings  {:name   "settings"
                                               :schema settings}}

     :plant/update-config ^:api {:modified-by   "modifiedBy"
                                 :date-modified (u/date-field "dateModified")
                                 :config    {:name   "config"
                                             :schema config}}
     }))