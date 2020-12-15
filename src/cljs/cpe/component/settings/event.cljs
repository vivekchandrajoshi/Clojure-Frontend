;; events for component settings
(ns cpe.component.settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [cpe.component.uom.subs :as uom-subs]
            [cpe.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]
            [cpe.app.subs :as app-subs]
            [cpe.app.event :as app-event]
            [cpe.component.settings.subs :as subs]))

;;; Do NOT use rf/subscribe
;;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]
;
;;; Add some event handlers, like
;#_ (rf/reg-event-db
;    ::event-id
;    (fn [db [_ param]]
;      (assoc db :param param)))
;;;
;;; NOTE: all event handler functions should be pure functions
;;; Typically rf/reg-event-db should suffice for most cases, which
;;; means you should not access or modify any global vars or make
;;; external service calls.
;;; If external data/changes needed use rf/reg-event-fx, in which case
;;; your event handler function should take a co-effects map and return
;;; a effects map, like
;#_ (rf/reg-event-fx
;    ::event-id
;    [(inject-cofx ::inject/sub [::subs/data])]
;    (fn [{:keys[db ::subs/data]} [_ param]]
;      {:db (assoc db :param param
;                  :data data)}))
;;;
;;; If there is a need for external data then inject them using inject-cofx
;;; and register your external data sourcing in cofx.cljs
;;; Similarly, if your changes are not limited to the db, then use
;;; rf/reg-event-fx and register your external changes as effects in fx.cljs

(defonce comp-path [:component :settings])
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))


(defn uom-convert [k min max rfq type offset factor plant-min plant-max
                   plant-rfq plant-offset plant-factor selected-unit-val]
  (if (= (:id selected-unit-val) "deg.API")
    (if ( = type :base)
      {k {:min (if min
                 (cond
                   (=
                     (* (- plant-min offset)factor) (* (- min plant-offset) plant-factor))  plant-min
                   (not=
                     (*
                       (- plant-min offset)factor) (* (- min plant-offset) plant-factor))
                   (* (- (/ 141.5 (+ (js/parseInt min) 131.5)) offset) factor)
                   )
                 nil)
          :max (if max
                 (cond
                   (=
                     (* (- plant-max offset)factor) (* (- max plant-offset) plant-factor))  plant-max
                   (not=
                     (* (- plant-max offset)factor) (* (- max plant-offset) plant-factor))
                   (* (- (/ 141.5 (+ (js/parseInt max) 131.5)) offset) factor)
                   )
                 nil)
          :rfq (if rfq
                 (cond
                   (=
                     (* (- plant-rfq offset)factor) (* (- rfq plant-offset) plant-factor))  plant-rfq
                   (not=
                     (* (- plant-rfq offset)factor) (* (- rfq plant-offset) plant-factor))
                   (* (- (/ 141.5 (+ (js/parseInt rfq) 131.5)) offset) factor)
                   )
                 nil)}}
      {k {:min (if min
                 (-  (/ 141.5  (* (- min offset) factor)) 131.5)
                 nil)
          :max (if max
                 (-   (/ 141.5 (* (- max offset) factor)) 131.5)
                 nil)
          :rfq (if rfq
                 (- (/ 141.5  (* (- rfq offset) factor)) 131.5)
                 nil)}})


    (if ( = type :base)
      {k {:min (if min
                 (cond
                   (=
                     (* (- plant-min offset)factor) (* (- min plant-offset) plant-factor))  plant-min
                   (not=
                     (*
                       (- plant-min offset)factor) (* (- min plant-offset) plant-factor))  (* (- min offset) factor)
                   )
                 nil)
          :max (if max
                 (cond
                   (=
                     (* (- plant-max offset)factor) (* (- max plant-offset) plant-factor))  plant-max
                   (not=
                     (* (- plant-max offset)factor) (* (- max plant-offset) plant-factor))  (* (- max offset) factor)
                   )
                 nil)
          :rfq (if rfq
                 (cond
                   (=
                     (* (- plant-rfq offset)factor) (* (- rfq plant-offset) plant-factor))  plant-rfq
                   (not=
                     (* (- plant-rfq offset)factor) (* (- rfq plant-offset) plant-factor))  (* (- rfq offset) factor)
                   )
                 nil)}}
      {k {:min (if min
                 (* (- min offset) factor)
                 nil)
          :max (if max
                 (* (- max offset) factor)
                 nil)
          :rfq (if rfq
                 (* (- rfq offset) factor)
                 nil)}})))

(defn uom-data [units k d type base-unit-val selected-unit-val
                plant-chart-sensor sensor-name]

  (if (= (:id selected-unit-val) "deg.API")
    (let [ uom (reduce-kv (fn [m k v]
                            (into m (:units v)))
                          []  units)
          selected-unit-sg (some (fn [d]
                                   (if (= (d :id) "SG")
                                     d)) uom)
          a2 (if (= type :base)
               (get
                 base-unit-val :factor 1)
               (get selected-unit-sg :factor 1))
          a1 (if (= type :base)
               (get
                 selected-unit-sg :factor 1)
               (get base-unit-val :factor 1))
          b2 (if (= type :base)
               (get
                 base-unit-val :offset 1)
               (get selected-unit-sg :offset 1))
          b1 (if (= type :base)
               (get
                 selected-unit-sg :offset 1)
               (get base-unit-val :offset 1))
          plant-a2 (get base-unit-val :factor 1)
          plant-a1 (get base-unit-val :factor 1)
          plant-b2 (get selected-unit-sg :offset 1)
          plant-b1 (get selected-unit-sg :offset 1)
          plant-factor (/
                         plant-a2 plant-a1)
          plant-offset (-
                         (* plant-a1 plant-b2) (* plant-a1 plant-b1))

          factor (/ a2 a1)
          offset (- (* a1 b2) (* a1 b1))
          plant-min (get-in
                      plant-chart-sensor
                      [sensor-name :min])
          plant-max (get-in
                      plant-chart-sensor
                      [sensor-name :max])
          plant-rfq (get-in
                      plant-chart-sensor
                      [sensor-name :rfq])
          min (:min d)
          max (:max d)
          rfq (:rfq d)]
      (uom-convert k min max rfq type offset factor plant-min plant-max
                   plant-rfq plant-offset plant-factor selected-unit-val))


    (let [a2 (if (= type :base)
               (get
                 base-unit-val :factor 1)
               (get selected-unit-val :factor 1))
          a1 (if (= type :base)
               (get
                 selected-unit-val :factor 1)
               (get base-unit-val :factor 1))
          b2 (if (= type :base)
               (get
                 base-unit-val :offset 1)
               (get selected-unit-val :offset 1))
          b1 (if (= type :base)
               (get
                 selected-unit-val :offset 1)
               (get base-unit-val :offset 1))
          plant-a2 (get base-unit-val :factor 1)
          plant-a1 (get base-unit-val :factor 1)
          plant-b2 (get selected-unit-val :offset 1)
          plant-b1 (get selected-unit-val :offset 1)
          plant-factor (/
                         plant-a2 plant-a1)
          plant-offset (-
                         (* plant-a1 plant-b2) (* plant-a1 plant-b1))

          factor (/ a2 a1)
          offset (- (* a1 b2) (* a1 b1))
          plant-min (get-in
                      plant-chart-sensor
                      [sensor-name :min])
          plant-max (get-in
                      plant-chart-sensor
                      [sensor-name :max])
          plant-rfq (get-in
                      plant-chart-sensor
                      [sensor-name :rfq])
          min (:min d)
          max (:max d)
          rfq (:rfq d)]
      (uom-convert k min max rfq type offset factor plant-min plant-max
                   plant-rfq plant-offset plant-factor selected-unit-val))))

(defn base-selected-uom [type db units charts-sensor base-uom plant-chart-sensor]
  (reduce-kv (fn [col k d]
               (let [sensor-name k
                     sensor-uom-id (get-in db [:sensor (name sensor-name) :uom-id])
                     selected-uom (get-in units [sensor-uom-id :selected-unit-id])
                     uom-units (get-in units [sensor-uom-id :units])
                     selected-unit-val (some (fn [d]
                                               (if (= (d :id) selected-uom)
                                                 d)) uom-units)
                     base-unit-val (first
                                     (remove nil?
                                             (map (fn [d]
                                                    (if (or (= (d :name)
                                                               (:to (sensor-name base-uom)))
                                                                 (= (d :id) (:to (sensor-name base-uom))))
                                                           d)) uom-units)))

                     sensor-value-data (uom-data units k d type
                                                 base-unit-val
                                                 selected-unit-val
                                                 plant-chart-sensor
                                                 sensor-name)]
                 (-> col
                     (conj sensor-value-data))))
             {} charts-sensor))

(defn uom-conversion [db units type data]
  (let [charts-sensor (cond
														 (= type :base) (get-in db [:component :settings
																												:data :charts-config])
														 (= type :change) data
														 :else (get-in db [:plant :settings
																							 :charts-config]))
        plant-chart-sensor (get-in db [:plant :settings :charts-config])
        base-uom (reduce-kv
                   (fn [col index d ]
                     (-> col
                         (conj {(keyword (:name d)) (:uom d)})
                         )) {}
                   (get-in db [:plant :settings :calculated-tags]))
        final-data (base-selected-uom (if (= type :change)
                                        :base
                                        type) db units charts-sensor base-uom
                                      plant-chart-sensor)]
    final-data))

(defn sensor-config [charts-config db units]
  ;(print "units" db)
    (if charts-config
      (uom-conversion db units :init nil)
      (let [sensor-data (reduce-kv (fn [col k d]
                                     (-> col
                                         (conj {(keyword(:name d)) {:min nil
                                                           :max nil
                                                           :rfq nil}})
                                         )) {} @(rf/subscribe [::subs/sensor]))]
        sensor-data)))

(rf/reg-event-fx
  ::init
  [(inject-cofx ::inject/sub [::uom-subs/units])]
  (fn [{:keys [db ::uom-subs/units]} _]
    (let [{:keys [pinned-charts uoms]} @(rf/subscribe [::subs/data])
          charts-config  @(rf/subscribe [::subs/charts-config])
          sensor (sensor-config charts-config db units)]
      {:dispatch-n
       (list [::set-field [:pinned-charts] pinned-charts true]
             [::set-field [:charts-config] sensor true]
						 [::set-field [:validate-charts-config] (get-in db [:plant
																																:settings
																																:charts-config])
							true]
             [::set-field [:uoms] uoms true])})))

(rf/reg-event-db
  ::close
  (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-db
  ::set-field
  (fn [db [_ path value required?]]
    (let [data @(rf/subscribe [::subs/data])]
      (set-field db path value data data-path form-path required? ))))
(rf/reg-event-db
  ::set-field-number
  (fn [db [_ path value required?]]
    (let [data @(rf/subscribe [::subs/data])]
      (set-field db path value data data-path form-path required?))))

(rf/reg-event-fx
  ::update-pinned-charts
  (fn [{:keys [db]} [_ id pin?]]
    (let [data @(rf/subscribe [::subs/data])
          pinned-charts (:value @(rf/subscribe [::subs/field [:pinned-charts]]))
          missing? (not (some #(= id %) pinned-charts))
          modified-data (if pin?
                          (if missing?
                            (into [] (conj pinned-charts id))
                            pinned-charts)
                          (if missing?
                            pinned-charts
                            (into [] (remove #{id} pinned-charts))))
          ]
      {:db (assoc-in db data-path (assoc data :pinned-charts modified-data))
       :dispatch-n (list [::set-field [:pinned-charts] modified-data true])})))




(rf/reg-event-fx
  ::upload
  [(inject-cofx ::inject/sub [::app-subs/client])
   (inject-cofx ::inject/sub [::app-subs/plant])
   (inject-cofx ::inject/sub [::subs/data])
   (inject-cofx ::inject/sub [::uom-subs/units])]
  (fn [{:keys [db ::app-subs/client ::app-subs/plant ::subs/data ::uom-subs/units]} _]
   (merge (when @(rf/subscribe [::subs/can-submit?])
             ;; raise save fx with busy screen and then show confirmation
             {:forward-events {:register ::sync-after-save
                               :events #{::app-event/fetch-plant-success
                                         ::ht-event/service-failure}
                               :dispatch-to [::sync-after-save false]}
              :dispatch [::ht-event/set-busy? false]
              :service/update-plant-settings
                              {:client-id (:id client)
                               :plant-id (:id plant)
                               :settings {:pinned-charts (:pinned-charts data)
                                          :charts-config (uom-conversion db
                                                                         units :base nil)}
                               :evt-success [::app-event/fetch-plant (:id client) (:id plant)]}})
           {:db (-> db
                    (update-in  comp-path assoc :show-error? true)
                    (assoc-in (into data-path  [:validate-charts-config])
															 (uom-conversion db
																							 units :base nil))
                    )})))



(rf/reg-event-fx
  ::sync-after-save
  [(inject-cofx ::inject/sub [::app-subs/plant])]
  (fn [{:keys [db ::app-subs/plant]} [_ close? [eid]]]
    (let [success? (= eid ::app-event/fetch-plant-success)]
      (cond-> {:forward-events {:unregister ::sync-after-save}}
              ;; sync data on success
              success?
              (assoc :db (assoc-in db data-path (-> (select-keys (:settings
                                                                   plant) [:uoms :pinned-charts :charts-config])
                                                    (conj
                                                      {:validate-charts-config
                                                       (get-in db [:plant :settings :charts-config])}))))
              ;; leave if asked for
              (and success? close?)
              (assoc :dispatch [:cpe.component.root.event/activate-content :section nil "home"])))))

(rf/reg-event-fx
  ::set-chart-config
	[(inject-cofx ::inject/sub [::uom-subs/units])]
  (fn [{:keys [db ::uom-subs/units]} [_  id key value]]
    (let [data @(rf/subscribe [::subs/data])
          charts (:value @(rf/subscribe [::subs/field [:charts-config]]))
          missing? (not (some (fn [[k v]]
                                (if (= k (keyword id))
                                  {k (+ v 0)}
                                  nil)
                                ) charts))
          modified-data (if missing?
                          {(keyword id) {key value}}
                          {(keyword id)
                           (conj (charts (keyword id)) {key (if (= value "")
                                                              nil
                                                              value)})})
					final-data  (merge charts modified-data) ]
     {:db (-> db
							(assoc-in data-path (assoc data :charts-config
																							final-data))
							(assoc-in data-path (assoc data :validate-charts-config
																							(uom-conversion db units :change
																															final-data)
																							)))
      :dispatch-n (list [::set-field-number [:charts-config]
                         (merge charts modified-data) true])})))

(rf/reg-event-fx
  ::update-settings-active-tab
  (fn [{:keys [db]} [_ tab-index]]
    {:db (assoc db :settings-active-tab tab-index)}))


