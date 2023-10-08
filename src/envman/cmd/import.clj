(ns envman.cmd.import
  (:refer-clojure :exclude [import])
  (:require [babashka.fs :as fs]
            [envman.edit :as edit]
            [envman.files :as files]
            [envman.util :as util])
  (:import [java.nio.file FileAlreadyExistsException]))

(def opts-spec
  [[:name {:coerce util/check-name}]
   [:file {:desc "Specify the input file name. Defaults to `.env`."
           :coerce :string
           :default-desc "<file>"
           :default ".env"
           :alias :f}]
   [:edit {:desc "Edit file before saving"
           :coerce :boolean
           :alias :E}]
   [:force {:desc "Overwrite the existing name without error if it exists"
            :coerce :boolean}]])

(defn import [{:keys [opts]}]
  (files/ensure-envman-dirs)
  (let [name (:name opts)
        fpath (files/envman-path name)]
    (try
      (fs/copy (:file opts) fpath {:replace-existing (:force opts)})
      (when (:edit opts)
        (edit/edit {:to fpath}))
      (catch FileAlreadyExistsException e
        (throw (ex-info (str "name \"" name "\" already exists") {} e))))))
