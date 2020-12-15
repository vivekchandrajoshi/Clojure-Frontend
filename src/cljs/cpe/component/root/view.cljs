(ns cpe.component.root.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as mic]
            [ht.util.interop :as i]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [cpe.util.auth :as auth]
            [cpe.app.icon :as ic]
            [cpe.app.comp :as app-comp]
            [cpe.app.style :as app-style]
            [cpe.app.subs :as app-subs]
            [cpe.component.uom.subs :as uom-subs]
            [cpe.component.uom.event :as uom-event]
            [cpe.app.event :as app-event]
            [cpe.app.view :as app-view]
            [cpe.component.root.style :as style]
            [cpe.component.root.subs :as subs]
            [cpe.component.root.event :as event]
            [cpe.dialog.user-agreement.view :refer [user-agreement]]
            [cpe.dialog.choose-client.view :refer [choose-client]]
            [cpe.dialog.choose-plant.view :refer [choose-plant]]
            [cpe.component.section.view :refer [section]]
            [cpe.component.settings.view :refer [settings]]
            [cpe.component.report-settings.view :refer [report-settings]]
            [cpe.component.uom.view :refer [uom]]))

;;; language-menu ;;;

(defn language-menu [props]
  (let [anchor-el (:language @(:anchors props))
        options @(rf/subscribe [::ht-subs/language-options])
        active @(rf/subscribe [::ht-subs/active-language])]
    [app-comp/popover
     {:open @(rf/subscribe [::subs/menu-open? :language])
      ;; this is a workaround to hide the initial flashing
      ;; :style {:position "fixed", :top 100000}
      :anchor-el anchor-el
      :anchor-origin {:horizontal "right"
                      :vertical "bottom"}
      :target-origin {:horizontal "right"
                      :vertical "top"}
      :on-request-close #(rf/dispatch [::event/set-menu-open? :language false])}
     (into [ui/menu {:value active}]
           (map (fn [{:keys [id name]}]
                  [ui/menu-item
                   {:primary-text name
                    :on-click #(do
                                 (rf/dispatch [::event/set-menu-open? :language false])
                                 (rf/dispatch [::ht-event/set-language id]))
                    :value id}])
                options))]))

;;; settings-menu

(defn fa-icon [class]
  (r/as-element
   [ui/font-icon {:class-name class}]))

(defn svg-icon [src]
  (r/as-element
   [:img {:src src}]))

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

(def settings-menu-data
  {:top    [{:id        :choose-plant
             :disabled? false
             :hidden?   false
             :icon      (as-left-icon ic/plant)
             :label-fn  #(translate [:root :menu :choose-plant] "Choose plant")
             :event-id  ::event/choose-plant}
            {:id       :my-apps
             :icon     (as-left-icon ic/my-apps)
             :label-fn #(translate [:root :menu :my-apps] "My apps")
             :event-id ::event/my-apps}]
   :bottom [{:id       :logout
             :icon     (as-left-icon ic/logout)
             :label-fn #(translate [:root :menu :logout] "Logout")
             :event-id ::event/logout}]
   :middle {:section cpe.component.section.view/context-menu
            :settings cpe.component.settings.view/context-menu
            :report-settings cpe.component.report-settings.view/context-menu
            :uom cpe.component.uom.view/context-menu}})

(defn settings-sub-menu [props]
  (let [{:keys [menu-items]} props]
    (doall
     (map (fn [{:keys [id event-id
                       disabled? hidden?
                       icon label-fn]}]
            (if-not hidden?
              [ui/menu-item
               {:key id
                :disabled disabled?
                :left-icon icon
                :primary-text (label-fn)
                :on-click #(do
                             (rf/dispatch [::event/set-menu-open? :settings false])
                             (rf/dispatch [event-id]))}]))
          menu-items))))

(defn settings-menu [props]
  (let [anchor-el (:settings @(:anchors props))
        content-id @(rf/subscribe [::subs/active-content])
        allow-content? @(rf/subscribe [::subs/content-allowed? content-id])
        context-menu (let [f (if allow-content?
                               (get-in settings-menu-data [:middle content-id]))]
                       (->> (if (fn? f) (f) f)
                            (remove nil?)
                            (not-empty)))]
    [app-comp/popover
     {:open @(rf/subscribe [::subs/menu-open? :settings])
      :desktop true
      ;; this is a workaround to hide the inital flashing
      ;; :style {:position "fixed" :top 10000}
      :anchor-el anchor-el
      :anchor-origin {:horizontal "right"
                      :vertical "bottom"}
      :target-origin {:horizontal "right"
                      :vertical "top"}
      :on-request-close #(rf/dispatch [::event/set-menu-open? :settings false])}
     [ui/menu
      ;; top section
      (settings-sub-menu {:menu-items (:top settings-menu-data)})

      ;; middle (context) section
      (if context-menu
        (list
         [ui/divider {:key :div-middle}]
         (settings-sub-menu {:key :middle-sub-menu
                             :menu-items context-menu})))

      ;; bottom section
      [ui/divider]
      (settings-sub-menu {:menu-items (:bottom settings-menu-data)})]]))

;;; header ;;;

(defn header []
  (let [anchors (atom {})
				auth-claims @(rf/subscribe [::ht-subs/auth-claims])
				help-app-subs? (or (:admin? auth-claims)
													 (some #(= "ha" %) (:apps-list auth-claims)))]
    (fn []
      [:div (use-style style/header)
       [:div (use-sub-style style/header :left)]
       [:div (use-sub-style style/header :middle)]
       [:div (use-sub-style style/header :right)
        (doall
         (map
          (fn [[id icon label action]]
            ^{:key id}
            [:a (merge (use-sub-style style/header :link) {:on-click action})
             (if icon
							 [icon {:style {:width "18px", :height "18px"}, :color "white"}])
             [:span (use-sub-style style/header :link-label) label]])
				 (->>          [
												;[:language
												; mic/action-translate
												; nil
												; #(do
												;    (i/ocall % :preventDefault)
												;    (swap! anchors assoc :language (i/oget % :currentTarget))
												;    (rf/dispatch [::event/set-menu-open? :language true]))]
												(if help-app-subs?
													[:help
													 mic/action-help-outline
													 nil
													 #(do
															(i/ocall % :preventDefault)
															(rf/dispatch [::event/open-help-app]))]
													[:div])
												[:settings
												 mic/navigation-arrow-drop-right
												 (translate [:header-link :settings :label] "Settings")
												 #(do
														(i/ocall % :preventDefault)
														(swap! anchors assoc :settings (i/oget % :currentTarget))
														(rf/dispatch [::event/set-menu-open? :settings true]))]]
											 (remove nil?)
											 (vec))))
        [language-menu {:anchors anchors}]
        [settings-menu {:anchors anchors}]]])))


;;; sub-header ;;;

(defn app-logo [props]
  [:div (assoc-in props [:style :user-select] "none")
   [:span {:style {:font-family "arial"
                   :font-weight "900"
                   :font-size "18px"}} "Clearview™"]
   #_[:span {:style {:font-weight "300"
                   :font-size "18px"}} "Performance Evaluation"]])

(defn hot-links []
  (let [section-list @(rf/subscribe [::subs/section-list])
        links (conj (map (fn [s] [(:id s) (:name s)]) section-list) ["home" "Overview"])
        active-content-id @(rf/subscribe [::subs/active-content-id])
        view-size @(rf/subscribe [::ht-subs/view-size])
        hot-links-style (style/hot-links view-size)]
    [:div (use-style hot-links-style)
     (doall
      (map-indexed
       (fn [index [id label]]
         (let [active? (= id active-content-id)]
           ^{:key index}
           [:a
            (merge (use-sub-style hot-links-style
                                  (if active? :active-link :link))
                   {:href "#"
                    :on-click (if-not active?
                                #(rf/dispatch [::event/activate-content :section nil id]))})
            label]))
       links))]))

(defn info [label text]
  [:div (use-style style/info)
   [:p (use-sub-style style/info :p)
    [:span (use-sub-style style/info :head)
     label]
    [:span (use-sub-style style/info :body)
     text]]])

(defn company-info []
  (let [client @(rf/subscribe [::app-subs/client])
        label (translate [:info :company :label] "Company")]
    (info label (:name client))))

(defn plant-info []
  (let [plant @(rf/subscribe [::app-subs/plant])
        label (translate [:info :plant :label] "Plant")]
    (info label (:name plant))))

;;; uom dropdown ;;;

(defn uom-menu [props]
	(let [anchor-el (:uom @(:anchors props))
				all-unit-system @(rf/subscribe [::uom-subs/all-unit-system])
				unit-system-options (reduce (fn [col unit-name]
																			(conj col {:name unit-name}))
																		[] (keys all-unit-system))]
		[app-comp/popover
		 {:open             @(rf/subscribe [::subs/uom-open? :uom])
			:desktop          true
			;; this is a workaround to hide the inital flashing
			;:style            {:position "fixed" :top 10000}
			:anchor-el        anchor-el
			:anchor-origin    {:horizontal "right"
												 :vertical   "bottom"}
			:target-origin    {:horizontal "right"
												 :vertical   "top"}
			:on-request-close #(rf/dispatch [::uom-event/set-uom-open? :uom false])}
		 [ui/menu
			(map-indexed (fn [i d]
										 [ui/menu-item
											{:key          i
											 :primary-text (d :name)
											 :on-click     #(rf/dispatch
																				[::uom-event/update-default-unit-system d])
											 }]
										 ) unit-system-options)]]))

(defn uom-drop-menu []
	(fn []
		(let [anchors (atom {})
					active-unit-system @(rf/subscribe [::app-subs/active-unit-system])]
			[:div
			 [app-comp/button {:disabled? false
												 :icon ic/balance-scale
												 :label active-unit-system
												 :on-click #(do
																			(i/ocall % :preventDefault)
																			(swap! anchors assoc :uom (i/oget % :currentTarget))
																			(rf/dispatch [::uom-event/set-uom-open? :uom true]))}]
			 [uom-menu {:anchors anchors}]])))


(defn sub-header []
	(let [topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
[:div
  [:div (use-style style/sub-header)
   [:div (use-sub-style style/sub-header :left)
    [app-logo (use-sub-style style/sub-header :logo)]
    [:div (use-sub-style style/sub-header :spacer)]
		[uom-drop-menu]
		[app-comp/button {:disabled? false
											:icon ic/save
											:label "Report"
											:on-click #(rf/dispatch [::event/download-report])}]
		(if topsoe?
			[app-comp/button {:disabled? false
												:icon ic/datasheet
												:label "Export Excel"
												:on-click #(rf/dispatch [::event/export-excel])}])]
   [:div (use-sub-style style/sub-header :right)
    [company-info]
    [plant-info]]]
  [hot-links]]))

;;; content ;;;

(defn no-access []
  [:div (use-style style/no-access)
   [:p (use-sub-style style/no-access :p)
    (translate [:root :no-access :message] "Insufficient rights!")]])

(defn content []
  ;;[:div "Display content here"]
  (let [view-size        @(rf/subscribe [::ht-subs/view-size])
        active-content   @(rf/subscribe [::subs/active-content])
        active-content-id   @(rf/subscribe [::subs/active-content-id])
        content-allowed? @(rf/subscribe [::subs/content-allowed?])
        content-height (app-style/content-height view-size)
				plant-id     ( @(rf/subscribe [::app-subs/plant]) :id)
        content-size {:width (:width view-size)
                      :height content-height}]
    [:div (update (use-style style/content) :style
                  assoc :height content-height)
     (if content-allowed?
       (cond
         (= active-content :section) [section {:on-select #(rf/dispatch [::event/activate-content %])
                                               :size      content-size}
																			active-content-id plant-id]
         (= active-content :report-settings) [report-settings {:on-select #(rf/dispatch [::event/activate-content %])
                                                               :size      content-size} active-content-id]
         (= active-content :settings) [settings {:on-select #(rf/dispatch [::event/activate-content %])
                                                 :size      content-size}]
         (= active-content :uom) [uom {:on-select #(rf/dispatch [::event/activate-content %])
                                       :size      content-size}]
         :else (rf/dispatch [::event/activate-content :section nil "home"]))
       [no-access])]))

;;; root ;;;

(defn root []
  (let [agreed? @(rf/subscribe [::subs/agreed?])
        client @(rf/subscribe [::app-subs/client])
        plant @(rf/subscribe [::app-subs/plant])
        app-allowed? @(rf/subscribe [::subs/app-allowed?])]
    [:div (use-style style/root)
     [header]
     (if app-allowed?
       (if (and agreed? client plant)
         (list ^{:key :sub-header} [sub-header {:key "sub-header"}]
               ^{:key :content} [content {:key "content"}]))
       [no-access])

     ;;dialogs
     (if @(rf/subscribe [:cpe.dialog.user-agreement.subs/open?])
       [user-agreement])
     (if @(rf/subscribe [:cpe.dialog.choose-client.subs/open?])
       [choose-client])
     (if @(rf/subscribe [:cpe.dialog.choose-plant.subs/open?])
       [choose-plant])]))
