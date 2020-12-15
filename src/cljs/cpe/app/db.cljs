(ns cpe.app.db
  (:require [cpe.info :refer [about features operations]]))

(defonce default-db
  (atom
   {:about nil
    :features nil
    :operations nil
    :countries []

    ;; entities
    :user nil
    :client nil
    :plant      nil
    :section    nil
    :chart     	nil
    :misc       nil
    :uom    nil
    :unit-system nil
    :active-unit-system nil
    :section-active-tab 0
    :settings-active-tab 0
    :summary {}
    :component
    {:root {:header {}
            :sub-header {}
            :content {:id nil
                      :active :init}}
     :section {}
     :comment {}
     :report-settings {}
     :chart-sql-data {}

     ;; secondary
     :settings {}
     :config-history {}
     :uom {}
     :config {}
     :logs {}}

    :dialog
    {:user-agreement
     {:open? false}
     :choose-client
     {:open? false}
     :choose-plant
     {:open? false}
     :add-comment
     {:open? false}
     :edit-pyrometer
     {:open? false}
     :custom-emissivity
     {:open? false}}}))




(defn init []
  (swap! default-db
         (fn [db]
           (-> db
               (assoc :about about
                      :features features
                      :operations operations)))))
