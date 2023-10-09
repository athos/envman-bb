(ns envman.util
  (:require [clojure.string :as str]))

(defn check-name [s]
  (when-not (re-matches #"[-a-zA-Z0-9_.]+" s)
    (throw
     (ex-info (str "name must consist of one or more characters [-a-zA-Z0-9_.], "
                   "but got \"" s "\"")
              {})))
  s)

(defn parse-names [s]
  (let [names (str/split s #",")]
    (when (empty? names)
      (throw (ex-info "names must be one or more comma-separated names" {})))
    (mapv check-name names)))

(defn name-existing-error
  ([name]
   (name-existing-error name nil))
  ([name e]
   (let [msg (str "name \"" name "\" already exists")]
     (if e
       (ex-info msg {} e)
       (ex-info msg {})))))
