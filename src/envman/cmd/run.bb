(ns envman.cmd.run
  (:require [babashka.process :as proc]
            [envman.dotenv :as dotenv]
            [envman.files :as files]))

(def opts-spec
  [[:isolated {:desc "Execute the command with only the specified environment variables"
               :coerce :boolean
               :alias :i}]])

(defn- run-with-shell [env cmd]
  (apply proc/shell {:out *out* :err *err* :env env :continue true} cmd))

(defn run [{:keys [opts args]}]
  (let [fpath (files/envman-path (first args))
        init-env (into {} (System/getenv))
        env (cond->> (into {} (dotenv/parse (slurp fpath) {:vars init-env}))
              (not (:isolated opts))
              (merge init-env))
        {:keys [exit]} @(run-with-shell env (rest args))]
    (when (not= exit 0)
      (System/exit exit))))
