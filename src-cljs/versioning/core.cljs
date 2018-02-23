(ns versioning.core
  (:require [reagent.core :as r]
            [re-frame.core :as re]
            [versioning.db :as db]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs.pprint :as pp]))

(defn on-js-reload []
  (r/force-update-all))

(defn data->hiccup [entities data store path]
  (doall
    (for [[k v] data
            :let [path (conj path k)
                  id (clojure.string/join "," path)]]
      (into ^{:key id} [:div [:label {:for id} k]]
              (if (map? v)
                [(data->hiccup entities v store path)]
                [(cond
                   (and (coll? v) (= :ref (first v))) (into [:select {:on-change #(swap! store assoc-in path [:ref (cljs.reader/read-string (-> % .-target .-value))])}]
                                                           (let [[st sid sv :as uid] (second v)]
                                                             (for [v (db/get-entity-versions entities uid)]
                                                               [:option {:value (pr-str [st sid v])
                                                                         :selected (= sv v)} (str v)])))
                   (coll? v) (into [:div] (for [e v]
                                            [:div [:input {:id id
                                                           :defaultValue e}]]))
                   :else ^{:key (gensym)} [:input {:id id
                                                   :on-change #(swap! store assoc-in path (-> % .-target .-value))
                                                   :defaultValue v}])])))))

(defn data-component [id data store]
  (let [entities @(re/subscribe [:entities])]
    [:div
     (into [:form
            [:div (clojure.string/join "/" id)]]
           (data->hiccup entities data store []))
     [:div [:button {:on-click #(re/dispatch [:save id @store])} "Save"]]]))

(defn entity-details-component [entity-selection version-selection]
  (let [entities @(re/subscribe [:entities])]
    [:div "Entity Details Component"
     (if (and @entity-selection @version-selection)
       (let [[id data] (db/get-entity entities (conj @entity-selection @version-selection))]
         [data-component id data (r/atom {})])
       [:div "No concrete entity selected"])]))

(defn entity-versions-component [entity-selection version-selection]
  (let [entities @(re/subscribe [:entities])]
    (into [:div "Entities Versions Component"]
          (for [v (db/get-entity-versions entities @entity-selection)]
            [:span {:on-click #(reset! version-selection v)} v]))))

(defn entity-overview-component [entity-selection]
  [:div "Entity Overview Component"
   (let [version-selection (r/atom nil)]
     (if @entity-selection
       [:div
        [entity-versions-component entity-selection version-selection]
        [entity-details-component entity-selection version-selection]]
       [:div "No entity selected"]))])

(defn entities-list-component [selection]
  [:div "Entities List Component"
   (let [entities @(re/subscribe [:entities])]
     (if (empty? entities)
       [:div "No entities found"]
       (into [:div]
             (for [[type v] entities]
               (into [:div (name type)]
                     (for [[id entities] v]
                       [:div {:on-click #(reset! selection [type id])} (str (name type) "/" (name id))]))))))])

(defn ui []
  (let [selected-entity (r/atom nil)]
    [:div
     [entities-list-component selected-entity]
     [:br]
     [entity-overview-component selected-entity]
     [:br]
     [:div "Database Components"
      (let [entities @(re/subscribe [:entities])]
        (into [:div] (apply concat (map (fn [o] [(str o) [:br]]) (clojure.string/split (with-out-str (pp/pprint entities)) #"\n")))))]]))

(defn ui-root []
  ; do not add any ui components to this root element
  ; as those won't be redrawn in case of changes. Changes
  ; happening next to this root node are picked up and the
  ; ui is automatically updated
  [ui])

; platform within regions from dictionaries

(defn setup-entities []
  (let [db (-> {}
               (db/create-entity :p {:name "redis"})
               (db/create-entity :ta {:metrics ["instances"]}))
        [ta_id p_id] (:metadata/new-ids db)
        db (-> db
               (db/update-entity [:p p_id 1] {:technical-asset [:ref [:ta ta_id 1]] 
                                              :price {:amount 5 :metric "instances"}})
               (db/update-entity [:ta ta_id 1] {:metrics ["instances" "usage hours"]})
               (db/update-entity [:p p_id 2] {:technical-asset [:ref [:ta ta_id 2]] 
                                              :price {:amount 5 :metric "usage hours"}}))]
    db))

(re/reg-event-db :initialize
                 (fn [_ _]
                   {:entities (setup-entities)}))

(re/reg-event-fx :save
                 (fn [{:keys [db]} [_ id data]]
                   (let [entities (:entities db)]
                     (try
                       {:db (assoc db :entities (db/update-entity entities id data))}  
                       (catch :default e
                         (js/alert e))))))

(re/reg-sub :entities
            (fn [{:keys [entities]} [_ type-filter]]
              (if type-filter
                (select-keys entities type-filter)
                entities)))

(defn ^:export run
  []
  (re/dispatch-sync [:initialize])
  (r/render [ui-root]
            (js/document.getElementById "app")))

(comment
  (db/get-expanded-entity db [:p p_id 2]))

;lein repl
;(use 'figwheel-sidecar.repl-api)
;(start-figwheel!)

;in vim
;:Piggieback (figwheel-sidecar.repl-api/repl-env)
