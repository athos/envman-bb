(ns envman.cmd.misc
  (:refer-clojure :exclude [cat list remove])
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.envset :as envset]
            [envman.files :as files]
            [envman.util :as util])
  (:import [java.nio.file FileAlreadyExistsException]))

(def list-opts-spec
  [[:pattern {:coerce :string
              :default "**"}]
   [:all {:desc "Show envsets including hidden ones"
          :coerce :boolean
          :alias :a}]])

(defn list [{:keys [opts]}]
  (doseq [pat (str/split (:pattern opts) #",")
          file (files/matching-paths pat)
          :when (not (fs/directory? file))
          :let [rel-path (str (fs/relativize (files/envman-files-dir) file))
                {:keys [meta]} (envset/parse (slurp (fs/file file)))]
          :when (or (:all opts) (not (:hidden meta)))]
    (println (str/replace rel-path fs/file-separator "/"))))

(def cat-opts-spec
  [[:name {:coerce util/parse-names}]])

(defn cat [{{names :name} :opts}]
  (doseq [name names]
    (print (slurp (fs/file (files/existing-name-path name))))
    (flush)))

(def copy-opts-spec
  [[:src {:coerce util/parse-names}]
   [:dst {:coerce util/check-name}]
   [:force {:desc "Overwrite the existing name without error if it exists"
            :coerce :boolean
            :alias :f}]])

(defn copy [{:keys [opts]}]
  (let [tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})
        dst (:dst opts)
        dst-path (files/name-path dst)]
    (doseq [name (:src opts)
            :let [path (files/existing-name-path name)
                  content (str/split-lines (slurp (fs/file path)))]]
      (fs/write-lines tmp content {:append true}))
    (try
      (files/ensure-parent-dirs dst-path)
      (fs/move tmp dst-path {:replace-existing (:force opts)})
      (catch FileAlreadyExistsException e
        (throw (util/name-existing-error dst e))))))

(def move-opts-spec
  [[:src {:coerce util/check-name}]
   [:dst {:coerce util/check-name}]
   [:force {:desc "Overwrite the existing name without error if it exists"
            :coerce :boolean
            :alias :f}]])

(defn move [{:keys [opts]}]
  (let [{:keys [src dst]} opts
        dst-path (files/name-path dst)]
    (try
      (files/ensure-parent-dirs dst-path)
      (fs/move (files/existing-name-path src) dst-path
               {:replace-existing (:force opts)})
      (catch FileAlreadyExistsException e
        (throw (util/name-existing-error dst e))))))

(def remove-opts-spec
  [[:name {:coerce util/parse-names}]
   [:force {:desc "Do not report an error even if the specified name does not exist"
            :coerce :boolean
            :alias :f}]])

(defn remove [{:keys [opts]}]
  (let [path-fn (if (:force opts) files/name-path files/existing-name-path)]
    (doseq [name (:name opts)]
      (fs/delete-if-exists (path-fn name)))))
