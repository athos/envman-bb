(ns envman.files
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn envman-dir []
  (io/file (System/getProperty "user.home") ".envman-bb"))

(defn envman-path [& paths]
  (str (apply io/file (envman-dir) "envs" paths)))

(defn existing-envman-path [& paths]
  (let [fpath (apply envman-path paths)]
    (when-not (fs/exists? fpath)
      (throw (ex-info (str "no such name exists: " (str/join \/ paths)) {})))
    fpath))

(defn ensure-envman-dirs []
  (fs/create-dirs (envman-path)))
