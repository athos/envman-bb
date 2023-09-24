(ns envman.main
  (:require [babashka.cli :as cli]
            [envman.cmd.create :as create]
            [envman.cmd.edit :as edit]
            [envman.cmd.export :as export]
            [envman.cmd.import :as import]
            [envman.cmd.list :as list]
            [envman.cmd.misc :as misc]
            [envman.cmd.run :as run]))

(declare usage)

(defn error-fn-for [cmd]
  (fn [{:keys [type cause msg option] :as data}]
    (if (= :org.babashka/cli type)
      (case cause
        :restrict
        (binding [*out* *err*]
          (println "error: unknown option" option)
          (usage (assoc data :cmds (:cmds cmd)))))
      (throw (ex-info msg data)))
    (System/exit 1)))

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
    (->>
     [{:cmds ["create"] :fn create/create
       :usage "create NAME"
       :desc "Create new environment variable set"}
      {:cmds ["run"] :fn run/run :spec run/opts-spec
       :usage "run NAME[,NAME...] [OPTION...] -- COMMAND [ARG...]"
       :desc "Run command with specified sets of environment variables"}
      {:cmds ["ls"] :fn list/list
       :usage "ls"
       :desc "List environment variable sets"}
      {:cmds ["cat"] :fn misc/cat
       :usage "cat NAME[,NAME...]"
       :desc "Show environment variable sets"}
      {:cmds ["edit"] :fn edit/edit
       :usage "edit NAME"
       :desc "Edit environment variable set"}
      {:cmds ["cp"] :fn misc/copy
       :usage "cp NAME1[,NAME...] NAME2"
       :desc "Copy environment variable set"}
      {:cmds ["mv"] :fn misc/move
       :usage "mv NAME1 NAME2"
       :desc "Rename environment variable set"}
      {:cmds ["rm"] :fn misc/remove
       :usage "rm NAME[,NAME...]"
       :desc "Remove environment variable sets"}
      {:cmds ["import"] :fn import/import
       :usage "import NAME"
       :desc "Import .env file as environment variable set"}
      {:cmds ["export"] :fn export/export
       :usage "export NAME[,NAME...]"
       :desc "Export environment variable sets as .env file"}
      {:cmds ["help"] :fn usage
       :desc "Print this message"}
      {:cmds [] :fn fallback}]
     (mapv #(assoc % :error-fn (error-fn-for %))))))

(defn- pad-right [n s]
  (let [len (count s)]
    (if (< len n)
      (apply str s (repeat (- n len) \space))
      s)))

(defn- usage [{:keys [dispatch cmds args]}]
  (if  (or (and (= dispatch ["help"]) (empty? args))
           (and (empty? cmds) (empty? dispatch)))
    (let [max-len (apply max (map (comp count first :cmds) @table))]
      (println "Usage:  envman COMMAND

Commands:")
      (doseq [cmd @table
              :when (seq (:cmds cmd))]
        (printf "  %s   %s\n"
                (pad-right max-len (first (:cmds cmd)))
                (or (:desc cmd) "")))
      (flush))
    (let [target (if (= dispatch ["help"]) (take 1 args) cmds)
          cmd (first (filter #(= (:cmds %) target) @table))]
      (printf "Usage: envman %s\n\n" (:usage cmd))
      (println (:desc cmd))
      (when-let [spec (:spec cmd)]
        (println "\nOptions:")
        (println (cli/format-opts {:spec spec}))))))

(defn -main [& args]
  (cli/dispatch @table args {:restrict true}))
