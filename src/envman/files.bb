(ns envman.files
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]))

(defn envman-dir []
  (io/file (System/getProperty "user.home") ".envman-bb"))

(defn envman-path [& paths]
  (str (apply io/file (envman-dir) "envs" paths)))

(defn ensure-envman-dirs []
  (fs/create-dirs (envman-path)))
