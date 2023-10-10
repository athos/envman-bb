(ns envman.files
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(defn envman-home []
  (fs/path (fs/home) ".envman-bb"))

(defn envman-files-dir [& paths]
  (apply fs/path (envman-home) "files" paths))

(defn name-path [name]
  (apply envman-files-dir (str/split name #"/")))

(defn existing-name-path [name]
  (let [fpath (name-path name)]
    (when-not (fs/exists? fpath)
      (throw (ex-info (str "no such name exists: " name) {})))
    fpath))

(defn matching-paths [pattern]
  (sort (fs/glob (envman-files-dir) pattern)))

(defn ensure-parent-dirs [path]
  (when-let [p (fs/parent path)]
    (fs/create-dirs p {:posix-file-permissions "rwx------"})))

(defn ensure-files-dir []
  (fs/create-dirs (envman-files-dir) {:posix-file-permissions "rwx------"}))
