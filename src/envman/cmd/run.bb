(ns envman.cmd.run
  (:require [babashka.process :as proc]
            [envman.dotenv :as dotenv]
            [envman.files :as files]))

(defn- run-with-shell [env cmd]
  (apply proc/shell {:out *out* :err *err* :env env :continue true} cmd))

(defn run [{:keys [args]}]
  (let [fpath (files/envman-path (first args))
        env (-> {}
                (into (System/getenv))
                (into (dotenv/parse (slurp fpath))))
        {:keys [exit]} @(run-with-shell env (rest args))]
    (when (not= exit 0)
      (System/exit exit))))
