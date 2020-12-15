(ns cpe.app.scroll
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :refer [use-style use-sub-style]]
            [ht.style :refer [color-hex]]
            [ht.util.interop :as i]
            [ht.util.common :refer [add-event remove-event
                                    get-control-pos]]
            [cpe.util.common :refer [motion spring]]
            [cpe.app.style :as app-style]
            [ht.util.common :as u]))

(defn scroll-bar [{:keys [h? length page-f pos-f on-scroll]}]
  (let [state (atom {:length length
                     :page-f page-f
                     :pos-f pos-f
                     :on-scroll on-scroll})
        do-drag (fn [e]
                  (let [{:keys [length page-f on-scroll drag?
                                  start-pos-f start-x start-y]} @state]
                    (if (and drag? (fn? on-scroll))
                      (let [{:keys [page-x page-y]} (get-control-pos e)
                            l (if h? (- page-x start-x) (- page-y start-y))
                            p (+ start-pos-f (/ l length (- 1 page-f)))
                            new-pos-f (if (> 0 p) 0
                                          (if (< 1 p) 1  p))]
                        ;; (js/console.log "new pos: " new-pos-f)
                        (on-scroll new-pos-f)))))
        end-drag (fn _ed [e]
                   (let [{:keys [drag? ev-move ev-end]} @state]
                     (when drag?
                       ;; (js/console.log "drag end")
                       (swap! state dissoc :drag? :start-x :start-y :start-pos-f)
                       (remove-event js/window ev-move do-drag)
                       (remove-event js/window ev-end _ed))))
        start-drag (fn [touch? e]
                     (i/ocall e :preventDefault)
                     (i/ocall e :stopPropagation)
                     (let [[ev-move ev-end]
                           (if touch? ["touchmove" "touchend"]
                               ["mousemove" "mouseup"])
                           {:keys [page-x page-y]} (get-control-pos e)]
                       ;; (js/console.log "drag start")
                       (swap! state (fn [s]
                                      (assoc s
                                             :drag? true, :start-pos-f (:pos-f s)
                                             :ev-move ev-move, :ev-end ev-end
                                             :start-x page-x, :start-y page-y)))
                       (add-event js/window ev-move do-drag)
                       (add-event js/window ev-end end-drag)))
        on-click-bar (fn [e]
                       (let [{:keys [length page-f on-scroll]} @state]
                         (if (fn? on-scroll)
                           (let [rect (-> e
                                          (i/oget :target)
                                          (i/ocall :getBoundingClientRect))
                                 pos (if h?
                                       (- (i/oget e :clientX) (i/oget rect :left))
                                       (- (i/oget e :clientY) (i/oget rect :top)))
                                 p (/ (- pos (* length page-f 0.5))
                                      (* length (- 1 page-f)))
                                 new-pos-f (if (> 0  p) 0
                                               (if (< 1 p) 1
                                                   p))]
                             ;; (js/console.log "new pos:" new-pos-f)
                             (on-scroll new-pos-f)))))
        on-click-track #(doto %
                          (i/ocall :preventDefault)
                          (i/ocall :stopPropagation))]

    (fn [{:keys [h? length page-f pos-f on-scroll]
         :or {h? false, pos-f 0}}]
      (let [length (- length 8)
            page-l (* length page-f)
            pos-l (* (- length page-l) pos-f)]
        (swap! state assoc :on-scroll on-scroll
               :length length, :page-f page-f, :pos-f pos-f)
        [:div (-> (use-sub-style app-style/scroll-bar
                                 (if h? :bar-h :bar-v))
                  (update :style merge
                          (if h? {:width length} {:height length}))
                  (assoc :on-click on-click-bar))
         [:div (update (use-sub-style app-style/scroll-bar
                                      (if h? :line-h :line-v))
                       :style merge
                       (if h? {:width length} {:height length}))]
         [:div (-> (use-sub-style app-style/scroll-bar
                                  (if h? :track-h :track-v))
                   (update :style merge
                           (if h?
                             {:left pos-l, :width page-l}
                             {:top pos-l, :height page-l}))
                   (merge {:on-click on-click-track
                           :on-mouse-down (partial start-drag false)
                           :on-touch-start (partial start-drag true)
                           :on-mouse-up end-drag
                           :on-touch-end end-drag}))
          [:div (use-sub-style app-style/scroll-bar :track)]]]))))

(defn- f-> [s p f]
  (if (<= s p) 0 (* f (- s p))))

(defn- f<- [s p o]
  (if (<= s p) 0 (let [f (/ o (- s p))]
                   (if (< f 0) 0
                       (if (> f 1) 1 f)))))

(defn- update-f [sh h t sw w l]
  (let [hf (f<- sh h t)
        t (f-> sh h hf)
        wf (f<- sw w l)
        l (f-> sw w wf)]
    [hf t wf l]))

(defn- update-l [{:keys [sw w] :as state} wf]
  (assoc state :wf wf, :l (f-> sw w wf)))

(defn- update-t [{:keys [sh h] :as state} hf]
  (assoc state :hf hf, :t (f-> sh h hf)))

(defn- update-on-wheel [state e]
  (let [dx (i/oget e :deltaX)
        dy (i/oget e :deltaY)
        shift? (i/ocall e :getModifierState "Shift")
        [dx dy] (if shift? [dy dx] [dx dy])
        {:keys [sh h hf t sw w wf l]} @state]
    (if (some (fn [[s l f d]]
                (and (> s l)
                     (if (pos? d) (> 1 f) (< 0 f))))
              [[sw w wf dx] [sh h hf dy]])
      (let [[hf t wf l] (update-f sh h (+ t dy)
                                  sw w (+ l dx))]
        (i/ocall e :preventDefault)
        (i/ocall e :stopPropagation)
        (swap! state assoc :hf hf, :wf wf, :t t, :l l)))))

(defn- scroll-to [state {:keys [top left]}]
  (swap! state (fn [{:keys [sh h sw w] :as state}]
                 (let [[hf t wf l] (update-f sh h top
                                             sw w left)]
                   (assoc state :hf hf, :wf wf, :t t, :l l)))))

(defn- prevent-scroll [element event]
  (when (= element (i/oget event :target))
    ;; (js/console.log "prevent scroll")
    (if (pos? (i/oget element :scrollTop))
      (i/oset element :scrollTop 0))
    (if (pos? (i/oget element :scrollLeft))
      (i/oset element :scrollLeft 0))))

(defn lazy-scroll-box
  "[{:keys [width height scroll-width scroll-height
            style class-name body-style body-class-name render-fn]}]
  **props**: a map with width, height, scroll-width, scroll-height,
  style, class-name, body-style, body-class-name  
  **render-fn**: a function to render the content, it will be passed
  a map with top, left, height, width, scroll-height, scroll-width.
  Also a function is passed in as second parameter to it which can be
  called to scroll it to a specified position. Provide the new position
  as the top left coordinate in the scroll pane like {:top top, :left left}."
  [{:keys [width height scroll-width scroll-height skip-IE-animation?]}]
  ;; (js/console.log "lazy:" [width height scroll-width scroll-height])
  (let [ele (atom nil)
        prevent-scroll #(prevent-scroll @ele %)
        state (r/atom {:wf 0, :hf 0, :l 0, :t 0
                       :h height, :w width
                       :sh scroll-height, :sw scroll-width})
        update-box (fn [state new]
                     (let [state (merge state new)
                           {:keys [sh sw h w t l]} state
                           [hf t wf l] (update-f sh h t sw w l)]
                       (assoc state
                              :hf hf, :wf wf
                              :t t, :l l)))
        scroll-to (partial scroll-to state)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [node (dom/dom-node this)]
                               (add-event node "wheel"
                                          (partial update-on-wheel state))))
      :component-will-receive-props (fn [this [_ props]]
                                      ;; (js/console.log "props:" props)
                                      (let [{sh :scroll-height, sw :scroll-width
                                             h :height, w :width} props
                                            old (select-keys @state [:sh :sw :h :w])
                                            new {:sh sh, :sw sw, :h h, :w w}]
                                        (when (not= old new)
                                          ;; (js/console.log "receive-props:" [old new])
                                          (swap! state update-box new))))

      :reagent-render
      (fn [props]
        (let [{:keys [style class-name body-style body-class-name render-fn]} props
              {:keys [sh h hf t, sw w wf l]} @state
              body-fn (fn [t l]
                        [:div {:style (assoc body-style
                                             :width sw, :height sh
                                             :position "absolute"
                                             :top (- t), :left (- l))
                               :class-name body-class-name}
                         (if render-fn (render-fn {:top t, :left l
                                                   :height h, :scroll-height sh
                                                   :width w, :scroll-width sw}
                                                  scroll-to))])]
          [:div {:ref #(reset! ele %)
                 :style (assoc style :width w :height h
                               :overflow "hidden"
                               :position "relative")
                 :class-name class-name
                 :on-scroll prevent-scroll}
           (if (and skip-IE-animation? (u/browser-IE?))
             (body-fn t l)
             [motion {:defaultStyle #js{:t 0, :l 0}
                      :style #js{:t (spring t), :l (spring l)}}
              (fn [s]
                (let [t (i/oget s :t)
                      l (i/oget s :l)]
                  ;; (js/console.log (.valueOf (js/Date.)) t l)
                  (r/as-element (body-fn t l))))])
           (if (and h sh (< h sh))
             [scroll-bar {:h? false
                          :length h
                          :page-f (/ h sh)
                          :pos-f hf
                          :on-scroll #(swap! state update-t %)}])
           (if (and w sw (< w sw))
             [scroll-bar {:h? true
                          :length w
                          :page-f (/ w sw)
                          :pos-f wf
                          :on-scroll #(swap! state update-l %)}])]))})))

(defn scroll-box [props & children]
  (let [state (r/atom {:wf 0, :hf 0, :l 0, :t 0})
        update-box (fn [state node]
                     (let [bn (or node (:box-node state))
                           {:keys [sh sw t l]} state
                           h (i/oget bn :clientHeight)
                           w (i/oget bn :clientWidth)
                           [hf t wf l] (update-f sh h t sw w l)]
                       ;; (js/console.log "bn: " sh h t hf)
                       (assoc state :box-node bn
                              :h h, :w w
                              :hf hf, :wf wf
                              :t t, :l l)))
        update-scroll (fn [state node]
                        (let [sn (or node (:scroll-node state))
                              {:keys [h w t l]} state
                              sh (i/oget sn :scrollHeight)
                              sw (i/oget sn :scrollWidth)
                              [hf t wf l] (update-f sh h t sw w l)]
                          ;; (js/console.log "sn: " sh h t hf)
                          (assoc state :scroll-node sn
                                 :sh sh, :sw sw
                                 :hf hf, :wf wf
                                 :t t, :l l)))
        changed? (fn [so sn]
                   (let [[h? w?]
                         (->> [[:sh :h :hf] [:sw :w :wf]]
                              (map (fn [[sj j jf]]
                                     (let [{sho sj, ho j, hfo jf} so
                                           {shn sj, hn j, hfn jf} sn]
                                       (or (< 0.1 (js/Math.abs (- sho shn)))
                                           (< 0.1 (js/Math.abs (- ho hn)))
                                           (< 1e-6 (js/Math.abs (- hfo hfn))))))))]
                     ;; (js/console.log [w? h?])
                     (or w? h?)))

        ;; inner scroll body
        body (fn [_ children]
               (r/create-class
                {:component-did-mount (fn [this]
                                        ;; (js/console.log "body mounted")
                                        (swap! state update-scroll
                                               (dom/dom-node this)))
                 :component-did-update (fn [_ _]
                                         ;; (js/console.log "body updated")
                                         (js/setTimeout
                                          #(let [o @state
                                                 n (update-scroll o nil)]
                                             (when (changed? o n)
                                               ;; (js/console.log n)
                                               (swap! state merge n)))
                                          50))
                 :reagent-render
                 (fn [_ children]
                   (let [{:keys [t l]} @state]
                     [motion {:defaultStyle #js{:t 0, :l 0}
                              :style #js{:t (spring t), :l (spring l)}}
                      (fn [s]
                        (let [t (i/oget s :t)
                              l (i/oget s :l)]
                          (r/as-element
                           (into [:div {:style {:position "absolute"
                                                :top (- t) :left (- l)}}]
                                 children))))]))}))]

    ;; outer scroll box
    (let [ele (atom nil)
          prevent-scroll #(prevent-scroll @ele %)]
      (r/create-class
       {:component-did-mount (fn [this]
                               (let [node (dom/dom-node this)]
                                 ;; (js/console.log "box mounted")
                                 ;; TODO: later add a timer based update call
                                 ;; to check changes in sizes by flex etc.
                                 (add-event node "wheel"
                                            (partial update-on-wheel state))
                                 (swap! state update-box node)))
        :component-did-update (fn [_ _]
                                ;; (js/console.log "box updated")
                                (let [o @state
                                      n (update-box o nil)]
                                  (when (changed? o n)
                                    ;; (js/console.log n)
                                    (swap! state merge n))))
        :reagent-render
        (fn [{:keys [style] :as props} & children]
          (let [{:keys [sw w wf, sh h hf]} @state]
            ;; (js/console.log "wf:" wf "hf:" hf)
            [:div (-> props
                      (dissoc props :*-force-render)
                      (assoc :style (merge style {:overflow "hidden"
                                                  :position "relative"})
                             :ref #(reset! ele %)
                             :on-scroll prevent-scroll))
             [body {:props props} children] ;; do pass the props to ensure re-render
             (if (and h sh (< h sh))
               [scroll-bar {:h? false
                            :length h
                            :page-f (/ h sh)
                            :pos-f hf
                            :on-scroll #(swap! state update-t %)}])
             (if (and w sw (< w sw))
               [scroll-bar {:h? true
                            :length w
                            :page-f (/ w sw)
                            :pos-f wf
                            :on-scroll #(swap! state update-l %)}])]))}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- show-grid-cell [state row col]
  (let [{:keys [scroll-to top left width height cell-width cell-height]} @state
        cell-top (* row cell-height)
        cell-bottom (+ cell-top cell-height)
        cell-left (* col cell-width)
        cell-right (+ cell-left cell-width)
        right (+ left width)
        bottom (+ top height)
        left (if (> left cell-left) cell-left
                 (if (< right cell-right) (- cell-right width) left))
        top (if (> top cell-top) cell-top
                (if (< bottom cell-bottom) (- cell-bottom height) top))]
    (scroll-to {:top top, :left left})))

(defn lazy-grid
  "[{:keys [width height cell-width cell-height
            row-count col-count cells-render-fn]}]  
  Good for showing long list of long list of dom heavy items arranged in
  rows and columns with both horizontal and vertical scrollbars.
  It renders only those visible.  
  **cells-render-fn**: (fn [row-list col-list show-cell])  
  It should return a 2D sequence of sequence of hiccups arranged as rows of cols,
  with one for each index in **row-list** and **col-list**.
  You can scroll to bring an item into view by calling the **show-cell**
  function which takes two arguments *row* and *col*."
  [props]
  ;;TODO:
  (let [state (atom {})
        render-fn
        (fn [{:keys [top left]} scroll-to]
          (let [{:keys [width height cell-width cell-height
                        row-count col-count cells-render-fn]}
                (swap! state assoc :top top, :left left, :scroll-to scroll-to)
                from-row (quot top cell-height)
                to-row (min row-count (js/Math.ceil (/ (+ top height) cell-height)))
                from-col (quot left cell-width)
                to-col (min col-count (js/Math.ceil (/ (+ left width) cell-width)))
                cells (cells-render-fn (range from-row to-row)
                                       (range from-col to-col)
                                       (partial show-grid-cell state))]
            (doall
             (mapcat (fn [row-cells i]
                       (let [row (+ from-row i)]
                         (map (fn [cell j]
                                (let [col (+ from-col j)]
                                  [:span {:key (str row "," col)
                                          :style {:display "block"
                                                  :margin "0", :padding "0"
                                                  :position "absolute"
                                                  :top (* cell-height row)
                                                  :left (* cell-width col)}}
                                   cell]))
                              row-cells (range))))
                     cells (range)))))]
    (fn [{:keys [width height cell-width cell-height
                skip-IE-animation?
                row-count col-count] :as props}]
      (let [scroll-height (* cell-height row-count)
            scroll-width (* cell-width col-count)]
        (swap! state merge props)
        [lazy-scroll-box
         {:width width, :height height
          :scroll-width scroll-width, :scroll-height scroll-height
          :render-fn render-fn
          :skip-IE-animation? skip-IE-animation?}]))))

(defn lazy-rows
  "[{:keys [width height item-count item-height items-render-fn]}]  
  Good for showing long list of dom heavy items arranged in rows with
  a vertical scrollbar. It renders only those visible.  
  **items-render-fn**: (fn [index-list show-item])  
  It should return a sequence of hiccups , for each index in **index-list**.
  You can scroll to bring an item into view by calling the **show-item**
  function which takes a single argument *item-index*."
  [{:keys [width height item-count item-height items-render-fn
           skip-IE-animation?]}]
  [lazy-grid {:width width, :height height
              :cell-width width, :cell-height item-height
              :row-count item-count, :col-count 1
              :skip-IE-animation? skip-IE-animation?
              :cells-render-fn
              (fn [row-list col-list show-cell]
                (let [show-item #(show-cell % 0)]
                  (->> (items-render-fn row-list show-item)
                       (map list))))}])

(defn lazy-cols
  "[{:keys [width height item-count item-width items-render-fn]}]  
  Good for showing long list of dom heavy items arranged in columns with
  a horizontal scrollbar. It renders only those visible.  
  **items-render-fn**: (fn [index-list show-item])  
  It should return a sequence of hiccups , for each index in **index-list**.
  You can scroll to bring an item into view by calling the **show-item**
  function which takes a single argument *item-index*."
  [{:keys [width height item-count item-width items-render-fn
           skip-IE-animation?]}]
  [lazy-grid {:width width, :height height
              :cell-width item-width, :cell-height height
              :row-count 1, :col-count item-count
              :skip-IE-animation? skip-IE-animation?
              :cells-render-fn
              (fn [row-list col-list show-cell]
                (let [show-item #(show-cell 0 %)]
                  (list (items-render-fn col-list show-item))))}]) []


(defn table-grid [{:keys [height, width,
                          row-header-width, col-header-height,
                          row-count, col-count,
                          row-height, col-width,
                          row-header-renderer, col-header-renderer,
                          cell-renderer, corner-renderer,
                          table-count, gutter, labels, padding
                          skip-IE-animation?]}]
  (let [[table-count-y table-count-x] table-count
        [padding-left padding-top padding-right padding-bottom] padding
        [gutter-width gutter-height] gutter
        ;; calculate scroll height
        scroll-height (- (* table-count-y (+ col-header-height gutter-height
                                             (* row-count row-height))) gutter-height)
        ;; calculate scroll width
        scroll-width (- (* table-count-x (+ row-header-width gutter-width
                                            (* col-count col-width))) gutter-width)
        [label-row label-column] labels]

    [:div
     [lazy-scroll-box
      {:height        height
       :width         width
       :scroll-height scroll-height, :scroll-width scroll-width
       :style         {:font-size "10px"}
       :skip-IE-animation? skip-IE-animation?
       :render-fn
       (fn [{:keys [top left]}]
         (let [height (- height padding-top padding-bottom)
               width (- width padding-right padding-left)
               table-height (/ (- height
                                  (* gutter-height (dec table-count-y))) table-count-y)
               table-width (/ (- width
                                 (* gutter-width (dec table-count-x))) table-count-x)
               max-left (- scroll-width width)
               max-top (- scroll-height height)
               table-render-width (* col-count col-width)
               table-render-height (* row-count row-height)
               scroll-x (* -1 left (/ (- table-render-width
                                         (- table-width row-header-width)) max-left))
               scroll-y (* -1 top (/ (- table-render-height
                                        (- table-height col-header-height)) max-top))
               col-render-start (min
                                 (quot (- scroll-x) col-width) col-count)
               col-render-end (min (+ 2 col-render-start
                                      (quot (- table-width gutter-width row-header-width)
                                            col-width)) col-count)
               row-render-start (min (quot (- scroll-y)
                                           row-height) row-count)
               row-render-end (min (+ 2 row-render-start
                                      (quot (- table-height gutter-height col-header-height)
                                            row-height)) row-count)
               label-row (if label-row label-row "Rows")
               label-row (if label-row label-row "Rows")
               set-area (fn [[top left height width]]
                          {:position "absolute" :top top :left left
                           :height   height :width width})

               corner-renderer
               (or corner-renderer
                 (fn []
                   (if (and label-column
                            label-row)
                     [:div {:style {:height        "inherit"
                                    :border-right  "1px solid"
                                    :border-bottom "1px solid"
                                    :border-color (color-hex :sky-blue)}}
                      [:svg {:height col-header-height
                             :width  row-header-width
                             :style  {:font-size "10px"}}
                       [:text {:x (- (/ row-header-width 2) 5), :y 8
                               :fill (color-hex :sky-blue)}
                        label-column]
                       [:line {:x1    "0" :y1 "0" :x2 row-header-width
                               :y2    col-header-height
                               :style {:stroke (color-hex :sky-blue)
                                       :stroke-width "1"}}]
                       [:text {:x 10, :y (- col-header-height 5)
                               :fill (color-hex :sky-blue)}
                        label-row]]]
                     [:div])))]

           [:div {:style (set-area [top left height width])}
            ;; Rows of tables
            (map (fn [table-row-no] ^{:key table-row-no}
                   [:div {:style (set-area [(* table-row-no
                                               (+ gutter-height table-height)) 0])}
                    ;; Column of tables
                    (map (fn [table-col-no] ^{:key table-col-no}
                           [:div {:style (set-area [0 (* table-col-no
                                                         (+ gutter-width table-width))])}
                            [:div {:style {:height col-header-height}}
                             [:div {:style (set-area [padding-top padding-left
                                                      col-header-height row-header-width])}
                              [corner-renderer]]
                             [:div {:style (merge {:overflow "hidden"}
                                                  (set-area [padding-top
                                                             (+ padding-left row-header-width) col-header-height
                                                             (- table-width row-header-width)]))}
                              [:div {:style (set-area [nil scroll-x nil table-render-width])}
                               (map (fn [%] ^{:key %}
                                      [:div {:style (set-area [nil (* % col-width) col-header-height col-width])}
                                       [col-header-renderer % [table-row-no table-col-no]]])
                                    (range col-render-start col-render-end))]]]
                            [:div
                             [:div {:style (merge {:overflow "hidden"}
                                                  (set-area [(+ col-header-height padding-top)
                                                             padding-left (- table-height col-header-height)
                                                             row-header-width]))}
                              [:div {:style (set-area [scroll-y nil table-render-height row-header-width])}
                               (map (fn [%] ^{:key %}
                                      [:div {:style (set-area [(* % row-height) nil row-height col-width])}
                                       [row-header-renderer % [table-row-no table-col-no]]])
                                    (range row-render-start row-render-end))]]
                             [:div {:style (merge {:overflow "hidden"}
                                                  (set-area [(+ col-header-height padding-top) (+ row-header-width padding-left)
                                                             (- table-height col-header-height) (- table-width row-header-width)]))}
                              [:div {:style (set-area [scroll-y scroll-x table-render-height table-render-width])}
                               (map (fn [rowno]
                                      (map (fn [colno]
                                             ^{:key (str rowno "-" colno)}
                                             [:div {:style (set-area [(* rowno row-height)
                                                                      (* colno col-width) row-height col-width])}
                                              [cell-renderer rowno colno [table-row-no table-col-no]]])
                                           (range col-render-start col-render-end)))
                                    (range row-render-start row-render-end))]]]])
                         (range table-count-x))])
                 (range table-count-y))]))}]]))
