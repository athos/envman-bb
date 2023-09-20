(ns envman.cmd.edit
  (:require [babashka.process :as proc]
            [envman.files :as files]))

(defn edit [{:keys [args]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (first args))
        editor (System/getenv "EDITOR")]
    @(proc/process {:in :inherit :out :inherit :err :inherit} editor fpath)))
