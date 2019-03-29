(defproject locksmith "0.1.2-SNAPSHOT"
  :description "Change all your keys between idiomatic clojure and GraphQL"
  :url "https://github.com/oliyh/locksmith"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :dependencies [[camel-snake-kebab "0.4.0"]]
  :plugins [[lein-doo "0.1.8"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.9.0-beta2"]
                                       [org.clojure/clojurescript "1.9.946"]]}
             :dev {:source-paths ["dev"]
                   :exclusions [[org.clojure/tools.reader]]
                   :dependencies [[org.clojure/tools.reader "1.1.0"]
                                  [lein-doo "0.1.6"]]
                   :repl-options {:init-ns user}}})
