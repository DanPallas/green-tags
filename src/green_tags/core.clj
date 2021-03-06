(ns green-tags.core
  (:require [clojure.java.io :refer [as-file]]
            [clojure.string :as string]
            [clojure.set :refer [difference]])
  (:import [org.jaudiotagger.audio AudioFile AudioFileIO]
          [org.jaudiotagger.tag Tag FieldKey]
          [org.jaudiotagger.tag.datatype Artwork]
          [java.util.logging Logger Level]))

(defmacro ^:private debug [label i]
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

(def ^:private field-ids (dissoc (reduce #(assoc %1 (enum-val->key %2) %2) 
                               {} 
                               (vec (FieldKey/values)))
                       :cover-art))
(def ^{:doc "fields supported in mp3"} 
  mp3-fields #{:track :track-total :disc-no :disc-total :title :artist :album 
               :album-artist :year :genre :comment :composer :original-artist 
               :remixer :conductor :bpm :grouping :isrc :record-label :encoder 
               :lyricist :lyrics})
(def ^{:doc "fields supported in mp4"} 
  mp4-fields #{:track :track-total :disc-no :disc-total :title :artist :album 
               :album-artist :year :genre :comment :composer :conductor :bpm 
               :grouping :isrc :encoder :lyricist :lyrics})
(def ^{:doc "fields supported in flac/mp3"} 
  vorbis-fields #{:track :disc-no :title :artist :album :album-artist :year 
                 :genre :comment :composer :conductor :bpm :isrc :encoder 
                 :lyricist :lyrics})
(defn- get-field-set
  "returns set of supported field keys"
  [tag]
  (cond
    (or (instance? org.jaudiotagger.tag.flac.FlacTag tag) 
        (instance? org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag tag)) 
    vorbis-fields
    (instance? org.jaudiotagger.tag.mp4.Mp4Tag tag) 
    mp4-fields
    :else mp3-fields))

(defn- sanitize-tag-map
  "return tag-map with only the fields supported by that tag type"
  [tag tag-map]
  (apply (partial dissoc tag-map) (difference (set (keys tag-map)) 
                                              (get-field-set tag))))

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

(defn- get-image
  "Returns the first artwork field of file path. Returns as a map with two 
  entries data (byte array) and mimetype (string). Returns nil if path is bad
  or if the file doesn't have any artwork."
  [tag]
  (when-let [art (.getFirstArtwork tag)]
      {:artwork-mime (.getMimeType art)
       :artwork-data (.getBinaryData art)}))

(defn get-fields
  "returns a tag-map from audio file tag fields"
  [path] 
  (when-let [tag (get-tag path)] 
      (->
        (reduce (fn [m field] 
                  (let 
                    [v  (.getFirst tag (field-ids field))]
                    (if-not (= v "") 
                      (assoc m field v)
                      m))) 
                {} 
                (keys field-ids))
        (merge (get-image tag)))))

(defn get-header-info
  "get header info from path"
  [path]
  (let
    [f (if (instance? org.jaudiotagger.audio.AudioFile path)
         path (get-audio-file path))
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
  "get all header info and fields from path in one map"
  [path]
  (let
    [f (if (instance? org.jaudiotagger.audio.AudioFile path)
         path (get-audio-file path))]
    (if (nil? f) nil (conj (get-header-info f) (get-fields f)))))


(defn- set-image-fields
  [tag {:keys [artwork-data artwork-mime artwork-file]}]
  (if (or (= artwork-data :delete) (= artwork-mime :delete))
      (.deleteArtworkField tag)
   (let [art (if-let [art (.getFirstArtwork tag)] art (Artwork.))]
    (if artwork-file
      (.setFromFile art (as-file artwork-file))
      (do
        (.setMimeType art artwork-mime)
        (.setBinaryData art artwork-data)))
        (.setField tag art))))

(defn- set-field
  [tag [k v]]
  (if (= v :delete)
    (.deleteField tag (field-ids k))
    (.setField tag (field-ids k) v)))

(defn- set-fields
  "sets all fields in tag to the values in tag-map"
  [tag tag-map]
  (if (or (:artwork-mime tag-map) 
          (:artwork-data tag-map)
          (:artwork-file tag-map)) 
      (set-image-fields tag tag-map))
  (doall (map (partial set-field tag) 
              (vec (sanitize-tag-map tag tag-map))))
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
  new tag with the values from the map. Returns true on success, otherwise
  returns an error string. It ignores unsupported fields in the tag-map. 
  Additonally, it accepts the field :artwork-file which is the path of an 
  image file to be loaded into the artwork-mime and artwork-data fields."
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

(defn update-tag!
  "Takes a path (file/string), and a tag-map. Updates/adds fields from tag-map 
  to the existing tag on the file. It ignores unsupported fields in the 
  tag-map. If :deleted is passed as a value, then the field is deleted from the 
  tag. Additonally, it accepts the field :artwork-file which is the path of an 
  image file to be loaded into the artwork-mime and artwork-data fields."
  [path tag-map]
  (let [f (get-audio-file path)]
    (try 
      (let [ t (.getTagOrCreateDefault f)]
        (set-fields t tag-map)
        (.setTag f t)
        (.commit f)
        true)
      (catch Exception e
        (.toString e)))))
