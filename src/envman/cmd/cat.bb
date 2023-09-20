(ns envman.cmd.cat
  (:refer-clojure :exclude [cat])
  (:require [envman.files :as files]))

(defn cat [{:keys [args]}]
  (print (slurp (files/envman-path (first args))))
  (flush))
