(ns cpe.component.root.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [cpe.app.subs :as app-subs]
            [cpe.util.auth :as auth]))

(rf/reg-sub
 ::root
 (fn [db _] (get-in db [:component :root])))

(rf/reg-sub
 ::active-content
 :<- [::root]
 (fn [root _]
   (get-in root [:content :active])))

(rf/reg-sub
 ::active-content-id
 :<- [::root]
 (fn [root _]
   (get-in root [:content :id])))

(rf/reg-sub
 ::app-allowed?
 :<- [::ht-subs/auth-claims]
 (fn [claims _]
   (auth/allow-app? claims)))

(rf/reg-sub
 ::content-allowed?
 :<- [::ht-subs/auth-claims]
 :<- [::active-content]
 (fn [[claims active-content] _]
   (auth/allow-root-content? claims active-content)))

(rf/reg-sub
 ::agreed?
 :<- [::app-subs/user]
 :<- [::ht-subs/topsoe?]
 (fn [[user topsoe?] _]
   (or topsoe? (:agreed? user))))

(rf/reg-sub
 ::menu-open?
 :<- [::root]
 (fn [root [_ menu-id]]
   (or (get-in root [:menu menu-id :open?]) false)))

(rf/reg-sub
  ::uom-open?
  :<- [::root]
  (fn [root [_ uom-id]]
    (or (get-in root [:uom-menu uom-id :open?]) false)))

(rf/reg-sub
  ::section-list
  (fn [db _]
    (let [section-data (get-in db [:section])
          section-list (get-in db [:plant :config :section])]
      (map (fn [section-id]
             (get-in section-data [section-id])
             ) section-list))))
