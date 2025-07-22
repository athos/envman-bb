(ns envman.envset
  (:require [clj-yaml.core :as yaml]
            [envman.dotenv :as dotenv]))

(defn- parse-meta [s]
  (if-let [[_ more] (re-matches #"(?sm)^---\n(.*)" s)]
    (if-let [[_ pre post] (re-matches #"(?sm)(.*?)^---\n?(.*)" more)]
      [(yaml/parse-string pre) post]
      (throw (ex-info "a line that only contains `---` expected, but not found" {})))
    [nil s]))

(defn parse
  ([s] (parse s {}))
  ([s opts]
   (let [[meta body] (parse-meta s)]
     {:meta meta, :body (dotenv/parse body opts)})))
