(defproject craftconf-demo "0.1.0-SNAPSHOT"
  :description "FIXME"
  :url "https://github.com/anmonteiro/craftconf-demo"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [org.omcljs/om "1.0.0-alpha32"]
                 [cljsjs/codemirror "5.8.0-0"]
                 [devcards "0.2.1-6"]
                 [figwheel-sidecar "0.5.2"]
                 [org.clojure/test.check "0.9.0"]

                 [bidi "2.0.6"]
                 [ring "1.4.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.datomic/datomic-free "0.9.5350"]]
  ;; (setq cider-lein-parameters "with-profile +client,+server repl :headless")
  :source-paths ["src/clj" "src/cljs" "src/test-cljs"]
  :clean-targets ^{:protect false} ["target"
                                    "resources/public/main.js"])
