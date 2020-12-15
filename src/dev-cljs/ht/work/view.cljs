;; view elements dialog work
(ns ht.work.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.app.icon :as ic]
            [ht.work.style :as style]
            [ht.work.subs :as subs]
            [ht.work.event :as event]
            [ht.app.comp :as ht-comp]
            [cpe.icon-set]
            [cpe.comp-set]
            [cpe.table-grid]
            [cpe.scroll]
            [cpe.ref-sketch]
            [cpe.calendar]
            [cpe.tube-list]
            [cpe.tube-prefs]
            [cpe.tab]
            [cpe.wall-list]
    ;[cpe.burner]
            ))

(defn work []
  (if @(rf/subscribe [::subs/open?])
    [ui/dialog
     {:open true, :modal true
      :title (r/as-element
              (ht-comp/optional-dialog-head
               {:title "Workspace"
                :on-close #(rf/dispatch [::event/close])
                :close-tooltip "close"}))}
     (case (:work-key @(rf/subscribe [::subs/data]))
       :cpe/icons [cpe.icon-set/icon-set]
       :cpe/comps [cpe.comp-set/comp-set]
       :cpe/scroll [cpe.scroll/scroll-test]
       :cpe/ref-sketch [cpe.ref-sketch/ref-sketch]
       :cpe/calendar [cpe.calendar/calendar-test]
       :cpe/datepickers [cpe.calendar/datepickers-test]
       :cpe/table [cpe.table-grid/table-grid-test]
       :cpe/tube-list [cpe.tube-list/tube-list]
       :cpe/tube-prefs [cpe.tube-prefs/tube-prefs]
       :cpe/tab [cpe.tab/tab]
       :cpe/wall-list [cpe.wall-list/wall-list]
       ;:cpe/burner [cpe.burner/burner]
       ;; default
       [:p "empty workspace"])]))
