;; view elements dialog choose-client
(ns cpe.dialog.choose-client.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.app.comp :as ht-comp]
            [cpe.app.style :as app-style]
            [cpe.app.subs :as app-subs]
            [cpe.app.event :as app-event]
            [cpe.app.view :as app-view]
            [cpe.dialog.choose-client.style :as style]
            [cpe.dialog.choose-client.subs :as subs]
            [cpe.dialog.choose-client.event :as event]))

(defn- query-field [id label]
  [ui/text-field
   {:on-change #(rf/dispatch [::event/update-query id %2])
    :default-value (:value @(rf/subscribe [::subs/field id]))
    :hint-text (translate [:choose-client :field :hint] "Start typing..")
    :floating-label-text label
    :name (name id)}])

(defn- select-field [id label options]
  (into
   [ui/select-field
    {:value (:value @(rf/subscribe [::subs/field id]))
     :on-change #(rf/dispatch [::event/update-query id %3])
     :floating-label-text label
     :name (name id)}
    [ui/menu-item {:key "_", :value ""}]]
   (map (fn [o]
          [ui/menu-item {:key o, :value o, :primary-text o}])
        options)))

(defn- toggle-field [id label]
  [ui/toggle
   (merge (use-style style/toggle-field)
          {:toggled (:value @(rf/subscribe [::subs/field id]))
           :on-toggle #(rf/dispatch [::event/update-query id %2])
           :label label
           :name (name id)})])

(defn choose-client []
  (let [busy? @(rf/subscribe [::subs/busy?])
        title (translate [:choose-client :title :text] "Choose client")
        on-close #(rf/dispatch [::event/close])
        close-tooltip (translate [:choose-client :close :hint] "Close")
        optional? (some? @(rf/subscribe [::app-subs/client]))]
    [ui/dialog
     {:modal (not optional?)
      :open @(rf/subscribe [::subs/open?])
      :on-request-close on-close
      :title (if optional? (r/as-element (ht-comp/optional-dialog-head
                                          {:title title
                                           :on-close on-close
                                           :close-tooltip close-tooltip}))
                 title)}

     [(if busy? ui/linear-progress :div) (use-style style/progress-bar)]

     ;; left pane - query fields
     [:div (use-style style/container)
      [:div (use-sub-style style/container :left)
       (select-field :country
                     (translate [:choose-client :country :label] "Country")
                     @(rf/subscribe [::app-subs/countries]))
       (query-field :name
                    (translate [:choose-client :name :label] "Name"))
       (query-field :short-name
                    (translate [:choose-client :short-name :label] "Short name"))
       (query-field :location
                    (translate [:choose-client :location :label] "Location"))
       (toggle-field :plant?
                     (translate [:choose-client :with-plant :label] "Only with plant?"))]

      ;; right pane - client selection
      [:div (use-sub-style style/container :right)
       (let [clients @(rf/subscribe [::subs/clients])
             list-style (use-style (style/client-selector
                                    @(rf/subscribe [::ht-subs/view-size])))]
         (-> [ui/list list-style
              (if (zero? (count clients))
                [ui/list-item
                 {:disabled true
                  :secondary-text (translate [:choose-client :no-clients :text]
                                             "No clients found!")}])]
             (into (map (fn [c]
                          [ui/list-item
                           {:primary-text (:name c)
                            :secondary-text-lines 2
                            :secondary-text
                            (r/as-element
                             [:p (:short-name c) [:br]
                              (str (:location c) " - " (:country c))])
                            :on-click #(rf/dispatch [::event/select-client c])}])
                        clients))
             (conj (if (and (not busy?) @(rf/subscribe [::subs/more?]))
                     [ui/list-item
                      {:secondary-text (translate [:choose-client :more :text]
                                                  "Show more..")
                       :on-click #(rf/dispatch [::event/search-more-clients])}]))))]]]))
