(ns envman.cmd.import
  (:refer-clojure :exclude [import])
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [envman.files :as files]))

(defn import [{:keys [args]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (first args))
        editor (System/getenv "EDITOR")]
    (fs/copy ".env" fpath)
    @(proc/process {:in :inherit :out :inherit :err :inherit} editor fpath)))
