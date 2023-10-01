(ns envman.cmd.import
  (:refer-clojure :exclude [import])
  (:require [babashka.fs :as fs]
            [envman.edit :as edit]
            [envman.files :as files]
            [envman.util :as util]))

(def opts-spec
  [[:name {:coerce util/check-name}]
   [:file {:desc "Specify the input file name. Defaults to `.env`."
           :coerce :string
           :default-desc "<file>"
           :default ".env"
           :alias :f}]
   [:edit {:desc "Edit file before saving"
           :coerce :boolean
           :alias :E}]])

(defn import [{:keys [opts]}]
  (files/ensure-envman-dirs)
  (let [fpath (files/envman-path (:name opts))]
    (fs/copy (:file opts) fpath)
    (when (:edit opts)
      (edit/edit {:to fpath}))))
