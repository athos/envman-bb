(ns envman.cmd.export
  (:require [babashka.fs :as fs]
            [envman.files :as files]))

(defn export [{:keys [args]}]
  (let [fpath (files/envman-path (first args))]
    (fs/copy fpath ".env")))
