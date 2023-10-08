(ns envman.edit
  (:require [babashka.process :as proc]
            [babashka.fs :as fs]))

(defn edit [& {:keys [init-content to]}]
  (let [tmp (str (fs/create-temp-file {:posix-file-permissions "rw-------"}))
        editor (System/getenv "EDITOR")]
    (when init-content
      (spit tmp init-content))
    @(proc/process {:in :inherit :out :inherit :err :inherit} editor tmp)
    (if to
      (do (fs/move tmp to {:replace-existing true})
          to)
      tmp)))
