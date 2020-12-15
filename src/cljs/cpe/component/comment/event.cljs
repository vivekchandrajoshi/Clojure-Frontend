;; events for component home
(ns cpe.component.comment.event
	(:require [re-frame.core :as rf]
						[re-frame.cofx :refer [inject-cofx]]
						[ht.app.event :as ht-event]
						[cpe.app.event :as app-event]
						[vimsical.re-frame.cofx.inject :as inject]
						[cpe.app.subs :as app-subs]
						[cpe.util.common :as au :refer [make-field missing-field
																						set-field set-field-text
																						set-field-decimal
																						validate-field parse-value]]))


(rf/reg-event-db
	::read-less
	(fn [db [_ id]]
		(assoc-in db [:component :comment :comments id :collapse?] true)))

(rf/reg-event-db
	::comment
	(fn [db [_ path value required? id]]
		(-> db
				(assoc-in (into [:component :comment :comments id :new-comment :data] path) value)
				(assoc-in [:component :comment :comments id :new-comment :form :subject :value] value)
				(assoc-in [:component :comment :comments id :new-comment :form :subject :valid?] (if-not (= value "")
																																													 true
																																													 false)))))

(defn sort-data
	[data]
	(let [sorted-data (sort-by :date-created > data)]
		(into [] sorted-data)))

(defn sort-last-post
	[data]
	(let [sorted-data (sort-by :last-post > data)]
		(into [] sorted-data)))

(rf/reg-event-fx
	::add-comment
	(fn [_ _]
		{:dispatch [:cpe.dialog.add-comment.event/open]}))

;;;;;;;; fetch-comment  ;;;;;;;;;;;;;;;
(defn sort-by-last-post
	[data]
	(let [sorted-data (sort-by :last-post > data)]
		sorted-data
		))

(rf/reg-event-fx
	::fetch-comments-success
	(fn [{:keys [db]} [_ is-sort result]]
		(if (first result)
			(let [db-data (get-in db [:component :comment :comments])
						new-data result
						sorted-data (sort-last-post new-data)
						sorted-comment-id (mapv (fn [data] (data :id)) sorted-data)
						comments (->> (map (fn [data]
																 (let [last-post (data :last-post)
																			 last-read (data :last-read)
																			 include-permanently
																			 (get data
																						:include-in-report-permanently
																						false)
																			 include-in-report
																			 (get data
																						:include-in-report
																						false)
																			 id (data :id)
																			 old-data (db-data id)
																			 number-of-comment (data :number-of-comment)]
																	 {id (-> old-data
																					 (assoc :number-of-comment number-of-comment)
																					 (assoc :include-in-report
																									include-in-report)
																					 (assoc :include-in-report-permanently
																									include-permanently)
																					 (assoc :last-post last-post)
																					 (assoc :last-read
																									last-read
																									)
																					 )}))
															 new-data)
													(into {}))]
				{:db (if is-sort
							 (-> db
									 (assoc-in [:component :comment :comments] comments)
									 (assoc-in [:component :comment :sorted-comments-id] sorted-comment-id))
							 (-> db
									 (assoc-in [:component :comment :comments] comments)))}))))

(rf/reg-event-fx
	::fetch-comment
	(fn [{:keys [db]} [_ is-read]]
		{:dispatch [::ht-event/set-busy? false]
		 :service/fetch-comment
							 {:plant-id    (get-in db [:user :plant-id])
								:evt-success [::fetch-comments-success is-read]}}))


;;;;;;;;;;;;on read more ;;;;;;;;;;
(rf/reg-event-fx
	::fetch-reply-success-data
	(fn [{:keys [db]} [_ comment-id is-sort reply]]
		(let [comment-count (count reply)
					last-comment (- comment-count 1)]
			(if reply
				{:dispatch-n (list [::ht-event/set-busy? false]
													 ;[::fetch-comment is-sort]
													 )
				 :db         (-> db
												 (assoc-in [:component :comment :comments comment-id :comments]
																	 (into [] reply))
												 (assoc-in [:component :comment :comments comment-id :comments
																		last-comment :is-last]
																	 true)
												 (assoc-in [:component :comment :comments comment-id :collapse?]
																	 false))}))
		))


;;This is the event for the read more button
(rf/reg-event-fx
	::fetch-comments-reply
	[(inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db ::app-subs/plant]} [_ comment-id is-sort number-of-comment]]
		(if (> number-of-comment 0)
			{:dispatch [::ht-event/set-busy? false]
			 :service/fetch-reply
								 {:plant-id    (:id plant)
									:comment-id  comment-id
									:evt-success [::fetch-reply-success-data comment-id is-sort]}}
			 {:db (assoc-in db[:component :comment :comments comment-id :collapse?]
											false)})))

(rf/reg-event-fx
	::update-reply
	(fn [{:keys [db]} [_ comment-id result]]
		{:db (-> db
						 (assoc-in [:component :comment :comments comment-id :comments] result)
						 ;(assoc-in [:component :comment :comments comment-id :last-read] (js/Date.))
						 )}))

(rf/reg-event-fx
	::sort-comment-list
	(fn [{:keys [db]} [_ comment-id last-post]]
		(let [comment-data (get-in db [:component :comment :comments])
					new-data (map (fn [data]
													(let [comment (second data)]
														(if (= (first data) comment-id)
															(assoc comment :last-post last-post)
															comment)))
												comment-data)
					sorted-data (sort-last-post new-data)
					sorted-comment-id (mapv (fn [data]
																		(data :id))
																	sorted-data)
					comment (->> (map (fn [data]
															{(:id data) data}
															) sorted-data)
											 (into {}))]
			{:db (-> db
							 (assoc-in [:component :comment :comments] comment)
							 (assoc-in [:component :comment :sorted-comments-id] sorted-comment-id))}
			)))

;;;;;;;;;;;;;;;;; fatch reply by time;;;;;;;;;;;;;;;
(rf/reg-event-fx
	::fetch-reply-by-time-success
	(fn [{:keys [db]} [_ comment-id reply]]
		(if (map? reply)
			(doall
				(let [db-reply (get-in db [:component :comment :comments comment-id :comments])
							result (reply :comments)
							;merged-data (into db-reply result)
							;sorted-data (sort-data merged-data)
							]
					{:dispatch-n (list [::ht-event/set-busy? false]
														 [:cpe.component.comment.event/update-reply comment-id result]
														 [:cpe.component.comment.event/sort-comment-list comment-id (:last-post reply)])
					 })))))

(rf/reg-event-fx
	::fetch-reply-by-time
	[(inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db
							 ::app-subs/plant
							 :cpe.component.comment.subs/reply
							 ]} [_ comment-id]]
		(let [comments (get-in db [:component :comment :comments comment-id :comments])
					js-date (js/Date.)
					new-date (- (.getTime js-date) 60000)
					last-comment-time (if (first comments)
															((first comments) :date-created)
															(js/Date. new-date))]
			{:dispatch [::ht-event/set-busy? false]
			 :service/fetch-reply-by-time
								 {:plant-id     (:id plant)
									:comment-id   comment-id
									:comment-time (if last-comment-time
																	last-comment-time
																	(js/Date.))
									:evt-success  [::fetch-reply-by-time-success comment-id]}})))


;;;;;;;;reply  ;;;;;;;;;;;;;;;
(rf/reg-event-fx
	::reply
	[(inject-cofx ::inject/sub [::app-subs/user])
	 (inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db
							 ::app-subs/user
							 ::app-subs/plant
							 ]} [_ id]]
		{:dispatch-n (list
									 [::ht-event/set-busy? true]
									 [:cpe.component.comment.event/add-reply id])}))

;;;;;;;;add reply  ;;;;;;;;;;;;;;;
(rf/reg-event-fx
	::fetch-reply-success
	(fn [{:keys [db]} [_ id is-sort comment]]
		(if (:modified? comment)
			{:dispatch-n (list [::fetch-comment is-sort]
												 (if-not (get-in db [:component :comment :comments id :collapse?])
													 ;[:cpe.component.comment.event/fetch-reply-by-time id]
													 [:cpe.component.comment.event/fetch-comments-reply
														id false (get-in db [:component :comment
																								 :comments id :number-of-comment])]
													 [::ht-event/set-busy? false]))
			 :db         (-> db
											 (assoc-in [:component :comment :comments id :new-comment :form] nil)
											 (assoc-in [:component :comment :comments id :new-comment :data] nil))})))

(rf/reg-event-fx
	::add-reply
	[(inject-cofx ::inject/sub [::app-subs/user])
	 (inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db ::app-subs/user ::app-subs/plant]} [_ id is-sort]]
		(let [reply (get-in db [:component :comment :comments id :new-comment :data :reply])
					new-comment {:comment reply :comment-by (:id user)}]
			{:service/add-reply
			 {:plant-id    (:id plant)
				:comment-id  id
				:comment     new-comment
				:evt-success [::fetch-reply-success id is-sort]}})))

(rf/reg-event-fx
	::save-user-success
	(fn [{:keys [db]} [_ status user]]
		(if (:modified? user)
			{:dispatch [::ht-event/set-busy? false]
			 :db       (assoc-in db [:user :email-alert?] status)})))

(rf/reg-event-fx
	::email-sub
	(fn [{:keys [db]} [_ status]]
		{:service/save-user {:user        {:id           (get-in db [:user :id])
																			 :email-alert? status},
												 :evt-success [::save-user-success status]
												 :evt-failure [::ht-event/service-failure true]}
		 :dispatch          [::ht-event/set-busy? true]}))

(rf/reg-event-fx
	::include-in-report-permanently
	[(inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db ::app-subs/plant]} [_ comment-id value]]
		(let [plant-id (:id plant)]
			{:service/include-in-report-permanently
								 {:plant-id                      plant-id
									:comment-id                    comment-id
									:include-in-report-permanently value
									:evt-success                   [::fetch-include-in-report-success comment-id]}
			 :dispatch [::ht-event/set-busy? true]})))

(rf/reg-event-fx
	::fetch-include-in-report-success
	(fn [_ [_ comment-id result]]
		(if (:modified? result)
			{:dispatch-n (list [:cpe.dialog.add-comment.event/fetch-comments comment-id]
												 [::ht-event/set-busy? false])})))


;;;;;;;; paging ;;;;;;;;;;;;;;;

(rf/reg-event-db
	::load-previous-comment
	(fn [db [_ id]]
		(let [count (get-in db [:component :comment :comments id :page-count])]
			(assoc-in db [:component :comment :comments id :page-count] (if count
																																		(+ count 1)
																																		2)))))
;;;;;;;; read functionality;;;;;;;;;;;;;;;
(rf/reg-event-fx
	::fetch-read-success
	(fn [{:keys [db]} [_ id user result]]
		(let [db-last-read (get-in db [:component :comment :comments id :last-read])
					index (remove nil? (map-indexed (fn [i item]
																						(if (= (item :read-by) user)
																							i
																							))
																					db-last-read))]
			{:db
							 (if (first index)
									 (assoc-in db
														 [:component :comment :comments id
															:last-read (first index) :read-on]
														 (js/Date.))
									 (assoc-in db [:component :comment :comments id :last-read]
														 (if db-last-read
															 (merge db-last-read {:read-by user
																										:read-on (js/Date.)})
															 (conj [] {:read-by user
																										:read-on (js/Date.)}))))})))

(rf/reg-event-fx
	::read-comment
	[(inject-cofx ::inject/sub [::app-subs/user])
	 (inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [ db ::app-subs/user ::app-subs/plant]} [_ comment-id]]
		{:service/read
		 {:plant-id    (:id plant)
			:comment-id  comment-id
			:evt-success [::fetch-read-success comment-id (user :id)]
			:evt-failure [::ht-event/service-failure true]}}))
