(ns envman.cmd.vcs
  (:require [babashka.process :as proc]
            [envman.files :as files]))

(defn vcs [{:keys [args]}]
  (let [{:keys [exit]} (apply proc/shell {:dir (files/envman-files-dir)} "git" args)]
    (when (not= exit 0)
      (System/exit exit))))
