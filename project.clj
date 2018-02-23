(defproject versioning "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [reagent  "0.7.0"]
                 [re-frame "0.10.1"]]
  :plugins [[lein-figwheel "0.5.13"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src-cljs"]
                :figwheel {:on-jsload "versioning.core/on-js-reload"
                           #_:open-urls #_["http://localhost:3449/index.html"]}
                :compiler {:main versioning.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/core.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}]}
  :figwheel {:css-dirs ["resources/public/css"]}
  :profiles {:dev {:plugins [[lein-cljfmt "0.5.7"]
                             [venantius/yagni "0.1.4"]
                             [lein-cljfmt "0.5.7"]
                             [jonase/eastwood "0.2.5"]]
                   :dependencies [[com.rpl/specter "1.1.0"]
                                  [alembic "0.3.2"]
                                  [binaryage/devtools "0.9.4"]
                                  [figwheel-sidecar "0.5.13"]
                                  [org.clojure/clojurescript "1.9.908"]
                                  [org.clojure/core.async  "0.3.443"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [devcards "0.2.4" :exclusions [cljsjs/react]]]
                   :source-paths ["src-cljs"]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
