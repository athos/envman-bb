(ns envman.cmd.export
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]
            [envman.util :as util])
 (:import [java.nio.file FileAlreadyExistsException]))

(def opts-spec
  [[:name {:coerce util/parse-names}]
   [:file {:desc "Specify the output file name. Defaults to `.env`."
           :default-desc "<file>"
           :coerce :string
           :alias :f}]
  [:force {:desc "Overwrite the existing .env file without error if it exists"
           :coerce :boolean
           :alias :F}]])

(defn export [{{names :name :keys [file force]} :opts}]
  (let [tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})
        file' (or file ".env")]
    (doseq [name names
            :let [path (files/existing-envman-path name)
                  content (str/split-lines (slurp (fs/file path)))]]
      (fs/write-lines tmp content {:append true}))
    (try
      (fs/move tmp file' {:replace-existing force})
      (catch FileAlreadyExistsException e
        (throw (ex-info (str "file \"" file' "\" already exists") {} e))))))
