(ns envman.cmd.export
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]
            [envman.util :as util]))

(def opts-spec
  [[:name {:coerce util/parse-names}]])

(defn export [{{names :name} :opts}]
  (let [tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})]
    (doseq [name names
            :let [path (files/envman-path name)
                  content (str/split-lines (slurp path))]]
      (fs/write-lines tmp content {:append true}))
    (fs/move tmp ".env")))
