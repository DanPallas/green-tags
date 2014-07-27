(ns green-tags.core
  (:require [clojure.java.io :refer [as-file]]
            [clojure.string :as string])
  (:import [org.jaudiotagger.audio AudioFile AudioFileIO]
          [org.jaudiotagger.tag Tag FieldKey]
          [java.util.logging Logger Level]))

(.setLevel (Logger/getLogger "org.jaudiotagger") Level/OFF)

(defn- enum-val->key
  [enum-val]
 (keyword (string/replace 
  (string/lower-case (str enum-val))
  "_"
  "-")))

(def field-ids (dissoc (reduce #(assoc %1 (enum-val->key %2) %2) 
                               {} 
                               (vec (FieldKey/values)))
                       :cover-art))

(defn- get-audio-file
  [f]
  (let [f (if (instance? java.io.File f) f (as-file f))]
    (AudioFileIO/read f)))

(defn- get-tag
  "get a Tag implementing object from an AudioFile f"
  [f]
  (let [f (if (instance? org.jaudiotagger.audio.AudioFile f)
            f (get-audio-file f))]
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
            (keys field-ids))))

(defn get-header-info
  "get header info from path/file f"
  [f]
  (let
    [f (if (instance? org.jaudiotagger.audio.AudioFile f) f (get-audio-file f))
     header (.getAudioHeader f)]
    {:bit-rate (.getBitRateAsNumber header)
     :channels (.getChannels header)
     :encoding-type (.getEncodingType header)
     :format (.getFormat header)
     :sample-rate (.getSampleRateAsNumber header)
     :length (.getTrackLength header)
     :variable-bit-rate (.isVariableBitRate header)}))

(defn get-all-info
  "get all header info and fields from path/file f in one map"
  [f]
  (let
    [f (if (instance? org.jaudiotagger.audio.AudioFile f) f (get-audio-file f))]
    (conj (get-header-info f) (get-fields f))))
