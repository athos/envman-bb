(ns envman.cmd.edit
  (:require [envman.edit :as edit]
            [envman.files :as files]
            [envman.util :as util]
            [babashka.fs :as fs]))

(def opts-spec
  [[:name {:coerce util/check-name}]])

(defn edit [{:keys [opts]}]
  (files/ensure-files-dir)
  (let [fpath (files/existing-name-path (:name opts))
        time-created (fs/creation-time fpath)]
    (edit/edit :init-content (slurp (fs/file fpath)) :to fpath :force true)
    (fs/set-creation-time fpath time-created)))
