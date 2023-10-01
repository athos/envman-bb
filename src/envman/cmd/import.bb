(ns envman.cmd.import
  (:refer-clojure :exclude [import])
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [envman.files :as files]
            [envman.util :as util]))

(def opts-spec
  [[:name {:coerce util/check-name}]])

(defn import [{:keys [opts]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (:name opts))
        editor (System/getenv "EDITOR")]
    (fs/copy ".env" fpath)
    @(proc/process {:in :inherit :out :inherit :err :inherit} editor fpath)))
