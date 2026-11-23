package com.example.ui.components

import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale

data class TorrentFileEntry(
    val index: Int,
    val name: String,
    val lengthBytes: Long
)

data class WebTorrentTelemetry(
    val statusText: String = "Initializing WebTorrent client...",
    val peers: Int = 0,
    val downloadSpeedBytes: Long = 0L,
    val uploadSpeedBytes: Long = 0L,
    val progressPercent: Int = 0,
    val activeFileName: String = "",
    val errorMessage: String? = null,
    val isReady: Boolean = false
)

/**
 * WebTorrent P2P streaming helpers and HTML5 player generator aligned with https://webtorrent.io/docs.
 * Supports magnet URIs, .torrent URLs, and 40-char hex / 32-char base32 info hashes inside WebView.
 */
object WebTorrentPolicy {
    const val WEBTORRENT_BASE_URL = "https://webtorrent.io/"
    const val WEBTORRENT_CDN_PRIMARY = "https://cdn.jsdelivr.net/npm/webtorrent@1.9.7/webtorrent.min.js"
    const val WEBTORRENT_CDN_FALLBACK = "https://cdnjs.cloudflare.com/ajax/libs/webtorrent/1.9.7/webtorrent.min.js"

    /**
     * Official Creative Commons Sintel magnet link featured in WebTorrent documentation.
     */
    const val SAMPLE_SINTEL_MAGNET =
        "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel" +
            "&tr=wss%3A%2F%2Ftracker.openwebtorrent.com" +
            "&tr=wss%3A%2F%2Ftracker.btorrent.xyz" +
            "&tr=wss%3A%2F%2Ftracker.fastcast.nz" +
            "&tr=wss%3A%2F%2Ftracker.webtorrent.dev" +
            "&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F" +
            "&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fsintel.torrent"

    val DEFAULT_WSS_TRACKERS = listOf(
        "wss://tracker.openwebtorrent.com",
        "wss://tracker.btorrent.xyz",
        "wss://tracker.fastcast.nz",
        "wss://tracker.webtorrent.dev"
    )

    private val HEX_INFO_HASH_REGEX = Regex("^[a-fA-F0-9]{40}$")
    private val BASE32_INFO_HASH_REGEX = Regex("^[a-zA-Z2-7]{32}$")

    fun isMagnetUri(input: String): Boolean {
        return input.trim().lowercase(Locale.US).startsWith("magnet:?")
    }

    fun isTorrentFileUrl(input: String): Boolean {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.US)
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        val pathOnly = lower.substringBefore('?').substringBefore('#')
        return pathOnly.endsWith(".torrent")
    }

    fun isInfoHash(input: String): Boolean {
        val trimmed = input.trim()
        return HEX_INFO_HASH_REGEX.matches(trimmed) || BASE32_INFO_HASH_REGEX.matches(trimmed)
    }

    fun isWebTorrentSource(input: String): Boolean {
        return isMagnetUri(input) || isTorrentFileUrl(input) || isInfoHash(input)
    }

    fun normalizeTorrentId(input: String, displayName: String? = null): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""

        if (isInfoHash(trimmed)) {
            val encodedName = displayName?.trim()?.takeIf { it.isNotBlank() }?.let {
                "&dn=${URLEncoder.encode(it, "UTF-8")}"
            }.orEmpty()
            val trackerParams = DEFAULT_WSS_TRACKERS.joinToString("") { tracker ->
                "&tr=${URLEncoder.encode(tracker, "UTF-8")}"
            }
            return "magnet:?xt=urn:btih:${trimmed.lowercase(Locale.US)}$encodedName$trackerParams"
        }

        if (isMagnetUri(trimmed)) {
            val lower = trimmed.lowercase(Locale.US)
            val missingWssTrackers = DEFAULT_WSS_TRACKERS.filter { tracker ->
                val encoded = URLEncoder.encode(tracker, "UTF-8").lowercase(Locale.US)
                !lower.contains(tracker.lowercase(Locale.US)) && !lower.contains(encoded)
            }
            if (missingWssTrackers.isEmpty()) return trimmed
            val suffix = missingWssTrackers.joinToString("") { tracker ->
                "&tr=${URLEncoder.encode(tracker, "UTF-8")}"
            }
            return "$trimmed$suffix"
        }

        return trimmed
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        val safe = bytesPerSecond.coerceAtLeast(0L)
        return when {
            safe < 1024L -> "$safe B/s"
            safe < 1024L * 1024L -> String.format(Locale.US, "%.1f KB/s", safe / 1024.0)
            else -> String.format(Locale.US, "%.2f MB/s", safe / (1024.0 * 1024.0))
        }
    }

    fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe < 1024L -> "$safe B"
            safe < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
            safe < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", safe / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", safe / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun parseTorrentFilesJson(json: String): List<TorrentFileEntry> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val idx = obj.optInt("index", -1)
                    val name = obj.optString("name", "").trim()
                    val length = obj.optLong("length", 0L).coerceAtLeast(0L)
                    if (idx >= 0 && name.isNotBlank()) {
                        add(TorrentFileEntry(index = idx, name = name, lengthBytes = length))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun escapeJsString(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    fun buildWebTorrentPlayerHtml(
        torrentId: String,
        title: String = "",
        season: Int = 1,
        episode: Int = 1,
        isTv: Boolean = false
    ): String {
        val normalizedId = normalizeTorrentId(torrentId, title)
        val jsTorrentId = escapeJsString(normalizedId)
        val seasonPad = String.format(Locale.US, "%02d", season.coerceAtLeast(1))
        val episodePad = String.format(Locale.US, "%02d", episode.coerceAtLeast(1))
        val episodeRegexPrimary = "s0?$season[ ._\\-]*e0?$episode\\b|s${seasonPad}e${episodePad}"
        val episodeRegexAlt = "\\b${season}x0?$episode\\b"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background: #000;
                        color: #f8fafc;
                        font-family: system-ui, -apple-system, sans-serif;
                        overflow: hidden;
                    }
                    #player-shell {
                        position: relative;
                        width: 100%;
                        height: 100%;
                        background: #000;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    video {
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                        background: #000;
                        display: block;
                    }
                    #wt-status-pill {
                        position: absolute;
                        top: 10px;
                        left: 10px;
                        max-width: calc(100% - 20px);
                        padding: 6px 10px;
                        border-radius: 999px;
                        background: rgba(12, 14, 22, 0.78);
                        border: 1px solid rgba(255, 255, 255, 0.16);
                        color: #e2e8f0;
                        font-size: 11px;
                        font-weight: 600;
                        pointer-events: none;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        transition: opacity 0.35s ease;
                        z-index: 5;
                    }
                    #wt-status-pill.hidden {
                        opacity: 0;
                    }
                </style>
            </head>
            <body>
                <div id="player-shell">
                    <video id="wt-video" controls autoplay playsinline webkit-playsinline></video>
                    <div id="wt-status-pill">Connecting to WebTorrent P2P swarm...</div>
                </div>

                <script>
                    (function installSafeRtcShim() {
                        var NativeRTC = window.RTCPeerConnection || window.webkitRTCPeerConnection;
                        if (!NativeRTC || NativeRTC.__cinetraPatched) return;

                        var FALLBACK_ICE_SERVERS = [
                            { urls: "stun:stun.l.google.com:19302" },
                            { urls: "stun:stun1.l.google.com:19302" },
                            { urls: "stun:stun.cloudflare.com:3478" }
                        ];

                        function sanitizeConfig(config) {
                            var next = config ? Object.assign({}, config) : {};
                            var servers = Array.isArray(next.iceServers) ? next.iceServers : [];
                            var filtered = servers.filter(function(entry) {
                                if (!entry) return false;
                                var raw = entry.urls || entry.url;
                                if (!raw) return false;
                                var u = Array.isArray(raw) ? raw.join(",") : String(raw);
                                return u.toLowerCase().indexOf("global.stun.twilio.com") === -1;
                            });
                            next.iceServers = filtered.length > 0 ? filtered : FALLBACK_ICE_SERVERS;
                            return next;
                        }

                        function PatchedRTC(config, constraints) {
                            return new NativeRTC(sanitizeConfig(config), constraints);
                        }
                        PatchedRTC.prototype = NativeRTC.prototype;
                        Object.keys(NativeRTC).forEach(function(key) {
                            try { PatchedRTC[key] = NativeRTC[key]; } catch (e) {}
                        });
                        if (typeof NativeRTC.generateCertificate === "function") {
                            PatchedRTC.generateCertificate = NativeRTC.generateCertificate.bind(NativeRTC);
                        }
                        PatchedRTC.__cinetraPatched = true;

                        var origAddIceCandidate = NativeRTC.prototype.addIceCandidate;
                        if (typeof origAddIceCandidate === "function") {
                            NativeRTC.prototype.addIceCandidate = function(candidate) {
                                try {
                                    var candStr = candidate && typeof candidate.candidate === "string"
                                        ? candidate.candidate
                                        : (typeof candidate === "string" ? candidate : "");
                                    if (candStr && /\.local(\s|$)/i.test(candStr)) {
                                        return Promise.resolve();
                                    }
                                } catch (e) {}
                                return origAddIceCandidate.apply(this, arguments);
                            };
                        }

                        var origSetRemoteDescription = NativeRTC.prototype.setRemoteDescription;
                        if (typeof origSetRemoteDescription === "function") {
                            NativeRTC.prototype.setRemoteDescription = function(description) {
                                var args = Array.prototype.slice.call(arguments);
                                try {
                                    if (description && typeof description.sdp === "string" && /\.local(\s|$)/i.test(description.sdp)) {
                                        var cleanedSdp = description.sdp
                                            .split(/\r?\n/)
                                            .filter(function(line) {
                                                return !(/^a=candidate:.*\.local(\s|$)/i.test(line));
                                            })
                                            .join("\r\n");
                                        args[0] = { type: description.type, sdp: cleanedSdp };
                                    }
                                } catch (e) {}
                                return origSetRemoteDescription.apply(this, args);
                            };
                        }

                        window.RTCPeerConnection = PatchedRTC;
                        if (window.webkitRTCPeerConnection) {
                            window.webkitRTCPeerConnection = PatchedRTC;
                        }
                    })();

                    (function() {
                        var TORRENT_ID = "$jsTorrentId";
                        var IS_TV = $isTv;
                        var EP_PRIMARY_RE = new RegExp("$episodeRegexPrimary", "i");
                        var EP_ALT_RE = new RegExp("$episodeRegexAlt", "i");
                        var ANNOUNCE_TRACKERS = [
                            "wss://tracker.openwebtorrent.com",
                            "wss://tracker.btorrent.xyz",
                            "wss://tracker.fastcast.nz",
                            "wss://tracker.webtorrent.dev"
                        ];
                        var RTC_CONFIG = {
                            iceServers: [
                                { urls: "stun:stun.l.google.com:19302" },
                                { urls: "stun:stun1.l.google.com:19302" },
                                { urls: "stun:stun.cloudflare.com:3478" }
                            ]
                        };

                        var statusPill = document.getElementById("wt-status-pill");
                        var videoEl = document.getElementById("wt-video");
                        var client = null;
                        var activeTorrent = null;
                        var activeFileName = "";
                        var telemetryTimer = null;
                        var isReady = false;

                        function emitStatus(text, err) {
                            if (statusPill) {
                                statusPill.textContent = text;
                                if (isReady && !err) {
                                    setTimeout(function() {
                                        if (isReady && statusPill) statusPill.classList.add("hidden");
                                    }, 2500);
                                } else {
                                    statusPill.classList.remove("hidden");
                                }
                            }
                            try {
                                if (window.CinetraWebTorrentBridge && window.CinetraWebTorrentBridge.onTorrentStatus) {
                                    var peers = activeTorrent ? (activeTorrent.numPeers || 0) : 0;
                                    var dl = activeTorrent ? Math.round(activeTorrent.downloadSpeed || 0) : 0;
                                    var ul = activeTorrent ? Math.round(activeTorrent.uploadSpeed || 0) : 0;
                                    var prog = activeTorrent ? Math.round((activeTorrent.progress || 0) * 100) : 0;
                                    window.CinetraWebTorrentBridge.onTorrentStatus(
                                        String(text || ""),
                                        peers,
                                        dl,
                                        ul,
                                        Math.max(0, Math.min(100, prog)),
                                        String(activeFileName || ""),
                                        err ? String(err) : "",
                                        Boolean(isReady)
                                    );
                                }
                            } catch (e) {}
                        }

                        function isPlayableVideo(name) {
                            return /\.(mp4|webm|m4v|mkv|mov)$/i.test(name || "");
                        }

                        function pickBestVideoFile(files) {
                            var playable = [];
                            for (var i = 0; i < files.length; i++) {
                                if (isPlayableVideo(files[i].name)) {
                                    playable.push({ index: i, file: files[i] });
                                }
                            }
                            if (playable.length === 0) return null;

                            if (IS_TV) {
                                for (var j = 0; j < playable.length; j++) {
                                    var n = playable[j].file.name || "";
                                    if (EP_PRIMARY_RE.test(n) || EP_ALT_RE.test(n)) {
                                        return playable[j];
                                    }
                                }
                            }

                            playable.sort(function(a, b) {
                                return (b.file.length || 0) - (a.file.length || 0);
                            });
                            return playable[0];
                        }

                        function publishPlayableFiles(files) {
                            try {
                                if (!window.CinetraWebTorrentBridge || !window.CinetraWebTorrentBridge.onTorrentFiles) return;
                                var list = [];
                                for (var i = 0; i < files.length; i++) {
                                    if (isPlayableVideo(files[i].name)) {
                                        list.push({
                                            index: i,
                                            name: files[i].name,
                                            length: files[i].length || 0
                                        });
                                    }
                                }
                                window.CinetraWebTorrentBridge.onTorrentFiles(JSON.stringify(list));
                            } catch (e) {}
                        }

                        function renderFileByIndex(fileIndex) {
                            if (!activeTorrent || !activeTorrent.files || !activeTorrent.files[fileIndex]) return;
                            var file = activeTorrent.files[fileIndex];
                            activeFileName = file.name || "Stream";
                            for (var i = 0; i < activeTorrent.files.length; i++) {
                                if (i !== fileIndex && activeTorrent.files[i].deselect) {
                                    try { activeTorrent.files[i].deselect(); } catch (e) {}
                                }
                            }
                            try { file.select && file.select(); } catch (e) {}
                            emitStatus("Buffering " + activeFileName + "...", null);
                            try {
                                file.renderTo(videoEl, { autoplay: true, controls: true }, function(err, elem) {
                                    if (err) {
                                        emitStatus("Playback codec error: " + (err.message || err), err.message || String(err));
                                        return;
                                    }
                                    isReady = true;
                                    emitStatus("Streaming P2P • " + activeFileName, null);
                                });
                            } catch (e) {
                                emitStatus("Unable to render video: " + (e.message || e), e.message || String(e));
                            }
                        }

                        window.__wtSelectFile = function(idx) {
                            renderFileByIndex(Number(idx));
                        };

                        window.__wtDestroy = function() {
                            if (telemetryTimer) {
                                clearInterval(telemetryTimer);
                                telemetryTimer = null;
                            }
                            if (client) {
                                try { client.destroy(); } catch (e) {}
                                client = null;
                            }
                            activeTorrent = null;
                        };

                        if (videoEl) {
                            var lastReportedSec = -1;
                            videoEl.addEventListener("timeupdate", function() {
                                if (!window.CinetraPlayerBridge || !window.CinetraPlayerBridge.onPlaybackProgress) return;
                                var cur = Math.floor(videoEl.currentTime || 0);
                                var dur = Math.floor(videoEl.duration || 0);
                                if (dur > 0 && cur !== lastReportedSec && cur % 3 === 0) {
                                    lastReportedSec = cur;
                                    window.CinetraPlayerBridge.onPlaybackProgress(cur, dur);
                                }
                            });
                            videoEl.addEventListener("playing", function() {
                                isReady = true;
                                emitStatus("Playing • " + activeFileName, null);
                            });
                        }

                        function startWebTorrent() {
                            if (typeof window.WebTorrent !== "function") {
                                emitStatus("Failed to load WebTorrent engine.", "WebTorrent library unavailable");
                                return;
                            }
                            try {
                                client = new window.WebTorrent({
                                    rtcConfig: RTC_CONFIG,
                                    tracker: {
                                        rtcConfig: RTC_CONFIG
                                    }
                                });
                            } catch (e) {
                                emitStatus("WebRTC not supported in this WebView.", e.message || String(e));
                                return;
                            }

                            client.on("error", function(err) {
                                emitStatus("WebTorrent error: " + (err.message || err), err.message || String(err));
                            });

                            emitStatus("Searching WebRTC peers for metadata...", null);
                            activeTorrent = client.add(TORRENT_ID, { announce: ANNOUNCE_TRACKERS }, function(torrent) {
                                activeTorrent = torrent;
                                publishPlayableFiles(torrent.files || []);
                                var chosen = pickBestVideoFile(torrent.files || []);
                                if (!chosen) {
                                    emitStatus("No playable video (.mp4/.webm/.mkv) found in torrent.", "No compatible video file found");
                                    return;
                                }
                                renderFileByIndex(chosen.index);

                                torrent.on("done", function() {
                                    isReady = true;
                                    emitStatus("100% Cached • Seeding P2P", null);
                                });
                                torrent.on("error", function(err) {
                                    emitStatus("Torrent error: " + (err.message || err), err.message || String(err));
                                });
                            });

                            telemetryTimer = setInterval(function() {
                                if (!activeTorrent) return;
                                var peers = activeTorrent.numPeers || 0;
                                var pct = Math.round((activeTorrent.progress || 0) * 100);
                                var status = isReady
                                    ? ("Streaming P2P • " + pct + "% • " + peers + " peers")
                                    : (peers > 0 ? ("Fetching metadata/chunks (" + peers + " peers)...") : "Discovering WebRTC peers...");
                                emitStatus(status, null);
                            }, 800);
                        }

                        function loadScript(src, onOk, onErr) {
                            var s = document.createElement("script");
                            s.src = src;
                            s.async = true;
                            s.onload = onOk;
                            s.onerror = onErr;
                            document.head.appendChild(s);
                        }

                        loadScript(
                            "$WEBTORRENT_CDN_PRIMARY",
                            startWebTorrent,
                            function() {
                                loadScript(
                                    "$WEBTORRENT_CDN_FALLBACK",
                                    startWebTorrent,
                                    function() {
                                        emitStatus("Could not load WebTorrent JS from CDN.", "Network unreachable or CDN blocked");
                                    }
                                );
                            }
                        );
                    })();
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
