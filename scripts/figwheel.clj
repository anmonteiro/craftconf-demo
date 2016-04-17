(require '[figwheel-sidecar.repl :as r]
         '[figwheel-sidecar.repl-api :as ra])

(ra/start-figwheel!
  {:build-ids ["dev"]
   :all-builds
   [{:id "dev"
     :figwheel true ;{:devcards true}
     :source-paths ["src/cljs"]
     :compiler {:main 'craftconf-demo.core
                :asset-path "/out"
                :output-to "resources/public/main.js"
                :output-dir "resources/public/out"
                :foreign-libs [{:provides ["cljsjs.codemirror.addons.closebrackets"]
                                :requires ["cljsjs.codemirror"]
                                :file     "resources/public/codemirror/closebrackets.js"}
                               {:provides ["cljsjs.codemirror.addons.matchbrackets"]
                                :requires ["cljsjs.codemirror"]
                                :file     "resources/public/codemirror/matchbrackets.js"}]
                :parallel-build true
                :compiler-stats true}}]})

(ra/cljs-repl)
