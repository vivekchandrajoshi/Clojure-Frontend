;; view elements dialog add_comment
(ns cpe.dialog.add-comment.view
	(:require [reagent.core :as r]
						[re-frame.core :as rf]
						[stylefy.core :as stylefy :refer [use-style use-sub-style]]
						[cljs-react-material-ui.reagent :as ui]
						[ht.util.common :refer [to-date-time-map from-date-time-map]]
						[ht.app.style :as ht-style]
						[ht.app.subs :as ht-subs :refer [translate]]
						[ht.util.interop :as i]
						[ht.app.event :as ht-event]
						[ht.app.comp :as ht-comp]
						[cpe.app.comp :as app-comp]
						[cpe.app.style :as app-style]
						[cpe.app.subs :as app-subs]
						[cpe.app.icon :as ic]
						[cpe.app.event :as app-event]
						[cpe.app.calendar :as app-calender]
						[cpe.dialog.add-comment.style :as style]
						[cpe.dialog.add-comment.subs :as subs]
						[cpe.dialog.add-comment.event :as event]))

(defn show-error? []
	@(rf/subscribe [::subs/show-error?]))

(defn query-id [query-id & params]
	(let [{:keys [value error valid?]} @(rf/subscribe (into [query-id] params))
				show-err? (show-error?)]
		{:value value
		 :error (if show-err? (if (fn? error) (error) error))
		 :valid? (if show-err? valid? true)}))

(defn form-cell
	([style skey label widgets]
	 [:div (use-sub-style style skey)
		[:span (use-sub-style style :form-label) label]
		(into [:div] widgets)])
	([style skey error label widget]
	 [:div (use-sub-style style skey)
		[:span (use-sub-style style :form-label) label]
		widget
		[:span (use-sub-style style :form-error) error]]))

(defn form-cell-1
	([style label widgets] (form-cell style :form-cell-1 label widgets))
	([style error label widget] (form-cell style :form-cell-1 error label widget)))

(defn form-cell-2
	([style label widgets] (form-cell style :form-cell-2 label widgets))
	([style error label widget] (form-cell style :form-cell-2 error label widget)))

(defn run-day [{:keys [style label value-sub max data-key]}]
	(let [selected-data @(rf/subscribe [::subs/selected-data [:data :chart-info :chart-id]])]
		[form-cell-2 style nil
		 label
		 [app-comp/text-input {:value @(rf/subscribe value-sub)
																:valid?    true
																:on-change #(rf/dispatch [::event/set-run-day data-key %])
																}]]))

(defn dropdown [{:keys [style label opts-sub selected-sub data-key scroll?]}]
	(let [opts @(rf/subscribe opts-sub)
				selected @(rf/subscribe selected-sub)
				on-select #(rf/dispatch [::event/set-field data-key %])]
		[form-cell-2 style nil
		 label
		 [app-comp/dropdown-selector {:width      150
																	:item-width 250
																	:selected   selected
																	:items      opts
																	:scroll?    scroll?
																	:value-fn   :id
																	:on-select  on-select
																	:label-fn   :name}]]))

(defn comment-text [{:keys [style label event-data sub-data]}]
	(let [{:keys [value error valid?]} (query-id ::subs/field [:subject])]
		[form-cell-1 style error label
		 [app-comp/text-area
						 {:value     value
							:height     100
							:width     (get-in style [::stylefy/sub-styles :data :c-w-1])
							:on-change #(rf/dispatch [::event/comment %])
							:valid?    valid?
							}]]))





(defn form [props]
	(let [state (r/atom {:width 600})]
		(r/create-class
			{:component-did-mount
			 (fn [this]
				 (swap! state assoc
								:width (i/oget-in this [:refs :container :offsetWidth])))
			 :reagent-render
			 (fn []
				 (let [{:keys [width]} @state
							 style (style/body width)]
					 [:div {:ref "container"}
						[:div
						 (map (fn [data i]
										^{:key i}
										[dropdown data]
										) [											 {:style        style
												:label        "Chart"
												:opts-sub     [::subs/chart-name]
												:selected-sub [::subs/selected-data [:data :chart-info :chart-id]]
												:data-key     [:chart-info :chart-id]
												:scroll?      true
												}
											 {:style        style
												:label        "Status"
												:scroll?      false
												:opts-sub     [::subs/status]
												:selected-sub [::subs/selected-data [:data :type-id]]
												:data-key     [:type-id]}] (range))]
						[:div
						 (map (fn [data i]
										^{:key i}
										[run-day data]
										) [{:style        style
												:label        "Start of run day"
												:value-sub [::subs/date-of-sor [:data :chart-info :start-of-run-day]]
												:data-key     [:chart-info :start-of-run-day]}
											 {:style        style
												:label        "End of run day"
												:value-sub [::subs/date-of-sor [:data :chart-info :end-of-run-day]]
												:data-key     [:chart-info :end-of-run-day]}] (range))]
						[:div [comment-text {:style      style
																 :label      "Comment"
																 :event-data [:subject]
																 :sub-data   [:subject]
																 }]]]))})))



(defn dialog [props]
	(let [open? @(rf/subscribe [::subs/open?])
				on-close #(rf/dispatch [::event/close])
				h (get props :height)
				w (get props :width)
				style (style/comment-block h w)]
		[ui/dialog {:open  open?
								:modal true
								:title "Add Comment"
								:actions
											 [(r/as-element
													[:div
													 [app-comp/button
														{:disabled? (if (show-error?)
																					(not @(rf/subscribe [::subs/can-submit?]))
																					(not @(rf/subscribe [::subs/dirty?])))
														 :icon      ic/save
														 :label     "Save"
														 :on-click  #(rf/dispatch [::event/save])}]
													 [app-comp/button
														{:icon     ic/cancel
														 :label    "Cancel"
														 :on-click on-close}]
													 ])]}
		 [form props]]))

(defn add-comment [props]
	[dialog props])

