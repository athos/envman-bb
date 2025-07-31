(ns envman.query
  (:require
    [clojure.string :as str]))

(defn- safe-operator [op]
  (fn [& args]
    (when (every? some? args)
      (apply op args))))

(def safe+ (safe-operator +))
(def safe- (safe-operator -))
(def safe* (safe-operator *))
(def safe-div (safe-operator /))

(defn- compile-expr [expr]
  (cond (or (number? expr) (string? expr))
        expr

        (= expr :description)
        `(:description ~'m)

        (and (keyword? expr)
             (nil? (namespace expr))
             (str/starts-with? (name expr) "attrs."))
        (let [attr (str/replace (name expr) #"^attrs\." "")]
          `(get (:attrs ~'m) ~attr))

        (and (seq? expr) (seq expr))
        (case (first expr)
          + `(safe+ ~@(map compile-expr (rest expr)))
          - `(safe- ~@(map compile-expr (rest expr)))
          * `(safe* ~@(map compile-expr (rest expr)))
          / `(safe-div ~@(map compile-expr (rest expr))))

        :else
        (throw (ex-info (str "invalid expr: " (pr-str expr)) {}))))

(defn- compile-pattern [pat]
  (re-pattern pat))

(def safe= (safe-operator =))
(def safe!= (safe-operator not=))
(def safe< (safe-operator (comp neg? compare)))
(def safe> (safe-operator (comp pos? compare)))
(def safe<= (safe-operator #(<= (compare %1 %2) 0)))
(def safe>= (safe-operator #(>= (compare %1 %2) 0)))
(def safe-like (comp boolean (safe-operator re-matches)))

(defn- compile-formula [f]
  (cond (and (seq? f) (seq f))
        (case (first f)
          and `(and ~@(map compile-formula (rest f)))
          or `(or ~@(map compile-formula (rest f)))
          not `(not ~(compile-formula (second f)))
          = `(safe= ~@(map compile-expr (rest f)))
          not= `(safe!=)
          < `(safe< ~@(map compile-expr (rest f)))
          > `(safe> ~@(map compile-expr (rest f)))
          <= `(safe<= ~@(map compile-expr (rest f)))
          >= `(safe>= ~@(map compile-expr (rest f)))
          like `(safe-like ~(compile-pattern (nth f 2))
                           ~(compile-expr (nth f 1))))

        (and (keyword? f)
             (nil? (namespace f))
             (str/starts-with? (name f) "tags."))
        (let [tag (str/replace (name f) #"^tags\." "")]
          `(contains? (:tags ~'m) ~tag))

        :else
        (throw (ex-info (str "invalid formula: " (pr-str f)) {}))))

(defn compile-query [query]
  (let [compiled (compile-formula query)]
    (eval
     `(fn [~'m]
        (let [~'m (update ~'m :tags set)]
          ~compiled)))))
