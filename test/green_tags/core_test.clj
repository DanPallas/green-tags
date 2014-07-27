(ns green-tags.core-test
  (:require [clojure.java.io :refer [as-file]]
            [clojure.set :as sets]
            [clojure.data :as data]
            [green-tags.core :as core]
            [midje.sweet :refer :all]))
(keys (core/get-fields (as-file "test/resources/tagged/song3.mp3")))
(core/get-header-info (as-file "test/resources/tagged/song3.m4a"))
(keys (core/get-fields (as-file "test/resources/tagged/song3.flac")))
(core/get-header-info (as-file "test/resources/tagged/song3.ogg"))
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

