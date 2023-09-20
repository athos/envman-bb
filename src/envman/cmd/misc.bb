(ns envman.cmd.misc
  (:refer-clojure :exclude [remove])
  (:require [babashka.fs :as fs]
            [envman.files :as files]))

(defn copy [{[src dst] :args}]
  (fs/copy (files/envman-path src)
           (files/envman-path dst)))

(defn move [{[src dst] :args}]
  (fs/move (files/envman-path src)
           (files/envman-path dst)))

(defn remove [{:keys [args]}]
  (fs/delete (files/envman-path (first args))))
