(ns cpe.core
  (:require [ht.core :as ht]
            [cpe.app.db :as db]
            [cpe.util.service :as svc]
            [cpe.app.cofx] ;; ensure load
            [cpe.app.fx]   ;; ensure load
            [cpe.app.view] ;; ensure load
            [cpe.component.root.view :refer [root]]))

(js/console.log "!VERSION!")

(def mount-root (ht/create-root-mounter root))

(defn ^:export init []
  (ht/init)
  (db/init)
  (ht/init-db @db/default-db)
  (svc/init)
  (mount-root))
