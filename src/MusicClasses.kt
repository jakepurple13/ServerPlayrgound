package com.example

data class MusicBase(val message: Message?)

data class Body(val track_list: List<Track_list615124581>?)

data class Header(val status_code: Number?, val execute_time: Number?, val available: Number?)

data class Message(val header: Header?, val body: Body?)

data class Music_genre(val music_genre_id: Number?, val music_genre_parent_id: Number?, val music_genre_name: String?, val music_genre_name_extended: String?, val music_genre_vanity: String?)

data class Music_genre_list1397122927(val music_genre: Music_genre?)

data class Music_genre_list1457697838(val music_genre: Music_genre?)

data class Music_genre_list1777044154(val music_genre: Music_genre?)

data class Music_genre_list180020636(val music_genre: Music_genre?)

data class Music_genre_list1865556572(val music_genre: Music_genre?)

data class Music_genre_list1917713930(val music_genre: Music_genre?)

data class Music_genre_list201831901(val music_genre: Music_genre?)

data class Music_genre_list673761987(val music_genre: Music_genre?)

data class Primary_genres(val music_genre_list: List<Music_genre_list1457697838>?)

data class Track(val track_id: Number?, val track_name: String?, val track_name_translation_list: List<Any>?, val track_rating: Number?, val commontrack_id: Number?, val instrumental: Number?, val explicit: Number?, val has_lyrics: Number?, val has_subtitles: Number?, val has_richsync: Number?, val num_favourite: Number?, val album_id: Number?, val album_name: String?, val artist_id: Number?, val artist_name: String?, val track_share_url: String?, val track_edit_url: String?, val restricted: Number?, val updated_time: String?, val primary_genres: Primary_genres?)

data class Track_list615124581(val track: Track?)

data class SnippetBase(val message: SnippetMessage?)

data class SnippetBody(val snippet: MusicSnippet?)

data class SnippetHeader(val status_code: Number?, val execute_time: Number?)

data class SnippetMessage(val header: SnippetHeader?, val body: SnippetBody?)

data class MusicSnippet(val snippet_id: Number?, val snippet_language: String?, val restricted: Number?, val instrumental: Number?, val snippet_body: String?, val script_tracking_url: String?, val pixel_tracking_url: String?, val html_tracking_url: String?, val updated_time: String?)