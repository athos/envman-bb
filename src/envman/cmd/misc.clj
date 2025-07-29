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
          :alias :a}]
   [:long {:desc "Show detailed information of each envset"
           :coerce :boolean
           :alias :l}]])

(defn- pad-right [n s]
  (apply str s (repeat (- n (count s)) \space)))

(defn- print-envset-details [es]
  (let [max-name (apply max (map (comp count :name) es))
        max-tags (->> es
                      (map (fn [{{:keys [tags]} :meta}]
                             (apply + (dec (count tags)) (map count tags))))
                      (apply max))]
    (run! (fn [{:keys [name meta]}]
            (printf "%s    %s    %s\n"
                    (pad-right max-name name)
                    (pad-right max-tags (str/join \, (:tags meta)))
                    (or (get meta :description) "")))
          es)))

(defn list [{:keys [opts]}]
  (let [es (for [pat (str/split (:pattern opts) #",")
                 file (files/matching-paths pat)
                 :when (not (fs/directory? file))
                 :let [rel-path (str (fs/relativize (files/envman-files-dir) file))
                       name (str/replace rel-path fs/file-separator "/")
                       {:keys [meta]} (envset/parse (slurp (fs/file file)))]
                 :when (or (:all opts) (not (:hidden meta)))]
             {:name name :meta meta})]
    (when (seq es)
      (if (:long opts)
        (print-envset-details es)
        (run! (comp println :name) es)))))

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
