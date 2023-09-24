(ns envman.cmd.misc
  (:refer-clojure :exclude [cat list remove])
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [envman.files :as files]))

(defn list [_]
  (doseq [file (fs/list-dir (files/envman-path))
          :when (not (fs/directory? file))]
    (println (fs/file-name file))))

(defn cat [{:keys [args]}]
  (doseq [name (str/split (first args) #",")]
    (print (slurp (files/envman-path name)))
    (flush)))

(defn copy [{[src dst] :args}]
  (let [names (str/split src #",")
        tmp (fs/create-temp-file {:posix-file-permissions "rw-------"})]
    (doseq [name names
            :let [path (files/envman-path name)
                  content (str/split-lines (slurp path))]]
      (fs/write-lines tmp content {:append true}))
    (fs/move tmp (files/envman-path dst))))

(defn move [{[src dst] :args}]
  (fs/move (files/envman-path src)
           (files/envman-path dst)))

(defn remove [{:keys [args]}]
  (let [names (str/split (first args) #",")]
    (doseq [name names]
      (fs/delete (files/envman-path name)))))
