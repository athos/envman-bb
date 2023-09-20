(ns envman.cmd.list
  (:refer-clojure :exclude [list])
  (:require [envman.files :as files]
            [babashka.fs :as fs]))

(defn list [_]
  (doseq [file (fs/list-dir (files/envman-path))
          :when (not (fs/directory? file))]
    (println (fs/file-name file))))
