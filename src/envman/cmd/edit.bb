(ns envman.cmd.edit
  (:require [envman.edit :as edit]
            [envman.files :as files]))

(defn edit [{:keys [args]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (first args))]
    (edit/edit :init-content (slurp fpath) :to fpath)))
