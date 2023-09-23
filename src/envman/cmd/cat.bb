(ns envman.cmd.cat
  (:refer-clojure :exclude [cat])
  (:require [clojure.string :as str]
            [envman.files :as files]))

(defn cat [{:keys [args]}]
  (doseq [name (str/split (first args) #",")]
    (print (slurp (files/envman-path name)))
    (flush)))
