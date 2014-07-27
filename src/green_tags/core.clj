(ns green-tags.core
  (:require [clojure.java.io :refer [as-file]]
            [clojure.string :as string])
  (:import [org.jaudiotagger.audio AudioFile AudioFileIO]
          [org.jaudiotagger.tag Tag FieldKey]))

(defn- enum-val->key
  [enum-val]
 (keyword (string/replace 
  (string/lower-case (str enum-val))
  "_"
  "-")))

(def field-ids (reduce #(assoc %1 (enum-val->key %2) %2) 
                    {} 
                    (vec (FieldKey/values))))

(def mp3 (as-file "test/resources/library/unorganized.mp3"))

(defn- get-audio-file
  [f]
  (AudioFileIO/read f))

(defn- get-tag
  "get a Tag implementing object from an AudioFile f"
  [f]
  (let [f (if (= (type f) java.io.File) (get-audio-file f) f)]
   (.getTag f)))

(defn get-fields
  [f] 
  (let
    [tags (get-tag f)] 
    (reduce (fn [m field] 
              (let 
                [v  (.getFirst tags (field-ids field))]
                (if-not (= v "") 
                  (assoc m field v)
                  m))) 
            {} 
            (keys fields-ids))))

(defn get-header-info
  "get header info from file f"
  [f]
  (let
    [f (if (= (type f) java.io.File) (get-audio-file f) f)
     header (.getAudioHeader f)]
    {:bit-rate (.getBitRateAsNumber header)
     :channels (.getChannels header)
     :encoding-type (.getEncodingType header)
     :format (.getFormat header)
     :sample-rate (.getSampleRateAsNumber header)
     :length (.getTrackLength header)
     :variable-bit-rate (.isVariableBitRate header)}))

(defn get-all-info
  "get all header info and fields from file f in one map"
  [f]
  (let
    [f (get-audio-file f)]
    (conj (get-header-info f) (get-fields f))))

(get-header-info mp3)

(get-all-info mp3)
