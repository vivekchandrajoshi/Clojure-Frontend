(ns cpe.app.comp
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.app.subs :refer [translate]]
            [ht.app.style :as ht-style]
            [cpe.app.style :as app-style]
            [cpe.app.icon :as ic]
            [cpe.app.scroll :as scroll :refer [scroll-box scroll-bar]]
            [ht.util.common :as u]
            ))

(defn popover [props & children]
  (into [ui/popover (merge props (use-style app-style/popover))]
        children))

;; 72x48
(defn toggle [{:keys [disabled? value on-toggle]}]
  (let [on? value
        style (app-style/toggle on? disabled?)]
    [:span (-> (use-style style)
               (assoc :on-click
                      (if-not disabled?
                        #(on-toggle (not on?)))))
     [:div (use-sub-style style :main)
      [:span (use-sub-style style :label)
       (if on?
         (translate [:ht-comp :toggle :on] "on")
         (translate [:ht-comp :toggle :off] "off"))]
      [:div (use-sub-style style :circle)]]]))

;; 48x48, icon: 24x24, back: 32x32
(defn icon-button-l [{:keys [disabled? icon on-click tooltip]}]
  [ui/icon-button {:disabled disabled?
                   :on-click on-click
                   :tooltip tooltip
                   :tooltip-position "top-left"
                   :style {:vertical-align "top"}}
   [icon (-> (use-style (app-style/icon-button disabled?))
             (update :style assoc :padding "4px"
                     :width "32px", :height "32px"
                     :margin "-4px"))]])

;; 48x48, icon: 24x24
(defn icon-button [{:keys [disabled? icon on-click tooltip]}]
  [ui/icon-button {:disabled disabled?
                   :on-click on-click
                   :tooltip tooltip}
   [icon (use-style (app-style/icon-button disabled?))]])

(defn icon-button-pencil [{:keys [disabled? icon  on-click tooltip]}]
  [ui/icon-button {:disabled disabled?
                   :on-click on-click
                   :tooltip tooltip
                   }
   [:span (merge (use-style (app-style/icon-button disabled?) {:class "fa fa-pencil"}))]])

;; 48x48, icon: 22x22
(defn icon-button-s [{:keys [disabled? icon on-click tooltip]}]
  [ui/icon-button {:disabled disabled?
                   :tooltip tooltip
                   :on-click on-click}
   [icon (-> (use-style (assoc (app-style/icon-button disabled?)
                               :width "22px" :height "22px"
                               :margin "1px"))
             (assoc :view-box "1 1 22 22"))]])

;; *x48
(defn button [{:keys [disabled? label icon on-click]}]
  (let [style (app-style/button disabled?)]
    [:span (use-style (:container style))
     [ui/flat-button {:style (:btn style)
                      :disabled disabled?
                      :on-click on-click
                      :hover-color (:hc style)
                      :background-color (:bg style)}
      [:div (use-style (:div style))
       [icon (use-style (:icon style))]
       [:span (use-style (:span style)) label]]]]))

;; *x48
(defn selector
  "options: list of items, selected: selected item  
  on-select: (fn [selected-item])  
  label-fn: (fn [item]) should return the label to display"
  [{:keys [disabled? valid? item-width selected options on-select label-fn]
    :or {valid? true, label-fn identity, item-width 48}}]
  (let [index (some #(if (= selected (first %)) (second %))
                    (map list options (range)))
        style (app-style/selector disabled? valid?)]
    [:span (use-style style)
     (into
      [:div (update (use-sub-style style :main) :style assoc
                    :width (+ 10 (* item-width (count options))))
       (if index
         [:div (update (use-sub-style style :marker) :style assoc
                       :width item-width
                       :left (+ 4 (* index item-width)))])]
      (map (fn [o i]
             [:span (-> (use-sub-style style
                                       (if (= i index) :active-label
                                           :label))
                        (update :style assoc
                                :left (+ 4 (* i item-width))
                                :width item-width)
                        (assoc :on-click (if-not disabled? #(on-select o i))))
              (label-fn o)])
           options (range)))]))

;; *x48
(defn text-input [{:keys [read-only? valid? align width value on-change]
                   :or {valid? true, width 96, align "left"}}]
  (let [style (app-style/text-input read-only? valid?)]
    [:span (use-style style)
     [:input (-> (use-sub-style style :main)
                 (update :style assoc
                         :width width
                         :text-align align)
                 (merge {:type "text"
                         :value (or value "")
                         :on-change #(on-change (i/oget-in % [:target :value]))
                         :read-only read-only?}))]]))

(defn action-label-box [{:keys [width label
                                left-icon left-action left-disabled?
                                right-icon right-action right-disabled?]}]
  (let [style (app-style/action-label-box left-disabled? right-disabled?)
        align (if (and left-icon right-icon) "center" "left")]
    [:span (use-style style)
     [:div (use-sub-style style :main)
      (if left-icon
        [left-icon (merge (use-sub-style style :left)
                          {:on-click (if-not left-disabled? left-action)})])
      [:span (-> (use-sub-style style :span)
                 (update :style assoc
                         :width width
                         :text-align align))
       label]
      (if right-icon
        [right-icon (merge (use-sub-style style :right)
                           {:on-click (if-not right-disabled? right-action)})])]]))

;; *x48
(defn action-input-box [{:keys [disabled? valid? width label action
                                left-icon left-action left-disabled?
                                right-icon right-action right-disabled?]
                         :or {valid? true}}]
  (let [style (app-style/action-input-box disabled? valid? (some? action)
                                          left-disabled? right-disabled?)
        left-disabled? (or disabled? left-disabled?)
        right-disabled? (or disabled? right-disabled?)
        align (if (and left-icon right-icon) "center" "left")]
    [:span (use-style style)
     [:div (use-sub-style style :main)
      (if left-icon
        [left-icon (merge (use-sub-style style :left)
                          {:on-click (if-not left-disabled? left-action)})])
      [:span (-> (use-sub-style style :span)
                 (update :style assoc
                         :width width
                         :text-align align)
                 (merge {:on-click (if-not disabled? action)}))
       label]
      (if right-icon
        [right-icon (merge (use-sub-style style :right)
                           {:on-click (if-not right-disabled? right-action)})])]]))

;; *x48
(defn dropdown-selector [props]
  (let [state (r/atom {})]
    (r/create-class
     {:component-did-mount (fn [this]
                             (swap! state assoc :anchor (dom/dom-node this)))
      :reagent-render
      (fn [{:keys [disabled? valid? width on-select
                  left-icon left-action left-disabled?
                   scroll? selected items value-fn label-fn disabled?-fn]
           :or {valid? true, disabled?-fn (constantly false)
                value-fn identity, label-fn identity scroll? false}}]
        (let [{:keys [open? anchor]} @state
              action #(do
                        (i/ocall % :preventDefault)
                        (swap! state assoc :open? true))]
          [:span {:style {:display "inline-block"
                          :padding "0"
                          :vertical-align "top"}}
           [action-input-box
            (cond-> {:disabled? disabled?
                     :valid? valid?
                     :label (label-fn selected)
                     :width width
                     :action action
                     :right-icon ic/dropdown
                     :right-action action}
              left-icon (assoc :left-icon left-icon
                               :left-action left-action
                               :left-disabled? left-disabled?))]
           (if open?
             [popover {:open true
                       :on-request-close #(swap! state assoc :open? false)
                       :anchor-el anchor
                       :anchor-origin {:horizontal "right", :vertical "bottom"}
                       :target-origin {:horizontal "right", :vertical "top"}
                       }

              (if scroll?
                (into
                  [scroll-box {:style {:height 300 :width 250}}
                   [ui/menu {:value (value-fn selected)
                             :menu-item-style {:font-size "12px"
																							}}]
                   (map (fn [item i]
                          [ui/menu-item
                           {:key i
                            :primary-text (label-fn item)
                            :on-click #(do
                                         (swap! state assoc :open? false)
                                         (on-select item i))
                            :disabled (disabled?-fn item)
                            :value (value-fn item)
                            :style {:font-size "12px"
                                    :font-family "open_sans"
                                    :line-height "40px !important"
                                    :min-height "48px !important"
                                    :padding "4px 16px 4px 0"
                                    :color (if (= (value-fn selected) (value-fn item))
                                             "rgb(245, 81, 151)")
                                    }}])
                        items (range))])
                (into
                   [ui/menu {:value (value-fn selected)
                             :menu-item-style {:font-size "12px"
                                               :line-height "24px"
                                               :min-height "24px"}}]
                   (map (fn [item i]
                          [ui/menu-item
                           {:key i
                            :primary-text (label-fn item)
                            :on-click #(do
                                         (swap! state assoc :open? false)
                                         (on-select item i))
                            :disabled (disabled?-fn item)
                            :value (value-fn item)
                            }])
                        items (range))))])]))})))

(defn text-area [props]
  (let [txt-ref (atom nil)
        state (r/atom {:height 84, :page-h 68, :page-f 1, :pos-f 0})
        update-state (fn [state props]
                       (let [{:keys [pos-f]} state
                             {:keys [height]} props
                             height (- height 16)
                             page-h (- height 16)
                             sh (i/oget @txt-ref :scrollHeight)
                             page-f (/ page-h sh)
                             y (* pos-f (- sh page-h))
                             y (if (pos? y) y 0)]
                         (i/oset @txt-ref :scrollTop y)
                         (assoc state :height height
                                      :page-h page-h, :page-f page-f)))
        on-scroll (fn [pos-f]
                    (let [{:keys [page-h]} @state
                          sh (i/oget @txt-ref :scrollHeight)
                          y (* pos-f (- sh page-h))
                          y (if (pos? y) y 0)]
                      (i/oset @txt-ref :scrollTop y)
                      (swap! state assoc :pos-f pos-f)))
        scroll-bar (fn []
                     (let [{:keys [height page-f pos-f]} @state]
                       (if (and page-f (< page-f 1))
                         [scroll/scroll-bar {:length height
                                             :page-f page-f
                                             :pos-f pos-f
                                             :on-scroll on-scroll}])))
        on-txt-scroll (fn []
                        (let [sh (i/oget @txt-ref :scrollHeight)
                              y (i/oget @txt-ref :scrollTop)
                              {:keys [page-h]} @state
                              pos-f (if (= sh page-h) 0 (/ y (- sh page-h)))
                              pos-f (max 0 (min 1 pos-f))]
                          (swap! state (fn [state]
                                         (let [e (- pos-f (:pos-f state))]
                                           (if (< -1e-6 e 1e-6)
                                             state (assoc state :pos-f pos-f)))))))
        on-wheel (fn [e]
                   (let [dy (i/oget e :deltaY)
                         sh (i/oget @txt-ref :scrollHeight)
                         y (+ dy (i/oget @txt-ref :scrollTop))
                         {:keys [page-h page-f pos-f]} @state]
                     (if (and (> 1 page-f)
                           (if (pos? dy) (> 1 pos-f) (< 0 pos-f)))
                       (let [pos-f (if (= sh page-h) 0 (/ y (- sh page-h)))
                             pos-f (max 0 (min 1 pos-f))
                             y (* pos-f (- sh page-h))]
                         (i/ocall e :preventDefault)
                         (i/ocall e :stopPropagation)
                         (i/oset @txt-ref :scrollTop y)
                         (swap! state assoc :pos-f pos-f)))))]

    (r/create-class
      {:component-did-mount
       (fn [this]
         (u/add-event (dom/dom-node this) "wheel" on-wheel)
         (swap! state update-state (r/props this)))
       :component-did-update
       (fn [this _]
         (swap! state update-state (r/props this)))
       :reagent-render
       (fn [{:keys [read-only? valid? value
                    on-change align width height]
             :or {valid? true, width 96, height 72, align "left"}}]
         (let [style (app-style/text-area read-only? valid?)
               hdiv (- height 16), wdiv (- width 24)
               htxt (- hdiv 16), wtxt (- wdiv 24)]
           [:span (use-style style)
            [:div (update (use-sub-style style :div) :style assoc
                          :height hdiv, :width wdiv :background-color "#fff")
             [:textarea (-> (use-sub-style style :main)
                            (update :style assoc
                                    :width wtxt, :height htxt
                                    :text-align align)
                            (merge {:ref #(reset! txt-ref %)
                                    :on-scroll on-txt-scroll
                                    :type "text"
                                    :value (or value "")
                                    :on-change #(on-change (i/oget-in % [:target :value]))
                                    :read-only read-only?}))]
             [scroll-bar]]]))})))






