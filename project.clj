(defproject green-tags "0.1.0-alpha"
  :description "An audio tagging library supporing mp3 mp4 flac and ogg. It is intended to be useable with the with little boiler plate code."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org/jaudiotagger "2.0.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :plugins [[codox "0.8.10"]])
