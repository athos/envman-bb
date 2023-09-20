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

(def table
  [{:cmds ["create"] :fn create/create}
   {:cmds ["run"] :fn run/run}
   {:cmds ["ls"] :fn list/list}
   {:cmds ["cat"] :fn cat/cat}
   {:cmds ["edit"] :fn edit/edit}
   {:cmds ["cp"] :fn misc/copy}
   {:cmds ["mv"] :fn misc/move}
   {:cmds ["rm"] :fn misc/remove}
   {:cmds ["import"] :fn import/import}
   {:cmds ["export"] :fn export/export}])

(defn -main [& args]
  (cli/dispatch table args))
