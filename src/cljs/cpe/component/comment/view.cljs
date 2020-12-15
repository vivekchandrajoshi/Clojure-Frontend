;; view elements component home
(ns cpe.component.comment.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style class]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [cpe.app.icon :as ic]
            [cpe.app.comp :as app-comp]
            [cpe.app.style :as app-style]
            [cpe.app.subs :as app-subs]
            [cpe.app.event :as app-event]
            [cpe.app.scroll :refer [scroll-box]]
            [cpe.component.comment.style :as style]
            [cpe.component.comment.subs :as subs]
            [cpe.component.comment.event :as event]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cpe.dialog.add-comment.view :refer [add-comment]]
            ))

(def date-formatter (tf/formatter "yyyy-MM-dd"))

(defn format-date [date] (tf/unparse date-formatter (t/date-time date)))

(defn comment-created-by [created-by topsoe?]
  (cond
    (= topsoe? true) (str created-by)
    (not topsoe?) (if (= (re-find #"@topsoe.com" created-by) "@topsoe.com")
                    (str "Topsoe")
                    (str created-by))))

(defn reply [id]
  [app-comp/button {:disabled? (if @(rf/subscribe [::subs/valid? id])
                                 false
                                 true)
                    :label     "Reply"
                    :icon      ic/upload
                    :on-click  #(rf/dispatch [::event/add-reply id false])}])

(defn comment-area [w id]
  [:div {:style {:padding-top "10px"}}
   [app-comp/text-area {:height    100
                        :value     @(rf/subscribe [:cpe.component.comment.subs/reply id])
                        :width     (+ w 20)
                        :on-change #(rf/dispatch [::event/comment [:reply] % false id])}]])

(defn comment-status [level]
  (let [{:keys [icon]} style/comment-status-icon]
    [ui/font-icon {:class-name (case (:id level)
                                 "Critical" "fa fa-times-circle"
                                 "Warning" "fa fa-exclamation-circle"
                                 "Good" "fa fa-thumbs-o-up"
                                 "fa fa-envelope-o")
                   :style      (:icon (style/comment-status-icon level))}]))

(defn comment-subject [id include-in-report-permanently type-id style title subject
                       date-created created-by chart-info collapse? topsoe?]
  (let [{:keys [start-of-run-day end-of-run-day chart-id]} chart-info
        topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
    [:div
     [:div {:style {:display "inline-flex"}}
      [comment-status type-id]
      [:div {:style (:comment-title style)}
       [:span {:style (:comment-item style)} (str (comment-created-by created-by topsoe?) " :")]
       [:span {:style (:comment-item style)} (str (if start-of-run-day
                                                    (str "Run days " start-of-run-day))
                                               (if end-of-run-day
                                                 (str " To " end-of-run-day))
                                               (if (or start-of-run-day end-of-run-day)
                                                 " :"))]
       (if chart-id
         (if-not (= (:id chart-id) "0")
           [:span {:style (:comment-item style)} (:name chart-id)]
           ))]]

     ;;Adding the toggle button to include this commen tin report
     (if topsoe?
       [:div (use-style style/report-toggle)
        [:span (use-sub-style style/report-toggle :label) "Include in report"]
        [app-comp/toggle {:value include-in-report-permanently
                          :on-toggle #(rf/dispatch
                                       [::event/include-in-report-permanently id %])}]])

     [:div {:style (:comment-subject style)} (if collapse?
                                               title
                                               subject)]
     [:div {:style (:commented-on style)} (format-date date-created)]]))

(defn more-comment
  "Rander all the comment  on read more click"
  [props comments]
  (let [style   (style/comment-style (get props :height) (get props :width))
        topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
    [:div {:style (:comment-section style)}
     (doall
       (map-indexed (fn [i comment]
                      (let [{:keys [comment created-by date-created is-last]}
                            comment]
                        ^{:key i}
                        [:div
                         [:span {:style (:comment-by style)}
                         (str (comment-created-by created-by topsoe?) " :-")
                          ]
                         (if is-last
                           [:span {:style {:font-weight 600}} comment]
                           [:span comment]
                           )
                         [:div {:style (:commented-on style)} (format-date date-created)]
                         [:hr]]))
         comments))]))

(defn reply-block [id collapse? style comments props number-of-comment
									 page-count last-read]
  [:div
   [:div {:style {:text-align "right"}}
    [:a (merge {:href "javascript:void(0)"
                :on-click #(rf/dispatch [::event/fetch-comments-reply id
                                         false number-of-comment])}
          (if-not collapse?
            {:style (:hide style)}))
     "Read more"]]
   [:div (if collapse?
           {:style (:hide style)})
		(if (< (* (if  page-count
								page-count
								1) 5)  number-of-comment)
			[:a {:href     "javascript:void(0)"
					 :on-click #(rf/dispatch
												[::event/load-previous-comment
												 id])} "View previous comments"])
    [more-comment props comments]
    [:div {:style {:text-align "right"}}
     [:a {:href "javascript:void(0)"
					:on-click #(rf/dispatch [::event/read-less id])}
      "Read less"]]]])

(defn comment-card
  "Create comment card "
  [props]
  (let [h                 (get props :height)
        w                 (get props :width)
        style             (style/comment-style h w)
        sorted-comment-id (get-in props [:comments :sorted-comments-id])
        comments          (get-in props [:comments :comments])
        topsoe?           @(rf/subscribe [::ht-subs/topsoe?])
        active-user       (@(rf/subscribe [::ht-subs/auth-claims]) :id)
				page-count        (get props :comments)]
    [:div
     (doall
       (map-indexed (fn [i comment-id]
                      (let [{:keys [id subject title type-id comments
                                    created-by collapse? date-created
                                    chart-info last-read last-post
                                    include-in-report-permanently]} (comments comment-id)
                            last-read-map  (map (fn [d]
                                                  (if (= (d :read-by) active-user)
                                                    (d :read-on))) last-read)
                            last-read-date (first (remove nil? last-read-map))
                            number-of-comment (get-in props [:comments :comments
                                                             comment-id :number-of-comment])
														page-count (get-in props [:comments :comments
																											comment-id :page-count] )
														comments-data (take-last (if page-count
																											 (* page-count 5)
																											 5)
																										 comments)]
                        ^{:key i}
                        [:div {
                               :style (style/comment-block (< last-read-date last-post) w)
                               :on-click #(rf/dispatch [::event/read-comment comment-id])
                               }
                         [comment-subject id include-in-report-permanently type-id style
                                          title subject date-created created-by
                                          chart-info collapse? topsoe?]
                         (if (or (> (count subject) 80) (> number-of-comment 0))
                           [reply-block id collapse? style comments-data props
														number-of-comment page-count last-read]
                           )
                         [comment-area (- w 15) id]
                         [reply id]]))
         sorted-comment-id))]))

(defn comment-container
  "Comment container to display all the comment cards "
  [props]
  (let [h        (get props :height)
        w        (get props :width)
        comments @(rf/subscribe [::subs/comment])
        email-sub @(rf/subscribe [::subs/email-sub])
        style    (style/comment-style h w)]
    [:div
     [scroll-box {:style {:height (- h 50) :width (+ w 30) }}
      [comment-card (assoc props :comments comments)]]]))

(defn comment-data [props]
  [:div {:style (:container (style/comment-style (get-in props [:size :height])
                                      (get-in props [:size :width])))}
   [comment-container props]
   (if @(rf/subscribe [:cpe.dialog.add-comment.subs/open?])
     [add-comment props])])
