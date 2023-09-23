(ns envman.cmd.create
  (:require [envman.edit :as edit]
            [envman.files :as files]))

(defn create [{:keys [args]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (first args))]
    (edit/edit :to fpath)))
