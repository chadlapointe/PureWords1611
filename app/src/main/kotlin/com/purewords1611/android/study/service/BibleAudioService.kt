package com.purewords1611.android.study.service

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.*
import android.speech.tts.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.purewords1611.android.MainActivity
import com.purewords1611.android.R
import com.purewords1611.android.study.data.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BibleAudioService : Service(), TextToSpeech.OnInitListener {

    @Inject
    lateinit var repository: StudyRepository

    private var tts: TextToSpeech? = null
    private var mediaSession: MediaSessionCompat? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = -1
    private val queue = mutableListOf<VerseText>()
    private var currentIndex = -1
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _isPlaying = MutableStateFlow(value = false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(value = false)
    @Suppress("unused")
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    private val _currentVerseId = MutableStateFlow<Long?>(value = null)
    val currentVerseId: StateFlow<Long?> = _currentVerseId.asStateFlow()

    private var currentOrthographyMode = OrthographyMode.ORIGINAL_1611
    private var currentTranslationMode = TranslationMode.KJV_1611
    private var speechRate = 0.85f // More deliberate and biblical pace
    private var selectedVoiceName: String? = null
    private var isParallelMode = false
    private var isPlayingPart2 = false

    // Regional voice preferences
    private val PREFERRED_1611_VOICES = listOf(
        "en-gb-x-gbd-network", // Deep male British
        "en-gb-x-rjs-network", 
        "en-gb-x-fis-network", 
        "en-gb-x-fis-local"
    )
    private val PREFERRED_ESV_VOICES = listOf(
        "en-us-x-iol-network", // Deep male US
        "en-us-x-iol-local",
        "en-us-x-tgs-network", 
        "en-us-x-sfg-network", 
        "en-us-x-tpc-network",
        "en-us-x-ana-network"
    )

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    inner class AudioBinder : Binder() {
        fun getService(): BibleAudioService = this@BibleAudioService
    }

    private val binder = AudioBinder()

    override fun onCreate() {
        super.onCreate()
        
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioSessionId = audioManager.generateAudioSessionId()
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(2500) // 25dB boost for even more volume
                enabled = true
            }
            
            // Add Equalizer for speech clarity (boost mid-range)
            equalizer = Equalizer(0, audioSessionId).apply {
                if (numberOfBands > 0) {
                    val midBand = (numberOfBands / 2).toShort()
                    setBandLevel(midBand, 1000) // +10dB for mid-range clarity
                }
                enabled = true
            }
        } catch (e: Exception) {
            android.util.Log.e("BibleAudioService", "Audio effects init failed", e)
        }

        tts = TextToSpeech(this, this)
        
        mediaSession = MediaSessionCompat(this, "BibleAudioService").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        resume()
                    }

                    override fun onPause() {
                        pause()
                    }

                    override fun onSkipToNext() {
                        skipToNext()
                    }

                    override fun onSkipToPrevious() {
                        skipToPrevious()
                    }

                    override fun onStop() {
                        stopSelf()
                    }
                },
            )
            isActive = true
        }
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            tts?.language = Locale.US
            tts?.setPitch(0.92f) // Slightly higher for better clarity on small speakers
            tts?.setSpeechRate(speechRate)
            
            val voices = tts?.voices ?: emptySet()
            android.util.Log.i("BibleAudioService", "All Available Voices: ${voices.joinToString { it.name }}")
            
            // Priority list for high-quality biblical male voices
            val bestVoice = voices.asSequence()
                .filter { it.locale.language == "en" }
                .sortedWith(
                    compareByDescending<Voice> { v ->
                        // Prefer Neural/Studio/Premium voices for highest quality
                        if (v.name.contains("neural", ignoreCase = true) || v.name.contains("studio", ignoreCase = true) || v.name.contains("premium", ignoreCase = true)) 10 else 0
                    }.thenByDescending { v ->
                        // Prefer specific high-quality engines
                        if (v.name.contains("en-us-x-sfg", ignoreCase = true) || v.name.contains("en-us-x-tgs", ignoreCase = true)) 8 else 0
                    }.thenByDescending { v ->
                        // Prefer higher quality rating
                        when (v.quality) {
                            Voice.QUALITY_VERY_HIGH -> 5
                            Voice.QUALITY_HIGH -> 4
                            Voice.QUALITY_NORMAL -> 3
                            else -> 1
                        }
                    }.thenByDescending { v ->
                        // Prefer male for biblical feel
                        if (v.name.contains("male", ignoreCase = true)) 2 else 0
                    }.thenByDescending { v ->
                        // Prefer network voices (usually better neural models)
                        if (v.isNetworkConnectionRequired) 1 else 0
                    },
                ).firstOrNull()

            if (bestVoice != null) {
                tts?.voice = bestVoice
                selectedVoiceName = bestVoice.name
                android.util.Log.i("BibleAudioService", "Selected Initial Voice: ${bestVoice.name}")
            }
            
            // Set initial voice based on current mode
            updateVoiceForTranslation(currentTranslationMode)

            tts?.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId?.startsWith("verse_part") != true) {
                            utteranceId?.removePrefix("verse_")?.toLongOrNull()?.let { _currentVerseId.value = it }
                        } else {
                            val id = utteranceId.substringAfterLast("_").toLongOrNull()
                            if (id != null) _currentVerseId.value = id
                        }
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId?.startsWith("verse_part1_") == true) {
                            isPlayingPart2 = true
                            serviceScope.launch { playCurrent() }
                        } else {
                            isPlayingPart2 = false
                            skipToNext()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        isPlayingPart2 = false
                    }
                },
            )
        }
    }

    fun playQueue(verses: List<VerseText>, startVerseId: Long? = null, orthographyMode: OrthographyMode = OrthographyMode.ORIGINAL_1611, translationMode: TranslationMode = TranslationMode.KJV_1611) {
        queue.clear()
        queue.addAll(verses)
        currentIndex = if (startVerseId != null) {
            val idx = verses.indexOfFirst { it.id == startVerseId }
            if (idx != -1) idx else 0
        } else 0
        currentOrthographyMode = orthographyMode
        currentTranslationMode = translationMode
        
        // Automatically switch voice based on translation
        updateVoiceForTranslation(translationMode)
        
        _isPaused.value = false
        playCurrent()
    }

    private fun updateVoiceForTranslation(mode: TranslationMode) {
        val voices = tts?.voices ?: return
        val preferredList = if (mode == TranslationMode.KJV_1611) PREFERRED_1611_VOICES else PREFERRED_ESV_VOICES
        
        val bestVoice = preferredList.asSequence().mapNotNull { name ->
            voices.find { it.name.contains(name, ignoreCase = true) }
        }.firstOrNull() ?: voices.asSequence().filter { 
            val targetLang = if (mode == TranslationMode.KJV_1611) "en-GB" else "en-US"
            it.locale.toLanguageTag().contains(targetLang, ignoreCase = true) 
        }.firstOrNull()

        if (bestVoice != null) {
            tts?.voice = bestVoice
            selectedVoiceName = bestVoice.name
            
            // Adjust pitch for a deeper, resonant tone
            if (mode == TranslationMode.KJV_1611) {
                tts?.setPitch(0.82f) // Deep for 1611
            } else {
                tts?.setPitch(0.92f) // Refined for better clarity on mobile speakers
            }
            
            android.util.Log.i("BibleAudioService", "Switched voice to: ${bestVoice.name} for mode $mode")
        }
    }

    fun setParallelMode(enabled: Boolean) {
        isParallelMode = enabled
    }

    fun onVersionChanged(orthography: OrthographyMode, translation: TranslationMode) {
        currentOrthographyMode = orthography
        currentTranslationMode = translation
        updateVoiceForTranslation(translation)
        if (_isPlaying.value) {
            isPlayingPart2 = false
            playCurrent()
        }
    }

    private fun playCurrent() {
        if (currentIndex !in queue.indices) {
            _isPlaying.value = false
            _isPaused.value = false
            _currentVerseId.value = null
            isPlayingPart2 = false
            
            // Chapter finished
            if (queue.isNotEmpty()) {
                val lastVerse = queue.last()
                serviceScope.launch {
                    repository.markChapterCompleted(lastVerse.book, lastVerse.chapter)
                }
            }
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        
        requestAudioFocus()
        
        val verse = queue[currentIndex]
        
        if (isParallelMode && !isPlayingPart2) {
            playParallelPart1(verse)
        } else if (isParallelMode && isPlayingPart2) {
            playParallelPart2(verse)
        } else {
            playSingle(verse)
        }
    }

    private fun playSingle(verse: VerseText) {
        val rawText = when (currentTranslationMode) {
            TranslationMode.ESV -> verse.comparativeText ?: verse.modernizedText
            TranslationMode.KJV_STANDARD -> verse.standardText ?: verse.modernizedText
            TranslationMode.KJV_1611 -> if (currentOrthographyMode == OrthographyMode.ORIGINAL_1611) verse.originalText else verse.modernizedText
        }
        speakText(verse, rawText, "verse_${verse.id}")
    }

    private fun playParallelPart1(verse: VerseText) {
        updateVoiceForTranslation(TranslationMode.KJV_1611)
        val text = normalizeForSpeech(verse.originalText)
        speakText(verse, text, "verse_part1_${verse.id}")
    }

    private fun playParallelPart2(verse: VerseText) {
        val targetTranslation = if (currentTranslationMode == TranslationMode.KJV_1611) TranslationMode.ESV else currentTranslationMode
        updateVoiceForTranslation(targetTranslation)
        val rawText = if (targetTranslation == TranslationMode.ESV) verse.comparativeText ?: verse.modernizedText
        else verse.standardText ?: verse.modernizedText
        
        val text = normalizeForSpeech(rawText)
        speakText(verse, text, "verse_part2_${verse.id}")
    }

    private fun speakText(verse: VerseText, text: String, utteranceId: String) {
        val textToSpeak = normalizeForSpeech(text)
        _isPlaying.value = true
        updateMetadata(verse)
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startForeground(NOTIFICATION_ID, createNotification())
        
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            if (audioSessionId != -1) {
                putInt(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, audioSessionId)
            }
        }
        
        tts?.playSilentUtterance(450, TextToSpeech.QUEUE_ADD, null)
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    if ((focusChange == AudioManager.AUDIOFOCUS_LOSS) || (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)) {
                        pause()
                    }
                }
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun normalizeForSpeech(text: String): String {
        // Clean up markdown/italics
        var result = text.replace("_", "")
        
        // Add subtle pauses for a storytelling feel without volume dipping
        result = result.replace(". ", ".  ")
                     .replace(", ", ",  ")
                     .replace("; ", ";  ")
                     .replace(": ", ":  ")
                     .replace("! ", "!  ")
                     .replace("? ", "?  ")

        if (currentOrthographyMode != OrthographyMode.ORIGINAL_1611) return result

        // Advanced 1611 phonetic normalization for modern TTS engines
        val replacements = mapOf(
            "heauen" to "heaven", "euery" to "every", "liue" to "live", "loue" to "love", "haue" to "have",
            "giue" to "give", "vnto" to "unto", "vpon" to "upon", "vs" to "us", "Iesus" to "Jesus",
            "Ierusalem" to "Jerusalem", "Iohn" to "John", "Ioseph" to "Joseph", "iudge" to "judge",
            "iudgement" to "judgment", "vaine" to "vain", "doeth" to "do-eth", "sayeth" to "say-eth",
            "reioyce" to "rejoice", "beleeue" to "believe", "shalbe" to "shall be", "wilbe" to "will be",
            "hath" to "hath", "doth" to "doth", "thou" to "thou", "thee" to "thee", "thy" to "thy", "thine" to "thine",
            "vnder" to "under", "vp" to "up", "vpper" to "upper", "vnto" to "unto",
        )

        result = result.split(" ").joinToString(" ") { word ->
            var w = word
            val lastChar = w.lastOrNull()
            val hasPunc = (lastChar != null) && !lastChar.isLetterOrDigit()
            val base = if (hasPunc) w.dropLast(1) else w
            
            val mapped = replacements[base.lowercase(Locale.ROOT)]
            if (mapped != null) {
                w = if (base.getOrNull(0)?.isUpperCase() == true) {
                    mapped.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                } else mapped
                if (hasPunc) w += lastChar
            } else if (w.startsWith("v", ignoreCase = true) && (w.length > 2) && (!"aeiouAEIOU".contains(w[1]))) {
                // Medial U or Initial V replacement logic
                w = w.replaceFirst("v", "u", ignoreCase = true).replaceFirst("V", "U", ignoreCase = true)
            }
            w
        }
        
        return result
    }

    private fun resume() { 
        if ((_isPaused.value) && (currentIndex in queue.indices)) {
            _isPaused.value = false
            playCurrent()
        } else if (currentIndex in queue.indices) {
            playCurrent()
        }
    }
    
    private fun pause() { 
        tts?.stop()
        _isPlaying.value = false
        _isPaused.value = true
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun stop() {
        tts?.stop()
        queue.clear()
        currentIndex = -1
        _isPlaying.value = false
        _isPaused.value = false
        _currentVerseId.value = null
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun skipToNext() { 
        if (currentIndex < (queue.size - 1)) { 
            currentIndex++
            playCurrent() 
        } else { 
            currentIndex = queue.size // Mark as finished
            playCurrent()
        } 
    }
    private fun skipToPrevious() { if (currentIndex > 0) { currentIndex--; playCurrent() } }

    private fun updateMetadata(verse: VerseText) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "${verse.book} ${verse.chapter}:${verse.verse}")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Pure Words 1611")
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_STOP)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun createNotification(): Notification {
        val description = mediaSession?.controller?.metadata?.description
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(description?.title).setContentText(description?.subtitle).setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession?.sessionToken).setShowActionsInCompactView(0, 1, 2))
        builder.addAction(android.R.drawable.ic_media_previous, "Previous", PendingIntent.getService(this, 1, Intent(this, BibleAudioService::class.java).apply { action = ACTION_PREV }, PendingIntent.FLAG_IMMUTABLE))
        val playPauseAction = if (_isPlaying.value) ACTION_PAUSE else ACTION_PLAY
        builder.addAction(if (_isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, "Play/Pause", PendingIntent.getService(this, 2, Intent(this, BibleAudioService::class.java).apply { action = playPauseAction }, PendingIntent.FLAG_IMMUTABLE))
        builder.addAction(android.R.drawable.ic_media_next, "Next", PendingIntent.getService(this, 3, Intent(this, BibleAudioService::class.java).apply { action = ACTION_NEXT }, PendingIntent.FLAG_IMMUTABLE))
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Bible Audio", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_STOP_SERVICE -> stop()
            ACTION_NEXT -> skipToNext()
            ACTION_PREV -> skipToPrevious()
        }
        return START_NOT_STICKY
    }

    fun getSpeechRate(): Float = speechRate
    fun setSpeechRate(rate: Float) { speechRate = rate; tts?.setSpeechRate(rate) }
    fun getAvailableVoices(): List<String> = tts?.voices?.asSequence()?.filter { it.locale.language == "en" }?.map { it.name }?.toList() ?: emptyList()
    fun getSelectedVoice(): String? = selectedVoiceName
    fun setVoice(voiceName: String) { selectedVoiceName = voiceName; tts?.voices?.find { it.name == voiceName }?.let { tts?.voice = it } }
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { 
        super.onDestroy()
        serviceScope.cancel()
        tts?.stop()
        tts?.shutdown()
        mediaSession?.release()
        loudnessEnhancer?.release()
        equalizer?.release()
    }

    companion object {
        private const val CHANNEL_ID = "bible_audio_channel"; private const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.purewords1611.android.ACTION_PLAY"; const val ACTION_PAUSE = "com.purewords1611.android.ACTION_PAUSE"
        const val ACTION_STOP_SERVICE = "com.purewords1611.android.ACTION_STOP"
        const val ACTION_NEXT = "com.purewords1611.android.ACTION_NEXT"; const val ACTION_PREV = "com.purewords1611.android.ACTION_PREV"
    }
}
