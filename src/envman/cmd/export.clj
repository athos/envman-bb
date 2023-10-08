(ns envman.cmd.export
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]
            [envman.util :as util]))

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
  (let [tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})]
    (doseq [name names
            :let [path (files/existing-envman-path name)
                  content (str/split-lines (slurp path))]]
      (fs/write-lines tmp content {:append true}))
    (fs/move tmp (or file ".env") {:replace-existing force})))
