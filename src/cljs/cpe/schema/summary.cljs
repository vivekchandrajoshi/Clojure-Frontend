(ns cpe.schema.summary
  (:require [ht.util.schema :as u]))

(def schema
  {:summary {:id             u/id-field
             :plant-id       "plantId"
             :subject        "subject"
             :status         "status"
             :created-by     "createdBy"
             :date-created   (u/date-field "dateCreated")
             :modified-by    "modifiedBy"
             :date-modified  (u/date-field "dateModified")
             :published-by   "publishedBy"
             :date-published (u/date-field "datePublished")}})
