;; view elements dialog user-agreement
(ns cpe.dialog.user-agreement.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [cpe.app.style :as app-style]
            [cpe.app.subs :as app-subs]
            [cpe.app.event :as app-event]
            [cpe.dialog.user-agreement.style :as style]
            [cpe.dialog.user-agreement.subs :as subs]
            [cpe.dialog.user-agreement.event :as event]
            [clojure.string :as str]))

(defn user-agreement []
  (let [open? @(rf/subscribe [::subs/open?])]
    [ui/dialog
     {:open open?
      :modal true
      :title (translate [:app :disclaimer :title] "User Agreement")
      :actions
      [(r/as-element [ui/flat-button
                      {:label (translate [:app :disclaimer :accept] "I Agree")
                       :on-click #(rf/dispatch [::event/set-agreed? true])}])
       (r/as-element [ui/flat-button
                      {:label (translate [:app :disclaimer :reject] "I Disagree")
                       :on-click #(rf/dispatch [::event/set-agreed? false])}])]}
     (into [:div]
           (map
            #(conj [:p] %)
            (str/split-lines
             (translate
              [:app :disclaimer :description]
              "You accept that the TrueTemp™ is provided free of charge and “as is”, and you recognize that HTAS does not provide any warranty or representation of any kind, express or implied, and any warranties, representations, conditions or other terms that may be implied by statute or general law are, to the fullest extent permitted by law excluded, including, without limitation, any implied warranties of quality or fitness for purpose, the accuracy, completeness, or usefulness of the TrueTemp™.

For further details, please refer to the SOFTWARE LICENSE AGREEMENT with HTAS."))))]))
