;; events for component section
(ns cpe.component.section.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [cpe.component.section.subs :as subs]
            [ht.app.event :as ht-event]
            [cpe.app.event :as app-event]
            [cpe.app.subs :as app-subs]
            [cpe.component.uom.subs :as uom-subs]
            [cpe.component.uom.event :as uom-event]
            [cpe.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]
						[cpe.util.report :refer [download]]
						[cpe.util.excel :refer [export-excel]]))

(defonce comp-path [:component :section])
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n
    (list [::set-field [:summary] "" true]
          [::set-field [:edit-id] nil false])}))

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field db path value data data-path form-path required?))))

(rf/reg-event-fx
 ::update-summary
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ value]]
   {:db (assoc-in db data-path (assoc data :summary value))
    :dispatch-n (list [::set-field [:summary] value true])}))

(rf/reg-event-fx
 ::update-edit-summary
 [(inject-cofx ::inject/sub [::subs/comp-data])]
 (fn [{:keys [db ::subs/comp-data]} [_ value]]
   (let [edit-id (:edit-id comp-data)
         edit-value (get-in comp-data [:summary-history edit-id])
         new-value (assoc edit-value :subject value)
         new-summary-history (assoc (:summary-history comp-data) edit-id new-value)]
     {:db (assoc-in db data-path (assoc comp-data :summary-history new-summary-history))})))

(rf/reg-event-fx
	::sync-summary-history
	[(inject-cofx ::inject/sub [::app-subs/summary])
	 (inject-cofx ::inject/sub [::subs/data])]
	(fn [{:keys [db ::app-subs/summary ::subs/data]} [_ sync-key close? [eid]]]
		(let [success? (= eid ::app-event/fetch-summary-success)
					sync-data summary]
			(cond-> {:forward-events {:unregister ::sync-summary-history}}
							;; sync data on success
							success?
							(assoc :db (assoc-in db data-path (-> data
																										(assoc sync-key sync-data)
																										(assoc :edit-id nil))))))))

(rf/reg-event-fx
	::sync-summary-after-save
	[(inject-cofx ::inject/sub [::app-subs/summary])
	 (inject-cofx ::inject/sub [::subs/data])]
	(fn [{:keys [db ::app-subs/summary ::subs/data]} [_ sync-key close? [eid]]]
		(let [success? (= eid ::app-event/fetch-summary-success)
					sync-data summary]
			(cond-> {:forward-events {:unregister ::sync-summary-after-save}}
							;; sync data on success
							success?
							(assoc :db (assoc-in db data-path (-> data
																										(assoc sync-key sync-data)
																										(assoc :summary ""))))

							true
							(assoc :dispatch [::set-field [:summary] "" true])

							;; leave if asked for
							;;(and success? close?)
							;;(assoc :dispatch [:cpe.component.root.event/activate-content :section nil "home"])
							))))

(rf/reg-event-fx
 ::edit-summary
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ summary_id]]
   {:db (assoc-in db data-path (assoc data :edit-id summary_id))}))

(rf/reg-event-fx
 ::cancel-edit-summary
 [(inject-cofx ::inject/sub [::subs/src-data])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/src-data ::subs/data]} [_ _]]
   {:db (assoc-in db data-path (-> data
                                   (assoc :summary-history
                                          (:summary-history src-data))
                                   (assoc :edit-id nil)))}))

(rf/reg-event-fx
 ::upload-summary
 [(inject-cofx ::inject/sub [::subs/can-submit?])
  (inject-cofx ::inject/sub [::subs/comp-data])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::subs/can-submit?, ::subs/comp-data, ::app-subs/plant]} _]
   (let [subject (:summary comp-data)
         plant-id (:id plant)]
     (if can-submit?
       {:forward-events {:register ::sync-summary-after-save
                         :events #{::app-event/fetch-summary-success
                                   ::ht-event/service-failure}
                         :dispatch-to [::sync-summary-after-save :summary-history false]}
        :dispatch [::ht-event/set-busy? true]
        :service/create-summary
        {:subject     subject
         :plant-id    plant-id
         :evt-success [::app-event/fetch-summary]}}
       {:db (update-in db comp-path assoc :show-error? true)}))))

(rf/reg-event-fx
 ::upload-edit-summary
 [(inject-cofx ::inject/sub [::subs/can-submit?])
  (inject-cofx ::inject/sub [::subs/comp-data])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::subs/can-submit?, ::subs/comp-data, ::app-subs/plant]} _]
   (let [edit-id (:edit-id comp-data)
         subject (get-in comp-data [:summary-history edit-id :subject])
         plant-id (:id plant)]
     (if can-submit?
       {:forward-events {:register ::sync-summary-history
                         :events #{::app-event/fetch-summary-success
                                   ::ht-event/service-failure}
                         :dispatch-to [::sync-summary-history :summary-history false]}
        :dispatch [::ht-event/set-busy? true]
        :service/update-summary
        {:summary-id edit-id
         :subject     subject
         :plant-id    plant-id
         :evt-success [::app-event/fetch-summary]}}
       {:db (update-in db comp-path assoc :show-error? true)}))))

(rf/reg-event-fx
 ::publish-summary
 [(inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db, ::app-subs/plant]} [_ summary-id]]
   (let [plant-id (:id plant)]
     {:forward-events {:register ::sync-summary-history
                       :events #{::app-event/fetch-summary-success
                                 ::ht-event/service-failure}
                       :dispatch-to [::sync-summary-history :summary-history false]}
      :dispatch [::ht-event/set-busy? true]
      :service/publish-summary
      {:summary-id summary-id
       :plant-id    plant-id
       :evt-success [::app-event/fetch-summary]}})))

(rf/reg-event-fx
 ::delete-summary
 [(inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db, ::app-subs/plant]} [_ summary-id]]
   (let [plant-id (:id plant)]
     {:forward-events {:register ::sync-summary-history
                       :events #{::app-event/fetch-summary-success
                                 ::ht-event/service-failure}
                       :dispatch-to [::sync-summary-history :summary-history false]}
      :dispatch [::ht-event/set-busy? true]
      :service/delete-summary
      {:summary-id summary-id
       :plant-id    plant-id
       :evt-success [::app-event/fetch-summary]}})))



;;active-unit-system @(rf/subscribe [::subs/active-unit-system])
;;units @(rf/subscribe [::subs/units active-unit-system])]

(defn uom-converted-sensor-data [sensor-name uom-converted-data base-uom
																 uom-converted-data sensor-uom-conversion-config]
	(let [sensor-min (:min sensor-uom-conversion-config)
				sensor-max  (:max sensor-uom-conversion-config)
				sensor-rfq (:rfq sensor-uom-conversion-config)]
		{sensor-name {:sensor-name       sensor-name
									:sensor-value      uom-converted-data
									:base-uom          base-uom
									:x-min             (apply min (map #(:x %) uom-converted-data))
									:x-max             (apply max (map #(:x %) uom-converted-data))
									:y-min             (if sensor-min
																			 sensor-min
																			 (apply min (map #(:y %) uom-converted-data)))
									:y-max             (if sensor-max
																			 sensor-max
																			 (apply max (map #(:y %) uom-converted-data)))
									:rfq                (if sensor-rfq
																				sensor-rfq)
									:last-service-call (js/Date.)}})

	)

(defn sensor-uom-conversion [ sensor-name db units base-uom]
	(let [sensor-value ((keyword sensor-name) @(rf/subscribe [::subs/get-sensor-config]))
				sensor-uom-id (get-in db [:sensor sensor-name :uom-id])
				selected-uom (get-in units [sensor-uom-id :selected-unit-id])
				uom-units (get-in units [sensor-uom-id :units])
				selected-unit-val (some (fn [d]
																	(if (= (d :id) selected-uom)
																		d)) uom-units)
				has-api (nil? (some (fn [d]
															(if (= (d :id) "deg.API")
																d)) uom-units))
				base-unit-val (some (fn [d]
															(if (or (= (d :name) base-uom) (= (d :id) base-uom))
																d)) uom-units)
				sensor-value-data
				(cond
					(= (:id  selected-unit-val) "deg.API")
								(let [selected-unit-sg (some (fn [d]
																						(if (= (d :id) "SG")
					 																						d	)) uom-units)
											sensor-value-data-sg (let [a1 (get base-unit-val :factor 1)
																								 a2 (get selected-unit-sg :factor 1)
																								 b1 (get base-unit-val :offset 1)
																								 b2 (get selected-unit-sg :offset 1)
																								 factor (/ a2 a1)
																								 offset (- (* a1 b2) (* a1 b1))
																								 min (:min sensor-value)
																								 max (:max sensor-value)
																								 rfq (:rfq sensor-value)]
																						 {:min (if min
																										 (- (/ 141 (* (- min offset)
																																	factor)) 131.5))
																							:max (if max
																										 (- (/ 141 (* (- max offset)
																																	factor)) 131.5))
																							:rfq (if rfq
																										 (- (/ 141 (* (- rfq offset)
																																	factor)) 131.5))})]
									sensor-value-data-sg)
					(and (or (= (:id  selected-unit-val) "SG") (= (:id  selected-unit-val)
																										"kg/m3") ) (= has-api
																																	false))
								(let [a1 (get base-unit-val :factor 1)
											a2 (get selected-unit-val :factor 1)
											b1 (get base-unit-val :offset 1)
											b2 (get selected-unit-val :offset 1)
											factor (/ a2 a1)
											offset (- (* a1 b2) (* a1 b1))
											min (:min sensor-value)
											max (:max sensor-value)
											rfq (:rfq sensor-value)]
									{:min (if max
													(* (- max offset) factor))

									 :max (if min
													(* (- min offset) factor))
									 :rfq (if rfq
													(* (- rfq offset) factor))})
					:else (let [a1 (get base-unit-val :factor 1)
											a2 (get selected-unit-val :factor 1)
											b1 (get base-unit-val :offset 1)
											b2 (get selected-unit-val :offset 1)
											factor (/ a2 a1)
											offset (- (* a1 b2) (* a1 b1))
											min (:min sensor-value)
											max (:max sensor-value)
											rfq (:rfq sensor-value)]
									{:min (if min
													(* (- min offset) factor))

									 :max (if max
													(* (- max offset) factor))
									 :rfq (if rfq
													(* (- rfq offset) factor))}))]
		sensor-value-data))

(defn uom-conversion [sensor-value sensor-name db units base-uom]
  (let [sensor-uom-id      (get-in db [:sensor sensor-name :uom-id])
        date-sor           (get-in db [:plant :config :date-sor])
        selected-uom       (get-in units [sensor-uom-id :selected-unit-id])
        uom-units (get-in units [sensor-uom-id :units])
        selected-unit-val  (some (fn [d]
                                   (if (= (d :id) selected-uom)
                                     d)) uom-units)
        base-unit-val   (some (fn [d]
                              (if (or (= (d :name) base-uom) (= (d :id) base-uom))
                                d)) uom-units)
        sensor-value-data  	(if (= (:id  selected-unit-val) "deg.API")
															 (let [selected-unit-sg (some (fn [d]
																															(if (= (d :id) "SG")
																																d	)) uom-units)
																		 sensor-value-data-sg  (mapv (fn [sensor-val]
																																	 (let [{:keys [sensor-date sensor-value sensor-time]} sensor-val
																																				 ;{:keys [factor offset]} selected-unit-val
																																				 a1 (get base-unit-val :factor 1)
																																				 a2 (get selected-unit-sg :factor 1)
																																				 b1  (get base-unit-val :offset 1)
																																				 b2 (get selected-unit-sg :offset 1)
																																				 factor (/ a2 a1)
																																				 offset (- (* a1 b2) (* a1 b1))]
																																		 ;(print factor "factor" offset "offset" sensor-value "value" (* (- sensor-value offset) factor), "(* (- sensor-value offset) factor)")
																																		 {:sensor-date  sensor-date
																																			:sensor-value sensor-value
																																			:y            (if sensor-uom-id
																																											(- (/ 141 (* (- sensor-value offset) factor))131.5)
																																											sensor-value)
																																			:sensor-time  sensor-time
																																			:x            (.floor js/Math (/ (- sensor-date date-sor) 1000 3600 24))
																																			})) sensor-value)]
																 sensor-value-data-sg)
															 (mapv (fn [sensor-val]
																			 (let [{:keys [sensor-date sensor-value sensor-time]} sensor-val
																						 ;{:keys [factor offset]} selected-unit-val
																						 a1 (get base-unit-val :factor 1)
																						 a2 (get selected-unit-val :factor 1)
																						 b1  (get base-unit-val :offset 1)
																						 b2 (get selected-unit-val :offset 1)
																						 factor (/ a2 a1)
																						 offset (- (* a1 b2) (* a1 b1))]
																				 ;(print factor "factor" offset "offset" sensor-value "value" (* (- sensor-value offset) factor), "(* (- sensor-value offset) factor)")
																				 {:sensor-date  sensor-date
																					:sensor-value sensor-value
																					:y            (if sensor-uom-id
																													(* (- sensor-value offset) factor)
																													sensor-value)
																					:sensor-time  sensor-time
																					:x            (.floor js/Math (/ (- sensor-date date-sor) 1000 3600 24))
																					})) sensor-value)
															 )

				]
    ;(print "base-uom" "units" units "sensor-uom-id" sensor-uom-id "selected-uom" selected-uom "selected-unit-val" selected-unit-val)
    ;(print "base-uom" base-uom "base-unit-val" base-unit-val)
    sensor-value-data))

(rf/reg-event-fx
	::fetch-sql-data-success
	[(inject-cofx ::inject/sub [::uom-subs/units])]
	(fn [{:keys [db ::uom-subs/units]} [_ from result]]

		(if result
			(let [prepared-data (map (fn [{:keys [sensor-name sensor-value base-uom]}]
																 (let [uom-converted-data (uom-conversion sensor-value sensor-name db units base-uom)
																			 sensor-uom-converted-data
																			 (sensor-uom-conversion
																				 sensor-name db units base-uom)]
																	 (uom-converted-sensor-data sensor-name
																															uom-converted-data base-uom uom-converted-data sensor-uom-converted-data)
																	 )) result)]
				(if from
					(do {:dispatch (cond
													 (= from :report) [::on-report-download]
													 (= from :excel) [::on-excel-export])
							 :db       (update-in db [:component :chart-sql-data] #(merge % (into {} prepared-data)))})
					(do {:db (update-in db [:component :chart-sql-data] #(merge % (into {} prepared-data)))})
					)

				))))


(rf/reg-event-fx
	::on-uom-change
	[(inject-cofx ::inject/sub [::uom-subs/units])
	 (inject-cofx ::inject/sub [:cpe.component.section.subs/charts-sql-data])]
	(fn [{:keys [db ::app-subs/plant
							 ::uom-subs/units
							 :cpe.component.section.subs/charts-sql-data]} [_ from]]
		(if charts-sql-data
			(let [charts-sql-val (map val charts-sql-data)
						prepared-data (map (fn [{:keys [sensor-name sensor-value base-uom]}]
																 (let [uom-converted-data (uom-conversion sensor-value sensor-name db units base-uom)
																			 sensor-uom-converted-data
																			 (sensor-uom-conversion
																				 sensor-name db units base-uom)]
																	 (uom-converted-sensor-data sensor-name uom-converted-data base-uom uom-converted-data
																															sensor-uom-converted-data)
																	 )) charts-sql-val)]
				(if from
					(do {:dispatch (cond
													 (= from :report) [::on-report-download]
													 (= from :excel) [::on-excel-export])
							 :db       (update-in db [:component :chart-sql-data] #(merge % (into {} prepared-data)))})
					(do {:db (update-in db [:component :chart-sql-data] #(merge % (into {} prepared-data)))})
					)
				))))


(rf/reg-event-fx
	::fetch-sql-data
	[(inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db
							 ::app-subs/plant]} [_ sensors from]]
		(if from
			(let [chart-sql-sensor (count (get-in db [:component :chart-sql-data]))
						sensor (count (get db :sensor))
						has-call? (if (= chart-sql-sensor sensor)
												true
												true)]
				(if has-call?
					{:service/fetch-sql-data
					 {:plant-id    (get-in plant [:config :sql-plant-id])
						:start-date  (get-in plant [:config :date-sor])
						:evt-success [::fetch-sql-data-success from]}}
					{:dispatch
					 [::on-uom-change from]}))

			(let [new-sensors (mapv (fn [s]
																(let [sensor (get-in db [:component :chart-sql-data s])
																			old-plant-id (db :old-plant)
																			new-plant-id (get-in db [:plant :id])
																			last-call (if (= old-plant-id new-plant-id)
																									(if sensor
																										(sensor :last-service-call)))]
																	(cond
																		(= sensor nil) s
																		(not= last-call nil) (if-not (= (.getDate last-call) (.getDate (js/Date.)))
																													 s)
																		:else nil))) sensors)
						service-call-sensor (into [] (remove nil? new-sensors))]
				(if-not (empty? service-call-sensor)
					{:service/fetch-sql-data
					 {:plant-id    (get-in plant [:config :sql-plant-id])
						:start-date  (get-in plant [:config :date-sor])
						:name        service-call-sensor
						:evt-success [::fetch-sql-data-success from]}}
					{:dispatch [::on-uom-change from]})))))

(rf/reg-event-db
  ::show-full-chart
  (fn [db [_ id]]
    (assoc db :show-chart-dialog {:open     true
                                  :chart-id id})))


(rf/reg-event-db
  ::hide-full-chart
  (fn [db [_]]
    (assoc db :show-chart-dialog nil)))

(rf/reg-event-fx
  ::pin-chart
  [(inject-cofx ::inject/sub [::app-subs/plant])
   (inject-cofx ::inject/sub [::app-subs/client])]
  (fn [{:keys [db ::app-subs/plant ::app-subs/client]} [_ id]]
    (let [pined-chart (get-in plant [:settings :pinned-charts])]
      {:dispatch [::ht-event/set-busy? true]
       :service/update-plant-settings
                 {:client-id   (:id client)
                  :plant-id    (:id plant)
                  :settings    {:pinned-charts (conj pined-chart id)}
                  :evt-success [::app-event/fetch-plant (:id client) (:id plant)]}})))

(rf/reg-event-fx
  ::un-pin-chart
  [(inject-cofx ::inject/sub [::app-subs/plant])
   (inject-cofx ::inject/sub [::app-subs/client])]
  (fn [{:keys [db
               ::app-subs/plant ::app-subs/client]} [_ id]]
    (let [pined-chart     (get-in plant [:settings :pinned-charts])
          new-pined-chart (remove nil?
                            (mapv (fn [d]
                                    (if-not (= d id)
                                      d)) pined-chart))]
      {:dispatch [::ht-event/set-busy? true]
       :service/update-plant-settings
                 {:client-id   (:id client)
                  :plant-id    (:id plant)
                  :settings    {:pinned-charts new-pined-chart}
                  :evt-success [::app-event/fetch-plant (:id client) (:id plant)]}})))

(rf/reg-event-fx
	::on-report-download
	[(inject-cofx ::inject/sub [::uom-subs/units])]
	(fn [{:keys [db ::uom-subs/units]} [_]]
		(let [client-name (get-in db [:client :name])
					plant-name (get-in db [:plant :name])
					summary (-> (->> (map (fn [d]
																	(second d)
																	) (db :summary))
													 (sort-by :date-published >)
													 (first)) :subject)
					comments (get-in db [:component :comment :comments])
					is-topsoe (get-in db [:auth :claims :topsoe?])
					section (db :section)
					report-settings {:date-rage           (get-in db [:component :report-settings :data
																														:report-date-range])
													 :comments-to-include (remove nil?
																												(->> (get-in db [:component :report-settings :data
																																				 :comments-to-include])
																														 (mapv (fn [d]
																																		 (if ((second d) :include-in-report)
																																			 ((second d) :id)
																																			 )))))}
					permanent-comment   (remove nil? (mapv (fn [d]
																									 (if (:include-in-report-permanently (second d) )
																										 (second d))
																									 )comments) )
					plant-section (get-in db [:plant :config :section])
					chart (db :chart)
					sensor (db :sensor)
					sensor-data (get-in db [:component :chart-sql-data])
					include-comment (mapv (fn [d]
																	(get comments d))
																(report-settings :comments-to-include))]
			(download {:client-name   client-name :plant-name plant-name
								 :summary       summary :section section
								 :plant-section plant-section :chart chart
								 :report-data   report-settings
								 :comments    (into permanent-comment include-comment)
								 :is-topsoe     is-topsoe
								 :sensor        sensor
								 :sensor-data   sensor-data
								 :units         units})
			(rf/dispatch [::ht-event/set-busy? false]))))

(rf/reg-event-fx
	::fetch-export-excel-data-success
	(fn [{:keys [db ]} [_ result]]
		(let [data (first result)
					calculated  (:calculated data)
					raw   (:raw data)
					sensor  (:sensor db)
					raw-tag (reduce-kv (fn [col k d]
												 (-> col
														 (conj {(:name d) {:description (:description d)}}))
												 ) {}
											 (get-in db [:plant :settings :raw-tags]))
					calculated-dec (into [] (map-indexed (fn [i d]
																								 (cond
																									 (= i 0) (str "Date")
																									 (= i 1) (str "Run Day")
																									 :else (get-in sensor [d :description] )))
																							 (first calculated)))
					raw-dec (into [] (map-indexed (fn [i d]
																					(cond
																						(= i 0) (str "Date")
																						(= i 1) (str "Run Day")
																						:else (get-in raw-tag [d :description] )))
																				(first raw))
												)
					plant-name (get-in db [:plant :name])
					date-of-sor (get-in db [:plant :config :date-sor ])

					plant-constant  (get-in db [:plant :settings
																			:constant])
					run-constant (get-in db [:plant :config
																	 :constant])
					constant-data (into plant-constant run-constant)

					constant (reduce-kv (fn [col indexed d]
																(let [description  (:description  d)
																			value (:value d)]
																	(-> col
																			(into  (vector description value))
																			))) []  constant-data)]
			(export-excel (-> []
												(conj calculated-dec)
												(into calculated)
												) (-> []
															(conj constant)
															(into [[nil] [nil]])
															(conj raw-dec)
															(into raw)) plant-name )
			(rf/dispatch [::ht-event/set-busy? false]))))


(rf/reg-event-fx
	::excel-export
	[(inject-cofx ::inject/sub [::app-subs/plant])]
	(fn [{:keys [db  ::app-subs/plant]} [_]]
		{:service/fetch-export-excel-data
		 {:plant-id    (get-in plant [:config :sql-plant-id])
			:start-date  (get-in plant [:config :date-sor])
			:evt-success [::fetch-export-excel-data-success ]}}))


