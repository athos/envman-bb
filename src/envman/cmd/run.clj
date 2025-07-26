(ns envman.cmd.run
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.string :as str]
            [envman.dotenv :as dotenv]
            [envman.edit :as edit]
            [envman.envset :as envset]
            [envman.files :as files]
            [envman.util :as util]))

(def opts-spec
  [[:name {:coerce util/parse-names}]
   [:env {:desc "Set variable <name> to <value>"
          :default-desc "<name>=<value>"
          :coerce [:string]
          :alias :e}]
   [:isolated {:desc "Execute the command with only the specified environment variables"
               :coerce :boolean
               :alias :i}]
   [:edit {:desc "Launch an editor to edit variables on the fly before executing the command"
           :coerce :boolean
           :alias :E}]])

(defn- load-dotenv [env in]
  (let [{:keys [meta body]} (envset/parse (slurp (fs/file in)))]
    (if-let [script (:setup meta)]
      (let [script-path (fs/create-temp-file {:suffix ".sh" :posix-file-permissions "rw-------"})
            script-file (fs/file script-path)
            env-file (fs/create-temp-file {:suffix ".env" :posix-file-permission "rw-------"})
            env' (assoc env "ENVMAN_ENV" (str env-file))
            boundary (str "---" (random-uuid) "---")]
        (spit script-file script)
        (spit script-file (str "echo\necho \"" boundary "\"\n") :append true)
        (spit script-file "set\n" :append true)
        (try
          (let [{:keys [out]} @(proc/shell {:env env', :in script-file,
                                            :out :string, :err :inherit}
                                           "/bin/bash -s")
                shell-vars (->> (str/split-lines out)
                                (drop-while (partial not= boundary))
                                rest
                                (into {}
                                      (map (fn [line]
                                             (let [[_ k v] (re-matches #"([^=]+)=(.*)" line)]
                                               [k v])))))
                out-vars (dotenv/parse (slurp (fs/file env-file)))
                env' (-> env (into shell-vars) (into out-vars))]
            (-> shell-vars
                (into out-vars)
                (into (dotenv/expand-vars env' body))))
          (finally
            (fs/delete-if-exists env-file)
            (fs/delete-if-exists script-path))))
      (dotenv/expand-vars env body))))

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

(defn- quote-content [s]
  (str \"
       (str/replace s #"(?<!\\)[\\\"]" (fn [c] (str \\ c)))
       \"))

(defn- print-env [vars env]
  (doseq [k vars
          :let [v (or (get env k) "")
                v' (cond-> v
                     (not (re-matches #"[a-zA-Z0-9_]*" v))
                     quote-content)]]
    (printf "%s=%s\n" k v'))
  (flush))

(defn- edit-and-load [edit-vars env]
  (let [content (with-out-str
                  (print-env edit-vars env))
        tmp (edit/edit :init-content content)
        env' (into {} (load-dotenv env tmp))]
    (fs/delete tmp)
    env'))

(defn- run-with-shell [env cmd]
  (apply proc/shell {:inherit true :env env :continue true} cmd))

(defn run [{:keys [opts args]}]
  (let [paths (map files/name-path (:name opts))
        init-env (into {} (System/getenv))
        {:keys [env updated]} (update-env init-env paths (:env opts))
        env (cond->> (select-keys env updated)
              (:edit opts)
              (edit-and-load updated)

              (not (:isolated opts))
              (merge init-env))
        {:keys [exit]} @(run-with-shell env args)]
    (when (not= exit 0)
      (System/exit exit))))
