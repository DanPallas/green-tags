green-tags
==========

[![Clojars Project](http://clojars.org/green-tags/latest-version.svg)](http://clojars.org/green-tags)

green-tags is an audio/music tagging library supporing mp3, mp4, flac, and ogg containers. It supports all of the common fields used in these files including one image in a simple abstracted way which hides most of the differences between the formats. 

Supported tags: 
    
    mp3: track, track-total, disc-no, disc-total, title,
      artist, album, album-artist, year, genre, comment, composer, 
      original-artist, remixer, conductor, bpm, grouping, isrc, record-label, 
      encoder, lyricist, lyrics, artwork-mime, artwork-data (byte array)
    
    mp4: all from mp3 except original-artist, remixer, record-label
    
    ogg/flac: all except original-artist, track-total, record-label, disc-total,
      remixer, grouping
               
#Examples

ex. To get read all supported tags and image into a map: (get-all-info <path>) a map will be returned with all tag data.

ex. To update an existing tag with a new artist and album while leaving all other fields untouched, use 
(update-tag! \<file\> {:artist "new artist", :album "new album"})

ex. To delete the lyrics field, use (update-tag! \<file\> {:lyrics :delete})


#Contributing
 Pull requests are welcome, but try to keep the API simple and submit updated tests to go along with it. Don't use macros in the API because they complicate test mocking for any project that implements this library.

[api documentation](http://danpallas.github.io/green-tags/)
