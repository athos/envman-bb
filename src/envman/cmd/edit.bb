(ns envman.cmd.edit
  (:require [envman.edit :as edit]
            [envman.files :as files]
            [envman.util :as util]))

(def opts-spec
  [[:name {:coerce util/check-name}]])

(defn edit [{:keys [opts]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (:name opts))]
    (edit/edit :init-content (slurp fpath) :to fpath)))
