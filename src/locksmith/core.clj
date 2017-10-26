(ns locksmith.core
  (:require [clojure.string :as string]))

;; taken from plumbing.core

(defmacro for-map
  "Like 'for' for building maps. Same bindings except the body should have a
  key-expression and value-expression. If a key is repeated, the last
  value (according to \"for\" semantics) will be retained.
  (= (for-map [i (range 2) j (range 2)] [i j] (even? (+ i j)))
     {[0 0] true, [0 1] false, [1 0] false, [1 1] true})
  An optional symbol can be passed as a first argument, which will be
  bound to the transient map containing the entries produced so far."
  ([seq-exprs key-expr val-expr]
     `(for-map ~(gensym "m") ~seq-exprs ~key-expr ~val-expr))
  ([m-sym seq-exprs key-expr val-expr]
     `(let [m-atom# (atom (transient {}))]
        (doseq ~seq-exprs
          (let [~m-sym @m-atom#]
            (reset! m-atom# (assoc! ~m-sym ~key-expr ~val-expr))))
        (persistent! @m-atom#))))

(defn map-vals
  "Build map k -> (f v) for [k v] in map, preserving the initial type"
  [f m]
  (cond
   (sorted? m)
   (reduce-kv (fn [out-m k v] (assoc out-m k (f v))) (sorted-map) m)
   (map? m)
   (persistent! (reduce-kv (fn [out-m k v] (assoc! out-m k (f v))) (transient {}) m))
   :else
   (for-map [[k v] m] k (f v))))

(defn ->gql [k type]
  (-> k
      name
      (string/replace "-" "_")
      (string/replace "?" "")
      (#(if (= :enum type)
          %
          (keyword %)))))

(defn ->clj [k type]
  (-> k
      name
      (string/replace "_" "-")
      (#(if (= 'Boolean type)
          (str % "?")
          %))
      keyword))

(defn clj? [k _]
  (and (keyword? k)
       (re-find #"[-\?]" (name k))))

(defn gql? [k type]
  (or (= 'Boolean type)
      (and (keyword? k)
           (re-find #"[_]" (name k)))))

(defn rename-key [{:keys [from to]} path [k {:keys [type]}]]
  (when (gql? k type)
    (let [from-name (from k type)
          to-name (to k type)]

      (if (empty? path)
        (fn [x]
          (if (contains? x from-name)
            (let [v (get x from-name)]
              (-> x (assoc to-name v) (dissoc x from-name)))
            x))

        (fn [x]
          (if (not= ::not-found (get-in x path ::not-found))
            (update-in x path #(if (contains? % from-name)
                                 (let [v (get % from-name)]
                                   (-> % (assoc to-name v) (dissoc from-name)))
                                 %))
            x))))))

(defn merge-aliases
  "Create objects named as aliases, copied from object definitions that they alias"
  [objects aliases]
  (map-vals
   (fn [definition]
     (update definition :fields (fn [fields]
                                  (reduce-kv (fn [fields k alias]
                                               (if (contains? fields alias)
                                                 (assoc fields k (get fields alias))
                                                 fields))
                                             fields
                                             aliases))))
   objects))

(defn renamers
  ([schema type-key converter] (renamers schema type-key converter {}))
  ([schema type-key converter aliases]
   (let [objects (-> (:objects schema)
                     (merge-aliases aliases))
         type-key (if-let [query (get (:queries schema) type-key)]
                    (:type query)
                    type-key)
         enums (:enums schema)
         {:keys [from to]} converter]

     (loop [renames identity
            to-walk [{:type type-key
                      :path []}]]

       (if (empty? to-walk)
         renames

         (let [{:keys [type path] :as walking} (first to-walk)
               to-walk (rest to-walk)]

           (cond
             ;; enums
             (and (keyword? type)
                  (contains? enums type))
             (recur (comp (let [enum-values (get-in enums [type :values])
                                old->new (zipmap (map #(from % :enum) enum-values)
                                                 (map #(to % :enum) enum-values))]
                            (fn [x]
                              (if (not= ::not-found (get-in x path ::not-found))
                                (update-in x path old->new)
                                x)))
                          renames)
                    to-walk)

             ;; objects
             (and (keyword? type)
                  (contains? objects type))
             (let [{:keys [fields]} (get objects type)]
               (recur (comp (apply comp (keep #(rename-key converter path %) fields))
                            renames)
                      (concat to-walk
                              (map (fn [[k {:keys [type]}]]
                                     {:type type
                                      :path (conj path (to k type))})
                                   fields))))

             ;; lists
             (and (list? type)
                  (= 'list (first type)))
             (let [type (second type)
                   renamer (renamers schema type converter aliases)]
               (recur (comp
                       (if (empty? path)
                         (fn [x]
                           (map renamer x))
                         (fn [x]
                           (if (not= ::not-found (get-in x path ::not-found))
                             (update-in x path #(map renamer %))
                             x)))
                       renames)
                      to-walk))

             :else (recur renames to-walk))))))))

(defn clj->gql [schema type & [{:keys [aliases]}]]
  (let [f (renamers schema type {:from ->clj
                                 :to ->gql}
                    aliases)]
    (fn [data]
      (f data))))

(defn gql->clj [schema type & [{:keys [aliases]}]]
  (let [f (renamers schema type {:from ->gql
                                 :to ->clj}
                    aliases)]
    (fn [data]
      (f data))))
