(defproject green-tags/green-tags "0.3.0-alpha"
  :description "green-tags is an audio tagging library supporing mp3, mp4, flac, and ogg containers. It supports all of the common fields used in these files including one image in a simple abstracted way which hides most of the differences between the formats. 

ex. To get read all supported tags and image into a map use (get-all-info <path>) and a map will be returned with all tag data.

ex. To update an existing tag with a new artist and album while leaving all other fields untouched, use (update-tag! <file> {:artist \"new artist\"
                                           :album \"new album\"})

ex. To delete the lyrics field, use (update-tag! <file> {:lyrics :delete})

Supported tags: 
    mp3: track, track-total, disc-no, disc-total, title,
      artist, album, album-artist, year, genre, comment, composer, 
      original-artist, remixer, conductor, bpm, grouping, isrc, record-label, 
      encoder, lyricist, lyrics, artwork-mime, artwork-data (byte array)
    mp4: all from mp3 except original-artist, remixer, record-label
    ogg/flac: all except original-artist, track-total, record-label, disc-total,
      remixer, grouping

API Documentation: http://danpallas.github.io/green-tags/
               
contributing: Pull requests are welcome, but try to keep the API simple and submit updated tests to go along with it. Don't use macros in the API because they complicate test mocking for any project that implements this library."
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
