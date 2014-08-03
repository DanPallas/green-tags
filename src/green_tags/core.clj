(ns green-tags.core
  "Supported tags: 
    mp3: track, track-total, disc-no, disc-total, title,
      artist, album, album-artist, year, genre, comment, composer, 
      original-artist, remixer, conductor, bpm, grouping, isrc, record-label, 
      encoder, lyricist, lyrics
    aac: all from mp3 except original-artist, remixer, record-label
    ogg/flac: all except original-artist, track-total, record-label, disc-total,
      remixer, grouping"
  (:require [clojure.java.io :refer [as-file]]
            [clojure.string :as string]
            [clojure.set :refer [difference]])
  (:import [org.jaudiotagger.audio AudioFile AudioFileIO]
          [org.jaudiotagger.tag Tag FieldKey]
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

(defn get-fields
  "returns a tag-map from audio file tag fields"
  [path] 
  (let
    [tag (get-tag path)] 
    (if (nil? tag) 
      nil
      (-> (reduce (fn [m field] 
              (let 
                [v  (.getFirst tag (field-ids field))]
                (if-not (= v "") 
                  (assoc m field v)
                  m))) 
            {} 
            (keys field-ids))
          (merge (when-let [art (.getFirstArtwork tag)]
                   {:artwork (.getMimeType art)}))))))

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

(defn- set-fields
  "sets all fields in tag to the values in tag-map"
  [tag tag-map]
  (doall (map (fn [[k v]] (.setField tag (field-ids k) v)) 
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
  returns an error string. It ignores unsupported fields in the tag-map."
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
  tag-map."
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

#_(.getMimeType (.getFirstArtwork (get-tag (get-audio-file "test/resources/tagged/song3-no-art.mp3"))))
#_(add-new-tag! "test/resources/tagged/song3-no-art.mp3" {:title "test"})
#_(merge {:d "d"} nil)
