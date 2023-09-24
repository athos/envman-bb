(ns envman.cmd.export
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]))

(defn export [{:keys [args]}]
  (let [names (str/split (first args) #",")
        tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})]
    (doseq [name names
            :let [path (files/envman-path name)
                  content (str/split-lines (slurp path))]]
      (fs/write-lines tmp content {:append true}))
    (fs/move tmp ".env")))
