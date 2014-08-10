(ns green-tags.core-test
  (:require [clojure.java.io :refer [as-file copy delete-file input-stream]]
            [clojure.set :as sets]
            [clojure.data :as data]
            [green-tags.core :as core]
            [midje.sweet :refer :all])
  (:import [org.apache.commons.io IOUtils]))

(defmacro debug [label i]
  `(let [o# ~i
         _# (println (str "Debug " ~label ": " o#))]
     o#))

(defn get-byte-array
  [path]
  (IOUtils/toByteArray (input-stream path)))

(def fields-not-in-aac [:original-artist :remixer :record-label])
(def fields-dif-in-aac [:genre :encoder])
(def fields-not-in-vorbis [:original-artist :track-total :record-label 
                           :disc-total :remixer :grouping])
(def fields-dif-in-vorbis [:track :encoder])
(def song3 (let
             [fields {:track "1"
                     :track-total "10"
                     :disc-no "1"
                     :disc-total "2"
                     :title "song3"
                     :artist "artist"
                     :album "album"
                     :album-artist "album-artist"
                     :year "2014"
                     :genre "Synthpop"
                     :comment "these are comments"
                     :composer "composer"
                     :original-artist "origart"
                     :remixer "remixer"
                     :conductor "conducted"
                     :bpm "220"
                     :grouping "art"
                     :isrc "isrc"
                     :record-label "published"
                     :encoder "someone"
                     :lyricist "writer"
                     :lyrics "the lyrics"
                     :artwork "image/png"
                     }]
             (-> (assoc {} :id3-fields fields)
                 (assoc :id3-header {:bit-rate 128 :channels "Mono"
                                     :encoding-type "mp3"
                                     :format "MPEG-1 Layer 3" 
                                     :sample-rate 44100 :length 0
                                     :variable-bit-rate false})
                 (assoc :aac-fields (-> (reduce dissoc fields fields-not-in-aac)
                                        (assoc :genre "SynthPop"
                                               :encoder 
                                               "Nero AAC codec / 1.5.4.0")))
                 (assoc :aac-header {:bit-rate 2 :channels "1"
                                     :encoding-type "AAC"
                                     :format "AAC" 
                                     :sample-rate 44100 :length 0
                                     :variable-bit-rate true})
                 (assoc :flac-fields 
                        (-> (reduce dissoc 
                                    fields 
                                    fields-not-in-vorbis)
                            (assoc :track "01"
                                   :encoder "reference libFLAC 1.2.1 20070917")
                            ))
                 (assoc :ogg-fields 
                        (-> (reduce dissoc 
                                    fields 
                                    fields-not-in-vorbis)
                            (assoc :track "01"
                                   :encoder "Xiph.Org libVorbis I 20120203 (Omnipresent)")))
                 (assoc :flac-header {:bit-rate 0 :channels "1"
                                        :encoding-type "FLAC 16 bits"
                                        :format "FLAC 16 bits" 
                                        :sample-rate 44100 :length 0
                                        :variable-bit-rate true})
                 (assoc :ogg-header {:bit-rate 96 :channels "1"
                                        :encoding-type "Ogg Vorbis v1"
                                        :format "Ogg Vorbis v1" 
                                        :sample-rate 44100 :length 0
                                        :variable-bit-rate true})
                 (assoc :paths {:mp3 "test/resources/tagged/song3.mp3"
                                :m4a "test/resources/tagged/song3.m4a"
                                :ogg "test/resources/tagged/song3.ogg"
                                :flac "test/resources/tagged/song3.flac"}))))

(facts 
  "about get-all-info"
  (fact "it can read information from java files"
        (core/get-all-info (as-file (get-in song3 [:paths :mp3])))
           => (conj (song3 :id3-fields) (song3 :id3-header))
        (core/get-all-info (as-file (get-in song3 [:paths :flac])))
           => (conj (song3 :flac-fields) (song3 :flac-header))
        (core/get-all-info (as-file (get-in song3 [:paths :ogg])))
           => (conj (song3 :ogg-fields) (song3 :ogg-header))
        (core/get-all-info (as-file (get-in song3 [:paths :m4a])))
           => (conj (song3 :aac-fields) (song3 :aac-header)))
  (fact "it can read information when passed a path as a string"
        (core/get-all-info (get-in song3 [:paths :m4a]))
           => (conj (song3 :aac-fields) (song3 :aac-header)))
  (fact "it returns nil when the file doesn't exist"
        (core/get-all-info "ageaewafa")
          => nil))
(facts
  "about get-fields"
  (fact "it can read information from java files"
        (core/get-fields (as-file (get-in song3 [:paths :mp3])))
          => (song3 :id3-fields)
        (core/get-fields (as-file (get-in song3 [:paths :flac])))
          => (song3 :flac-fields)
        (core/get-fields (as-file (get-in song3 [:paths :ogg])))
          => (song3 :ogg-fields)
        (core/get-fields (as-file (get-in song3 [:paths :m4a])))
          => (song3 :aac-fields))
  (fact "it can read information when passed a path as a string"
        (core/get-fields (get-in song3 [:paths :m4a]))
          => (song3 :aac-fields))
  (fact "it returns nil when the file doesn't exist"
        (core/get-fields "ageaewafa")
          => nil))
(facts
  "about get-header-info"
  (fact "it can read information from java files"
        (core/get-header-info (as-file (get-in song3 [:paths :mp3])))
          => (song3 :id3-header)
        (core/get-header-info (as-file (get-in song3 [:paths :flac])))
          => (song3 :flac-header)
        (core/get-header-info (as-file (get-in song3 [:paths :ogg])))
          => (song3 :ogg-header)
        (core/get-header-info (as-file (get-in song3 [:paths :m4a])))
          => (song3 :aac-header))
  (fact "it can read information when passed a path as a string"
        (core/get-header-info (get-in song3 [:paths :m4a]))
          => (song3 :aac-header))
  (fact "it returns :no-file when the file doesn't exist"
        (core/get-header-info "ageaewafa")
          => nil))

(def test-files {:tags {:a {:artist "artist" :title "song3"}}
                  :paths {:1 "test/resources/tagged/unorganized.mp3"
                          :2 "test/resources/tagged/unorganized2.mp3"
                          :3 "test/resources/tagged/song3.flac"
                          :4 "test/resources/tagged/song3.m4a"
                          :5 "test/resources/tagged/song3.ogg"
                          :6 "test/resources/tagged/song3.mp3"
                          :7  "test/resources/tagged/song3-no-art.mp3"}
                  :images {:1 "test/resources/images/music_icon.png"}})

(defn- clear-scratch
  []
  (doall (map delete-file 
              (vec (.listFiles (as-file "test/resources/scratch"))))))
(defn- get-scratch-path
  [path]
  (str "test/resources/scratch/" 
       (.getName (as-file (get-in test-files [:paths path])))))
(defn- copy-to-scratch
[path]
  (let [f (as-file (get-in test-files [:paths path]))]
   (copy f (as-file (get-scratch-path path)))))

(facts 
  "about add-new-tag!"
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :1))]
    (fact "it overwrites old tag, clearing all fields and returns true (mp3)"
          (core/add-new-tag! (get-scratch-path :1)
                             (get-in test-files [:tags :a]))
            => true
          (core/get-fields (get-scratch-path :1))
            => (get-in test-files [:tags :a]))
    (fact "it ignores unsupported fields and writes the supported fields"
          (core/add-new-tag! (get-scratch-path :1)
                             (assoc (get-in test-files [:tags :a]) :bad-field 1))
            => true
          (core/get-fields (get-scratch-path :1))
            => (get-in test-files [:tags :a]) )
    (fact "it overwrites and copies in ALL fields (mp3)"
          (core/add-new-tag! (get-scratch-path :1)
                             (dissoc (song3 :id3-fields) :artwork))
            => true
          (core/get-fields (get-scratch-path :1))
            => (dissoc (song3 :id3-fields) :artwork))
    (fact "it returns an error string if file doesn't exist"
          (core/add-new-tag! "bad/path" {:title ""})
            => (contains ""))
    (fact "it updates the genre tag correctly on mp3 files"
          (core/add-new-tag! (get-scratch-path :1) {:genre "Rock"})
            => true
          (core/get-fields (get-scratch-path :1))
            => {:genre "Rock"}))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :3))]
    (fact "it overwrites and copies in ALL fields (flac)"
          (core/add-new-tag! (get-scratch-path :3)
                             (song3 :flac-fields))
            => true
          (core/get-fields (get-scratch-path :3))
            => (dissoc (song3 :flac-fields) :artwork))
    (fact "it overwrites old tag, clearing all fields and returns true (flac)"
          (core/add-new-tag! (get-scratch-path :3)
                             (get-in test-files [:tags :a]))
            => true
          (core/get-fields (get-scratch-path :3))
            => (get-in test-files [:tags :a]))
    (fact "it updates the genre tag correctly on flac files"
          (core/add-new-tag! (get-scratch-path :3) {:genre "Rock"})
            => true
          (core/get-fields (get-scratch-path :3))
            => {:genre "Rock"}))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :4))]
    (fact "it overwrites and copies in ALL fields (m4a)"
          (core/add-new-tag! (get-scratch-path :4)
                             (song3 :aac-fields))
            => true
          (core/get-fields (get-scratch-path :4))
            => (dissoc (song3 :aac-fields) :artwork))
    (fact "it overwrites old tag, clearing all fields and returns true (m4a)"
          (core/add-new-tag! (get-scratch-path :4)
                             (get-in test-files [:tags :a]))
            => true
          (core/get-fields (get-scratch-path :4))
            => (get-in test-files [:tags :a]))
    (fact "it updates the genre tag correctly on files (m4a)"
          (core/add-new-tag! (get-scratch-path :4) {:genre "Rock"})
            => true
          (core/get-fields (get-scratch-path :4))
            => {:genre "Rock"}))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :5))]
    (fact "it overwrites and copies in ALL fields (ogg)"
          (core/add-new-tag! (get-scratch-path :5)
                             (song3 :ogg-fields))
            => true
          (core/get-fields (get-scratch-path :5))
            => (dissoc (song3 :ogg-fields) :artwork))
    (fact "it overwrites old tag, clearing all fields and returns true (ogg)"
          (core/add-new-tag! (get-scratch-path :5)
                             (get-in test-files [:tags :a]))
            => true
          (core/get-fields (get-scratch-path :5))
            => (get-in test-files [:tags :a]))
    (fact "it updates the genre tag correctly on files (ogg)"
          (core/add-new-tag! (get-scratch-path :5) {:genre "Rock"})
            => true
          (core/get-fields (get-scratch-path :5))
            => {:genre "Rock"})))
(facts 
  "about update-tag!"
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :1))]
    (fact "it updates a single value to the existing tag and returns true (mp3)"
          (core/update-tag! (get-scratch-path :1)
                             {:title "updated-title"})
          => true
          (core/get-fields (get-scratch-path :1))
          => (merge (core/get-fields (get-in test-files [:paths :1])) 
                    {:title "updated-title"}))
    (fact "it updates multiple values to the existing tag and returns true (mp3)"
          (core/update-tag! (get-scratch-path :1)
                             {:title "updated-title" :artist "updated-artist"})
          => true
          (core/get-fields (get-scratch-path :1))
          => (merge (core/get-fields (get-in test-files [:paths :1])) 
                    {:title "updated-title" :artist "updated-artist"}))
    (fact "it ignores unsupported fields and writes supported fields"
          (core/update-tag! (get-scratch-path :1)
                             {:title "updated-title" :artist "updated-artist"
                              :bad-field 1})
          => true
          (core/get-fields (get-scratch-path :1))
          => (merge (core/get-fields (get-in test-files [:paths :1])) 
                    {:title "updated-title" :artist "updated-artist"}))
    (fact "it returns an error string if file doesn't exist (mp3)"
          (core/update-tag! "bad/path" {:title ""})
          => (contains ""))
    (fact "it updates the genre tag correctly on mp3 files"
          (core/update-tag! (get-scratch-path :1) {:genre "Rock"})
          => true
          (core/get-fields (get-scratch-path :1))
          => (merge (core/get-fields (get-in test-files [:paths :1])) 
                    {:genre "Rock"})))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :3))]
    (fact "it updates a single value to the existing tag and returns true (flac)"
          (core/update-tag! (get-scratch-path :3)
                             {:title "updated-title"})
          => true
          (core/get-fields (get-scratch-path :3))
          => (merge (core/get-fields (get-in test-files [:paths :3])) 
                    {:title "updated-title"}))
    (fact "it updates multiple values to the existing tag and returns true (flac)"
          (core/update-tag! (get-scratch-path :3)
                             {:title "updated-title" :artist "updated-artist"})
          => true
          (core/get-fields (get-scratch-path :3))
          => (merge (core/get-fields (get-in test-files [:paths :3])) 
                    {:title "updated-title" :artist "updated-artist"}))
    (fact "it returns an error string if file doesn't exist (flac)"
          (core/update-tag! "bad/path" {:title ""})
          => (contains ""))
    (fact "it updates the genre tag correctly on mp3 files"
          (core/update-tag! (get-scratch-path :3) {:genre "Rock"})
          => true
          (core/get-fields (get-scratch-path :3))
          => (merge (core/get-fields (get-in test-files [:paths :3])) 
                    {:genre "Rock"})))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :4))]
    (fact "it updates a single value to the existing tag and returns true (m4a)"
          (core/update-tag! (get-scratch-path :4)
                             {:title "updated-title"})
          => true
          (core/get-fields (get-scratch-path :4))
          => (merge (core/get-fields (get-in test-files [:paths :4])) 
                    {:title "updated-title"}))
    (fact "it updates multiple values to the existing tag and returns true (m4a)"
          (core/update-tag! (get-scratch-path :4)
                             {:title "updated-title" :artist "updated-artist"})
          => true
          (core/get-fields (get-scratch-path :4))
          => (merge (core/get-fields (get-in test-files [:paths :4])) 
                    {:title "updated-title" :artist "updated-artist"}))
    (fact "it returns an error string if file doesn't exist (m4a)"
          (core/update-tag! "bad/path" {:title ""})
          => (contains ""))
    (fact "it updates the genre tag correctly on mp3 files"
          (core/update-tag! (get-scratch-path :4) {:genre "Rock"})
          => true
          (core/get-fields (get-scratch-path :4))
          => (merge (core/get-fields (get-in test-files [:paths :4])) 
                    {:genre "Rock"})))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :5))]
    (fact "it updates a single value to the existing tag and returns true (ogg)"
          (core/update-tag! (get-scratch-path :5)
                            {:title "updated-title"})
          => true
          (core/get-fields (get-scratch-path :5))
          => (merge (core/get-fields (get-in test-files [:paths :5])) 
                    {:title "updated-title"}))
    (fact "it updates multiple values to the existing tag and returns true (ogg)"
          (core/update-tag! (get-scratch-path :5)
                            {:title "updated-title" :artist "updated-artist"})
          => true
          (core/get-fields (get-scratch-path :5))
          => (merge (core/get-fields (get-in test-files [:paths :5])) 
                    {:title "updated-title" :artist "updated-artist"}))
    (fact "it returns an error string if file doesn't exist (ogg)"
          (core/update-tag! "bad/path" {:title ""})
          => (contains ""))
    (fact "it updates the genre tag correctly on mp3 files"
          (core/update-tag! (get-scratch-path :5) {:genre "Rock"})
          => true
          (core/get-fields (get-scratch-path :5))
          => (merge (core/get-fields (get-in test-files [:paths :5])) 
                    {:genre "Rock"}))))
(facts
  "about get-image"
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :6))]
    (fact "it reads an image into a map containing correct mimetype and
         a byte array (mp3)"
          (let [m (core/get-image (get-scratch-path :6))]
            (do 
              (type m) => (type {})
              (:mimetype m) 
              => "image/png"
              (seq (:data m))
              =>(seq (get-byte-array (get-in test-files [:images :1])))))))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :5))]
    (fact "it reads an image into a map containing correct mimetype and
         a byte array (ogg)"
          (let [m (core/get-image (get-scratch-path :5))]
            (do 
              (type m) => (type {})
              (:mimetype m) 
              => "image/png"
              (seq (:data m))
              =>(seq (get-byte-array (get-in test-files [:images :1])))))))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :4))]
    (fact "it reads an image into a map containing correct mimetype and
         a byte array (m4a)"
          (let [m (core/get-image (get-scratch-path :4))]
            (do 
              (type m) => (type {})
              (:mimetype m) 
              => "image/png"
              (seq (:data m))
              =>(seq (get-byte-array (get-in test-files [:images :1])))))))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :3))]
    (fact "it reads an image into a map containing correct mimetype and
         a byte array (flac)"
          (let [m (core/get-image (get-scratch-path :3))]
            (do 
              (type m) => (type {})
              (:mimetype m) 
              => "image/png"
              (seq (:data m))
              =>(seq (get-byte-array (get-in test-files [:images :1])))))))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :3))]
    (fact "it reads an image into a map containing correct mimetype and
         a byte array when given a java file (flac)"
          (let [m (core/get-image (as-file (get-scratch-path :3)))]
            (do 
              (type m) => (type {})
              (:mimetype m) 
              => "image/png"
              (seq (:data m))
              =>(seq (get-byte-array (get-in test-files [:images :1])))))))
  (against-background 
    [(after :facts (clear-scratch))
     (before :facts (copy-to-scratch :7))]
    (fact "returns nil if the file doesn't have artwork"
          (core/get-image (as-file (get-scratch-path :7)))
          => nil))
  (fact "it returns nil when the file doesn't exist"
        (core/get-image "bad path")
        => nil))
