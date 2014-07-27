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
                 (assoc :id3-header {:bit-rate "128" :channels "Mono"
                                     :encoding-type "mp3"
                                     :format "MPEG1 Layer 3" 
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
                 (assoc :vorbis-fields 
                        (-> (reduce dissoc 
                                    fields 
                                    fields-not-in-vorbis)
                            (assoc :track "01"
                                   :encoder "reference libFLAC 1.2.1 200170917")
                            ))
                 (assoc :vorbis-header {:bit-rate 0 :channels "1"
                                        :encoding-type "FLAC 16 bits"
                                        :format "AAC" 
                                        :sample-rate 44100 :length 0
                                        :variable-bit-rate true}))))


