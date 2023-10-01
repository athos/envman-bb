(ns envman.main
  (:require [babashka.cli :as cli]
            [envman.cmd.create :as create]
            [envman.cmd.edit :as edit]
            [envman.cmd.export :as export]
            [envman.cmd.import :as import]
            [envman.cmd.misc :as misc]
            [envman.cmd.run :as run]))

(declare usage)

(defn error-fn-for [cmd]
  (fn [{:keys [type cause msg option] :as data}]
    (if (= :org.babashka/cli type)
      (let [errmsg (case cause
                     :restrict (str "error: unknown option " option)
                     (str "error: " msg))]
        (binding [*out* *err*]
          (println errmsg)
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
     [{:cmds ["create"] :fn create/create :spec create/opts-spec :args->opts [:name]
       :usage "create NAME"
       :desc "Create new environment variable set"}
      {:cmds ["run"] :fn run/run :spec run/opts-spec
       :usage "run NAME[,NAME...] [OPTION...] -- COMMAND [ARG...]"
       :desc "Run command with specified sets of environment variables"}
      {:cmds ["ls"] :fn misc/list
       :usage "ls"
       :desc "List environment variable sets"}
      {:cmds ["cat"] :fn misc/cat :spec misc/cat-opts-spec :args->opts [:name]
       :usage "cat NAME[,NAME...]"
       :desc "Show environment variable sets"}
      {:cmds ["edit"] :fn edit/edit :spec edit/opts-spec :args->opts [:name]
       :usage "edit NAME"
       :desc "Edit environment variable set"}
      {:cmds ["cp"] :fn misc/copy :spec misc/copy-opts-spec :args->opts [:src :dst]
       :usage "cp NAME1[,NAME...] NAME2"
       :desc "Copy environment variable set"}
      {:cmds ["mv"] :fn misc/move :spec misc/move-opts-spec :args->opts [:src :dst]
       :usage "mv NAME1 NAME2"
       :desc "Rename environment variable set"}
      {:cmds ["rm"] :fn misc/remove :spec misc/remove-opts-spec :args->opts [:name]
       :usage "rm NAME[,NAME...]"
       :desc "Remove environment variable sets"}
      {:cmds ["import"] :fn import/import :spec import/opts-spec :args->opts [:name]
       :usage "import NAME"
       :desc "Import .env file as environment variable set"}
      {:cmds ["export"] :fn export/export :spec export/opts-spec :args->opts [:name]
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
          cmd (first (filter #(= (:cmds %) target) @table))
          hidden-opts (set (:args->opts cmd))
          spec (remove #(hidden-opts (first %)) (:spec cmd))]
      (printf "Usage: envman %s\n\n" (:usage cmd))
      (println (:desc cmd))
      (when (seq spec)
        (println "\nOptions:")
        (println (cli/format-opts {:spec spec}))))))

(defn -main [& args]
  (cli/dispatch @table args {:restrict true}))
