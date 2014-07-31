(ns green-tags.core
  (:require [clojure.java.io :refer [as-file]]
            [clojure.string :as string])
  (:import [org.jaudiotagger.audio AudioFile AudioFileIO]
          [org.jaudiotagger.tag Tag FieldKey]
          [java.util.logging Logger Level]))

(defmacro debug [label i]
  `(let [o# ~i
         _# (println (str "Debug " ~label ": " o#))]
     o#))

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
  "returns an audiofile from a path (file/string). Returns nil if file doesn't
  exist"
  [f]
  (let [f (if (instance? java.io.File f) f (as-file f))]
    (if (not (.exists f)) nil (AudioFileIO/read f))))

(defn- get-tag
  "get a Tag implementing object from an AudioFile f"
  [f]
  (let [f (if (instance? org.jaudiotagger.audio.AudioFile f)
            f 
            (get-audio-file f))]
    (if (nil? f) nil (.getTag f))))

(defn get-fields
  [f] 
  (let
    [tags (get-tag f)] 
    (if (nil? tags) 
      nil
      (reduce (fn [m field] 
              (let 
                [v  (.getFirst tags (field-ids field))]
                (if-not (= v "") 
                  (assoc m field v)
                  m))) 
            {} 
            (keys field-ids)))))

(defn get-header-info
  "get header info from path/file f"
  [f]
  (let
    [f (if (instance? org.jaudiotagger.audio.AudioFile f) f (get-audio-file f))
     header (if (nil? f) nil (.getAudioHeader f))]
    (if (nil? header)
     nil
     {:bit-rate (.getBitRateAsNumber header)
     :channels (.getChannels header)
     :encoding-type (.getEncodingType header)
     :format (.getFormat header)
     :sample-rate (.getSampleRateAsNumber header)
     :length (.getTrackLength header)
     :variable-bit-rate (.isVariableBitRate header)})))

(defn get-all-info
  "get all header info and fields from path/file f in one map"
  [f]
  (let
    [f (if (instance? org.jaudiotagger.audio.AudioFile f) f (get-audio-file f))]
    (if (nil? f) nil (conj (get-header-info f) (get-fields f)))))

(defn- set-fields
  "sets all fields in tag to the values in tag-map"
  [tag tag-map]
  (doall (map (fn [[k v]] (.setField tag (field-ids k) v)) (vec tag-map)))
  tag)

(defn- get-blank-tag!
  "Returns a blank tag of the appropriate type for AudioFile f. If called on 
  an mp3 file, the tag on the file is deleted."
  [f path]
  (let [t (.getTagOrCreateDefault f)] 
    (AudioFileIO/delete f)
    (if (or (instance? org.jaudiotagger.tag.flac.FlacTag t) 
            (instance? org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag t))
        (set-fields (.getTag (get-audio-file path)) {:encoder ""}) 
        (if (instance? org.jaudiotagger.tag.mp4.Mp4Tag t)
            (.getTag (get-audio-file path))
            (.createDefaultTag f))))) 

(defn add-new-tag!
  "Takes a path (file/string), removes the old tag from the file and writes a 
  new tag with the values from the map. Returns true on succes, otherwise
  returns an error string."
  [path tag-map]
  (let [f (get-audio-file path)]
    (try 
      (let [tag (get-blank-tag! f path)]
        (set-fields tag tag-map)
        (AudioFileIO/delete f)
        (.setTag f tag)
        (.commit f)
        true)
      (catch Exception e
        (.toString e))))) 
