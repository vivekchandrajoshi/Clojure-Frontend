(ns cpe.util.auth
  (:require [ht.util.auth :as ht-auth]
            [cpe.info :as info]))

(defn allow-app? [claims]
  (and
   (ht-auth/allow-feature? claims :standard)
   (ht-auth/allow-operation? claims :view info/operations)))

(defn allow-settings? [claims]
  (ht-auth/allow-operation? claims :modifyPlantSettings info/operations))

(defn allow-goldcup? [claims]
  (and (:topsoe? claims)
       (ht-auth/allow-operation? claims :editDataset info/operations)))

(defn allow-config-history? [claims]
  (:topsoe? claims))

(defn allow-config? [claims]
  (and (:topsoe? claims)
       (ht-auth/allow-operation? claims :configureReformer info/operations)))

(defn allow-data-entry? [claims]
  (or (ht-auth/allow-operation? claims :editDataset info/operations)
      (ht-auth/allow-operation? claims :createDataset info/operations)))

(defn allow-import-logger? [claims]
  (and (ht-auth/allow-feature? claims :importDataset)
       (allow-data-entry? claims)))

(defn allow-root-content?
  [claims content-id]
  ;;TODO: apply access-rules
  (case content-id
    :init                       true
    :section                    true
    :settings                   true
    :report-settings            true
    :uom                        true
    false))


(defn allow-edit-dataset? [claims]
  (ht-auth/allow-operation? claims :editDataset info/operations))

(defn allow-delete-dataset? [claims]
  (ht-auth/allow-operation? claims :deleteDataset info/operations))

(defn allow-export? [claims]
  (ht-auth/allow-operation? claims :export info/operations))

(defn allow-modify-uom-settings? [claims]
  (ht-auth/allow-operation? claims :modifyUOMSettings info/operations))
