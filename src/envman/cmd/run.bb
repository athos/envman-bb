(ns envman.cmd.run
  (:require [babashka.process :as proc]
            [clojure.string :as str]
            [envman.dotenv :as dotenv]
            [envman.files :as files]))

(def opts-spec
  [[:env {:desc "Set variable <name> to <value>"
          :coerce [:string]
          :alias :e}]
   [:isolated {:desc "Execute the command with only the specified environment variables"
               :coerce :boolean
               :alias :i}]])

(defn- run-with-shell [env cmd]
  (apply proc/shell {:out *out* :err *err* :env env :continue true} cmd))

(defn- load-dotenv [env in]
  (dotenv/parse (slurp in) {:vars env}))

(defn- update-env [init-env paths vars]
  (as-> {:env init-env :updated []} acc
    (reduce (fn [acc path]
              (let [vars (load-dotenv (:env acc) path)]
                (-> acc
                    (update :env into vars)
                    (update :updated into (map first vars)))))
            acc
            paths)
    (reduce (fn [acc var]
              (let [[k v] (dotenv/parse1 var {:vars (:env acc) :allow-export? false})]
                (-> acc
                    (assoc-in [:env k] v)
                    (update :updated conj k))))
            acc
            vars)))

(defn run [{:keys [opts args]}]
  (let [names (str/split (first args) #",")
        paths (map files/envman-path names)
        init-env (into {} (System/getenv))
        {:keys [env updated]} (update-env init-env paths (:env opts))
        env (cond->> (select-keys env updated)
              (not (:isolated opts))
              (merge init-env))
        {:keys [exit]} @(run-with-shell env (rest args))]
    (when (not= exit 0)
      (System/exit exit))))
