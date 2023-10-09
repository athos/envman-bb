(ns envman.cmd.create
  (:require [envman.edit :as edit]
            [envman.files :as files]
            [envman.util :as util]))

(def opts-spec
  [[:name {:coerce util/check-name}]])

(defn create [{:keys [opts]}]
  (files/ensure-files-dir)
  (let [fpath (files/name-path (:name opts))]
    (files/ensure-parent-dirs fpath)
    (edit/edit :to fpath)))
