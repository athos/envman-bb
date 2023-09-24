(ns envman.main
  (:require [babashka.cli :as cli]
            [envman.cmd.cat :as cat]
            [envman.cmd.create :as create]
            [envman.cmd.edit :as edit]
            [envman.cmd.export :as export]
            [envman.cmd.import :as import]
            [envman.cmd.list :as list]
            [envman.cmd.misc :as misc]
            [envman.cmd.run :as run]))

(declare usage)

(defn- fallback [{:keys [cmds] :as m}]
  (if-let [[cmd] (seq cmds)]
    (binding [*out* *err*]
      (printf "envman: '%s' is not an envman command.\n" cmd)
      (println "See 'envman help'."))
    (binding [*out* *err*]
      (usage m)))
  (System/exit 1))

(def table
  (delay
    [{:cmds ["create"] :fn create/create
      :desc "Create new environment variable set"}
     {:cmds ["run"] :fn run/run :spec run/opts-spec
      :desc "Run command with specified sets of environment variables"}
     {:cmds ["ls"] :fn list/list
      :desc "List environment variable sets"}
     {:cmds ["cat"] :fn cat/cat
      :desc "Show environment variable sets"}
     {:cmds ["edit"] :fn edit/edit
      :desc "Edit environment variable set"}
     {:cmds ["cp"] :fn misc/copy
      :desc "Copy environment variable set"}
     {:cmds ["mv"] :fn misc/move
      :desc "Rename environment variable set"}
     {:cmds ["rm"] :fn misc/remove
      :desc "Remove environment variable sets"}
     {:cmds ["import"] :fn import/import
      :desc "Import .env file as environment variable set"}
     {:cmds ["export"] :fn export/export
      :desc "Export environment variable sets as .env file"}
     {:cmds ["help"] :fn usage
      :desc "Print this message"}
     {:cmds [] :fn fallback}]))

(defn- pad-right [n s]
  (let [len (count s)]
    (if (< len n)
      (apply str s (repeat (- n len) \space))
      s)))

(defn- usage [_]
  (println
   "Usage:  envman COMMAND

Commands:")
  (let [max-len (apply max (map (comp count first :cmds) @table))]
    (doseq [cmd @table
            :when (seq (:cmds cmd))]
      (printf "  %s   %s\n"
              (pad-right max-len (first (:cmds cmd)))
              (or (:desc cmd) "")))
    (flush)))

(defn -main [& args]
  (letfn [(error-fn [{:keys [type msg] :as data}]
            (if (= :org.babashka/cli type)
              (prn :data data)
              (throw (ex-info msg data)))
            (System/exit 1))]
    (cli/dispatch @table args {:error-fn error-fn})))
