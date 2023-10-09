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
            :coerce :boolean
            :alias :F}]])

(defn import [{:keys [opts]}]
  (files/ensure-files-dir)
  (let [name (:name opts)
        file (:file opts)
        fpath (files/name-path name)]
    (if (and (not (:force opts)) (fs/exists? fpath))
      (throw (util/name-existing-error name))
      (try
        (if (:edit opts)
          (edit/edit {:init-content (slurp file) :to fpath :force (:force opts)})
          (fs/copy file fpath {:replace-existing (:force opts)}))
        (catch FileAlreadyExistsException e
          (throw (util/name-existing-error name e)))))))
