(ns versioning.db
  (:require [cljs.spec.alpha :as s]
            [goog.string :as gstring]
            [com.rpl.specter :as sp]))

(s/def ::id (s/and vector? #(= 3 (count %))))

(defn create-entity [db t data]
  (let [id (keyword (gensym "id"))]
    (-> db
        (assoc-in [t id] [[1 data]]) 
        (update :metadata/new-ids conj id))))

(defn update-entity [db [t id v :as uid] data]
  (dissoc
    (if-let [b (get-in db [t id])]
      (let [[lv] (last b)]
        (if (not= lv v)
          (throw (js/Error {:message (gstring/format "changes can only be appended to the end, %s/%s/%s" t id lv)
                            :type :invalid-append-position}))
          (update-in db [t id] conj [(inc lv) data])))
      (throw (js/Error {:message (gstring/format "entity %s/%s/%s does not exist" t id v)
                        :type :entity-not-found})))
    db :metadata/new-ids))

(defn get-entity-versions [db [t id]]
  (map first (get-in db [t id])))

(defn get-entity [db [t id sv :as uid]]
  (let [b (get-in db [t id])
        ret (cond->> b sv (filter (fn [[v d :as e]] (when (<= v sv) d))))]
    [[t id (-> ret last first)] (apply merge (map second ret))]))

(defn ref? [r]
  (and (coll? r) (= :ref (first r))))

(defn expand-entity [db e]
  (sp/transform [(sp/walker ref?)]
                (fn [[_ id]] [:ref (get-entity db id)])
                e))

(defn get-expanded-entity [db id]
  (->> id
       (get-entity db)
       (expand-entity db)))
