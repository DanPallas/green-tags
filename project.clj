(defproject green-tags/green-tags "0.3.0-alpha"
  :description "green-tags is an audio tagging library supporing mp3, mp4, flac, and ogg containers. It supports all of the common fields used in these files including one image in a simple abstracted way which hides most of the differences between the formats. 
               \n\n
Supported tags: \n
    mp3: track, track-total, disc-no, disc-total, title,
      artist, album, album-artist, year, genre, comment, composer, 
      original-artist, remixer, conductor, bpm, grouping, isrc, record-label, 
      encoder, lyricist, lyrics, artwork-mime, artwork-data (byte array)\n
    mp4: all from mp3 except original-artist, remixer, record-label\n
    ogg/flac: all except original-artist, track-total, record-label, disc-total,
      remixer, grouping"
  :url "https://github.com/DanPallas/green-tags"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org/jaudiotagger "2.0.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [commons-io/commons-io "2.4"]]}}
  :plugins [[codox "0.8.10"]]
  :scm {:name "git"
        :url "https://github.com/DanPallas/green-tags"}
  :signing {:gpg-key "6EFA1EC0"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :pom-addition [:developers [:developer
                              [:name "Dan Pallas"]
                              [:url "https://github.com/DanPallas"]
                              [:email "git@danpallas.com"]]])
