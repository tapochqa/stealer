(defproject stealer "0.6-SNAPSHOT"

  :description
  "Telegram Bot"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   
   [link.lmnd/tg-bot-api "0.1.1"]
   [com.github.igrishaev/dynamodb "0.1.3"]
   
   [http-kit            "2.6.0"]
   [cheshire            "5.10.0"]
   
   [seancorfield/next.jdbc "1.0.409"]
   [mysql/mysql-connector-java "8.0.31"]]

  :main ^:skip-aot stealer.core

  :target-path "target/uberjar"

  :uberjar-name "stealer.jar"
  
  :jvm-opts ["-Dfile.encoding=UTF-8"]
  
  :plugins [[lein-project-version "0.1.0"]
            [lein-bump-version "0.1.6"]]

  :profiles
  {:dev
   {:global-vars
    {*warn-on-reflection* true
     *assert* true}}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
