(ns envman.cmd.misc
  (:refer-clojure :exclude [cat list remove])
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]
            [envman.util :as util])
  (:import [java.nio.file FileAlreadyExistsException]))

(defn list [_]
  (doseq [file (fs/list-dir (files/envman-path))
          :when (not (fs/directory? file))]
    (println (fs/file-name file))))

(def cat-opts-spec
  [[:name {:coerce util/parse-names}]])

(defn cat [{{names :name} :opts}]
  (doseq [name names]
    (print (slurp (files/existing-envman-path name)))
    (flush)))

(def copy-opts-spec
  [[:src {:coerce util/parse-names}]
   [:dst {:coerce util/check-name}]
   [:force {:desc "Overwrite the existing name without error if it exists"
            :coerce :boolean
            :alias :f}]])

(defn copy [{:keys [opts]}]
  (let [tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})]
    (doseq [name (:src opts)
            :let [path (files/existing-envman-path name)
                  content (str/split-lines (slurp path))]]
      (fs/write-lines tmp content {:append true}))
    (try
      (fs/move tmp (files/envman-path (:dst opts))
               {:replace-existing (:force opts)})
      (catch FileAlreadyExistsException e
        (throw (ex-info (str "name \"" (:dst opts) "\" already exists") {} e))))))

(def move-opts-spec
  [[:src {:coerce util/check-name}]
   [:dst {:coerce util/check-name}]
   [:force {:desc "Overwrite the existing name without error if it exists"
            :coerce :boolean
            :alias :f}]])

(defn move [{:keys [opts]}]
  (let [{:keys [src dst]} opts]
    (try
      (fs/move (files/existing-envman-path src) (files/envman-path dst)
               {:replace-existing (:force opts)})
      (catch FileAlreadyExistsException e
        (throw (ex-info (str "name \"" dst "\" already exists") {} e))))))

(def remove-opts-spec
  [[:name {:coerce util/parse-names}]
   [:force {:desc "Do not report an error even if the specified name does not exist"
            :coerce :boolean
            :alias :f}]])

(defn remove [{:keys [opts]}]
  (let [path-fn (if (:force opts) files/envman-path files/existing-envman-path)]
    (doseq [name (:name opts)]
      (fs/delete-if-exists (path-fn name)))))
