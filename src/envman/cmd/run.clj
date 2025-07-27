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

(defn- load-envset [env {:keys [meta body]}]
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
              out-vars (dotenv/load (slurp (fs/file env-file)))
              env' (-> env (into shell-vars) (into out-vars))]
          (into out-vars (dotenv/expand-vars env' body)))
        (finally
          (fs/delete-if-exists env-file)
          (fs/delete-if-exists script-path))))
    (dotenv/expand-vars env body)))

(defn- parse-file [f]
  (envset/parse (slurp (fs/file f))))

(defn- resolve-deps [paths envsets]
  (letfn [(step [path {:keys [meta body]}]
            (-> []
                (into (mapcat #(step % (parse-file (files/name-path %))))
                      (:include meta))
                (conj {:path path, :meta meta, :body body})))]
    (loop [[{:keys [path] :as e} & more] (mapcat step paths envsets)
           seen #{}
           ret []]
      (if e
        (if (seen path)
          (recur more seen ret)
          (recur more (conj seen path) (conj ret e)))
        ret))))

(defn- update-env [init-env paths vars]
  (let [parsed-envsets (->> paths
                            (map #(parse-file (files/name-path %)))
                            (resolve-deps paths))]
    (as-> {:env init-env :updated []} acc
      (reduce (fn [acc parsed-envset]
                (let [vars (load-envset (:env acc) parsed-envset)]
                  (-> acc
                      (update :env into vars)
                      (update :updated into (map first vars)))))
              acc
              parsed-envsets)
      (reduce (fn [acc var]
                (let [[k v] (dotenv/parse1 var {:vars (:env acc) :allow-export? false})]
                  (-> acc
                      (assoc-in [:env k] v)
                      (update :updated conj k))))
              acc
              vars))))

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
        env' (into {} (load-envset env (dotenv/parse tmp)))]
    (fs/delete tmp)
    env'))

(defn- run-with-shell [env cmd]
  (apply proc/shell {:inherit true :env env :continue true} cmd))

(defn run [{:keys [opts args]}]
  (let [init-env (into {} (System/getenv))
        {:keys [env updated]} (update-env init-env (:name opts) (:env opts))
        env (cond->> (select-keys env updated)
              (:edit opts)
              (edit-and-load updated)

              (not (:isolated opts))
              (merge init-env))
        {:keys [exit]} @(run-with-shell env args)]
    (when (not= exit 0)
      (System/exit exit))))
