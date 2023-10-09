(ns envman.files
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(defn envman-dir []
  (fs/path (fs/home) ".envman-bb"))

(defn envman-path [& paths]
  (apply fs/path (envman-dir) "envs" paths))

(defn existing-envman-path [& paths]
  (let [fpath (apply envman-path paths)]
    (when-not (fs/exists? fpath)
      (throw (ex-info (str "no such name exists: " (str/join \/ paths)) {})))
    fpath))

(defn ensure-envman-dirs []
  (fs/create-dirs (envman-path)))
