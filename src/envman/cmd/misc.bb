(ns envman.cmd.misc
  (:refer-clojure :exclude [cat list remove])
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]
            [envman.util :as util]))

(defn list [_]
  (doseq [file (fs/list-dir (files/envman-path))
          :when (not (fs/directory? file))]
    (println (fs/file-name file))))

(def cat-opts-spec
  [[:name {:coerce util/parse-names}]])

(defn cat [{{names :name} :opts}]
  (doseq [name names]
    (print (slurp (files/envman-path name)))
    (flush)))

(def copy-opts-spec
  [[:src {:coerce util/parse-names}]
   [:dst {:coerce util/check-name}]])

(defn copy [{{names :name :keys [dst]} :opts}]
  (let [tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})]
    (doseq [name names
            :let [path (files/envman-path name)
                  content (str/split-lines (slurp path))]]
      (fs/write-lines tmp content {:append true}))
    (fs/move tmp (files/envman-path dst))))

(def move-opts-spec
  [[:src {:coerce util/check-name}]
   [:dsc {:coerce util/check-name}]])

(defn move [{{:keys [src dst]} :opts}]
  (fs/move (files/envman-path src)
           (files/envman-path dst)))

(def remove-opts-spec
  [[:name {:coerce util/parse-names}]
   [:force {:desc "Do not report an error even if the specified name does not exist"
            :coerce :boolean
            :alias :f}]])

(defn remove [{:keys [opts]}]
  (let [delete-fn (if (:force opts) fs/delete-if-exists fs/delete)]
    (doseq [name (:name opts)]
      (delete-fn (files/envman-path name)))))
