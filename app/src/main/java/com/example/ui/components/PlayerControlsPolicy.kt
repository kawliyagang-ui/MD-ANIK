package com.example.ui.components

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure playback control helpers & WebView JavaScript dispatch scripts
 * for seeking, volume synchronization, play/pause, and embed HTML framing.
 */
object PlayerControlsPolicy {
    const val SEEK_STEP_SECONDS = 10

    fun seekDeltaLabel(deltaSeconds: Int): String {
        return if (deltaSeconds >= 0) "+${deltaSeconds}s" else "${deltaSeconds}s"
    }

    fun volumeFractionToStreamIndex(fraction: Float, maxVolume: Int): Int {
        if (maxVolume <= 0) return 0
        val clamped = fraction.coerceIn(0f, 1f)
        return (clamped * maxVolume).roundToInt().coerceIn(0, maxVolume)
    }

    fun streamIndexToVolumeFraction(currentVolume: Int, maxVolume: Int): Float {
        if (maxVolume <= 0) return 0f
        return (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
    }

    fun volumePercentLabel(fraction: Float, isMuted: Boolean = false): String {
        val clamped = fraction.coerceIn(0f, 1f)
        if (isMuted || clamped <= 0.005f) return "Muted"
        return "${(clamped * 100f).roundToInt()}%"
    }

    fun buildSeekScript(deltaSeconds: Int): String {
        val key = if (deltaSeconds >= 0) "ArrowRight" else "ArrowLeft"
        val keyCode = if (deltaSeconds >= 0) 39 else 37
        return """
            (function() {
                var delta = $deltaSeconds;
                function seekInDoc(doc) {
                    if (!doc) return false;
                    var applied = false;
                    try {
                        var mediaEls = doc.querySelectorAll('video, audio');
                        for (var i = 0; i < mediaEls.length; i++) {
                            var m = mediaEls[i];
                            var dur = (typeof m.duration === 'number' && !isNaN(m.duration) && isFinite(m.duration)) ? m.duration : Number.MAX_SAFE_INTEGER;
                            var target = Math.max(0, Math.min(dur, (m.currentTime || 0) + delta));
                            m.currentTime = target;
                            applied = true;
                        }
                    } catch (e) {}
                    return applied;
                }
                seekInDoc(document);
                try {
                    var evtInit = { key: '$key', code: '$key', keyCode: $keyCode, which: $keyCode, bubbles: true, cancelable: true };
                    document.dispatchEvent(new KeyboardEvent('keydown', evtInit));
                    document.dispatchEvent(new KeyboardEvent('keyup', evtInit));
                } catch (e) {}
                try {
                    var frames = document.querySelectorAll('iframe');
                    for (var j = 0; j < frames.length; j++) {
                        var win = frames[j].contentWindow;
                        try { seekInDoc(frames[j].contentDocument); } catch (e) {}
                        if (win) {
                            try { win.postMessage(JSON.stringify({ event: 'command', func: 'seekBy', args: [delta] }), '*'); } catch (e) {}
                            try { win.postMessage({ type: 'seek', seconds: delta, delta: delta }, '*'); } catch (e) {}
                            try { win.postMessage({ method: 'seek', value: delta }, '*'); } catch (e) {}
                            try {
                                var fEvt = { key: '$key', code: '$key', keyCode: $keyCode, which: $keyCode, bubbles: true, cancelable: true };
                                win.document.dispatchEvent(new KeyboardEvent('keydown', fEvt));
                            } catch (e) {}
                        }
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }

    fun buildPlayPauseScript(shouldPlay: Boolean): String {
        val method = if (shouldPlay) "play" else "pause"
        val ytFunc = if (shouldPlay) "playVideo" else "pauseVideo"
        return """
            (function() {
                function toggleDoc(doc) {
                    if (!doc) return;
                    try {
                        var mediaEls = doc.querySelectorAll('video, audio');
                        for (var i = 0; i < mediaEls.length; i++) {
                            if ($shouldPlay) {
                                mediaEls[i].play && mediaEls[i].play();
                            } else {
                                mediaEls[i].pause && mediaEls[i].pause();
                            }
                        }
                    } catch (e) {}
                }
                toggleDoc(document);
                try {
                    var kEvt = { key: ' ', code: 'Space', keyCode: 32, which: 32, bubbles: true, cancelable: true };
                    document.dispatchEvent(new KeyboardEvent('keydown', kEvt));
                } catch (e) {}
                try {
                    var frames = document.querySelectorAll('iframe');
                    for (var j = 0; j < frames.length; j++) {
                        var win = frames[j].contentWindow;
                        try { toggleDoc(frames[j].contentDocument); } catch (e) {}
                        if (win) {
                            try { win.postMessage(JSON.stringify({ event: 'command', func: '$ytFunc', args: [] }), '*'); } catch (e) {}
                            try { win.postMessage({ method: '$method' }, '*'); } catch (e) {}
                            try { win.postMessage({ type: '$method' }, '*'); } catch (e) {}
                        }
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }

    fun buildVolumeScript(volumeFraction: Float, isMuted: Boolean): String {
        val clamped = volumeFraction.coerceIn(0f, 1f)
        val volFormatted = String.format(Locale.US, "%.2f", clamped)
        val volPercent = (clamped * 100f).roundToInt().coerceIn(0, 100)
        return """
            (function() {
                var vol = $volFormatted;
                var muted = $isMuted;
                function applyVol(doc) {
                    if (!doc) return;
                    try {
                        var mediaEls = doc.querySelectorAll('video, audio');
                        for (var i = 0; i < mediaEls.length; i++) {
                            mediaEls[i].volume = vol;
                            mediaEls[i].muted = muted || vol <= 0.005;
                        }
                    } catch (e) {}
                }
                applyVol(document);
                try {
                    var frames = document.querySelectorAll('iframe');
                    for (var j = 0; j < frames.length; j++) {
                        var win = frames[j].contentWindow;
                        try { applyVol(frames[j].contentDocument); } catch (e) {}
                        if (win) {
                            try { win.postMessage(JSON.stringify({ event: 'command', func: 'setVolume', args: [$volPercent] }), '*'); } catch (e) {}
                            try { win.postMessage(JSON.stringify({ event: 'command', func: muted ? 'mute' : 'unMute', args: [] }), '*'); } catch (e) {}
                            try { win.postMessage({ method: 'setVolume', value: vol, muted: muted }, '*'); } catch (e) {}
                            try { win.postMessage({ type: 'volume', volume: vol, muted: muted }, '*'); } catch (e) {}
                        }
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }

    fun buildProgressProbeScript(): String {
        return """
            (function() {
                if (!window.CinetraPlayerBridge || !window.CinetraPlayerBridge.onPlaybackProgress) return;
                function readVideo(doc) {
                    if (!doc) return false;
                    try {
                        var vids = doc.querySelectorAll('video');
                        for (var i = 0; i < vids.length; i++) {
                            var v = vids[i];
                            if (typeof v.currentTime === 'number' && typeof v.duration === 'number' &&
                                isFinite(v.duration) && v.duration > 0) {
                                window.CinetraPlayerBridge.onPlaybackProgress(Math.floor(v.currentTime), Math.floor(v.duration));
                                return true;
                            }
                        }
                    } catch (e) {}
                    return false;
                }
                if (!readVideo(document)) {
                    try {
                        var frames = document.querySelectorAll('iframe');
                        for (var j = 0; j < frames.length; j++) {
                            if (readVideo(frames[j].contentDocument)) break;
                        }
                    } catch (e) {}
                }
            })();
        """.trimIndent()
    }

    fun buildIframeHtml(streamUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: 0; }
                </style>
                <script>
                    window.addEventListener('message', function(event) {
                        if (!window.CinetraPlayerBridge || !window.CinetraPlayerBridge.onPlaybackProgress) return;
                        try {
                            var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                            if (!data || typeof data !== 'object') return;
                            var payload = data.data || data.info || data;
                            var cur = payload.currentTime !== undefined ? payload.currentTime : payload.seconds;
                            var dur = payload.duration;
                            if (typeof cur === 'number' && typeof dur === 'number' && isFinite(dur) && dur > 0) {
                                window.CinetraPlayerBridge.onPlaybackProgress(Math.floor(cur), Math.floor(dur));
                            }
                        } catch (e) {}
                    });
                </script>
            </head>
            <body>
                <iframe
                    src="$streamUrl"
                    allowfullscreen="true"
                    webkitallowfullscreen="true"
                    mozallowfullscreen="true"
                    allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
                    referrerpolicy="origin">
                </iframe>
            </body>
            </html>
        """.trimIndent()
    }
}
