package com.example.data.model

object AdGatePolicy {
    const val DEFAULT_STAY_SECONDS = 30
    const val DEFAULT_COOLDOWN_HOURS = 6
    private const val MILLIS_PER_HOUR = 3_600_000L

    fun isConfiguredAndEnabled(config: AdConfigEntity?): Boolean {
        if (config == null || !config.isEnabled) return false
        return config.adUrl.isNotBlank() || config.popUnderScript.isNotBlank()
    }

    fun cooldownMillis(config: AdConfigEntity): Long {
        return config.cooldownHours.coerceAtLeast(1) * MILLIS_PER_HOUR
    }

    fun shouldShowAdGate(
        config: AdConfigEntity?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isConfiguredAndEnabled(config) || config == null) return false
        if (config.lastCompletedAtMillis <= 0L) return true
        return (nowMillis - config.lastCompletedAtMillis) >= cooldownMillis(config)
    }

    fun remainingCooldownMillis(
        config: AdConfigEntity?,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        if (config == null || config.lastCompletedAtMillis <= 0L) return 0L
        val elapsed = (nowMillis - config.lastCompletedAtMillis).coerceAtLeast(0L)
        return (cooldownMillis(config) - elapsed).coerceAtLeast(0L)
    }

    fun formatRemainingCooldown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "Due now"
        val totalMinutes = (remainingMillis / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) {
            "${hours}h ${minutes}m remaining"
        } else {
            "${minutes}m remaining"
        }
    }

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "https://$trimmed"
        }
    }

    fun buildAdsterraPopUnderHtml(
        popUnderScript: String,
        adUrl: String
    ): String {
        val normalizedUrl = normalizeUrl(adUrl)
        val trimmedScript = popUnderScript.trim()
        val scriptBlock = when {
            trimmedScript.isBlank() -> ""
            trimmedScript.contains("<script", ignoreCase = true) -> {
                trimmedScript
                    .replace("src=\"//", "src=\"https://")
                    .replace("src='//", "src='https://")
            }
            else -> {
                val scriptSrc = normalizeUrl(trimmedScript)
                "<script type=\"text/javascript\" src=\"$scriptSrc\"></script>"
            }
        }

        val fallbackLinkBlock = if (normalizedUrl.isNotBlank()) {
            """
            <a class="cta-btn" href="$normalizedUrl">Continue to Sponsor Page</a>
            <script>
              setTimeout(function() {
                try {
                  if (!window.__cinetraRedirected) {
                    window.__cinetraRedirected = true;
                    window.location.href = "$normalizedUrl";
                  }
                } catch (e) {}
              }, 1200);
            </script>
            """.trimIndent()
        } else {
            ""
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  background: #09090C;
                  color: #F5F5F7;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  min-height: 100vh;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  padding: 24px;
                  text-align: center;
                }
                .card {
                  background: #1E1E28;
                  border: 1px solid #2E2E3E;
                  border-radius: 16px;
                  padding: 24px;
                  max-width: 420px;
                  width: 100%;
                }
                .badge {
                  display: inline-block;
                  background: #3B0B0E;
                  color: #FFC107;
                  border: 1px solid #E50914;
                  border-radius: 999px;
                  font-size: 11px;
                  font-weight: 700;
                  padding: 4px 12px;
                  margin-bottom: 14px;
                }
                h2 { font-size: 18px; margin-bottom: 8px; }
                p { font-size: 13px; color: #A1A1AA; line-height: 1.5; margin-bottom: 18px; }
                .cta-btn {
                  display: inline-block;
                  background: #E50914;
                  color: #FFFFFF;
                  text-decoration: none;
                  font-weight: 700;
                  font-size: 14px;
                  padding: 12px 20px;
                  border-radius: 10px;
                }
              </style>
              $scriptBlock
            </head>
            <body>
              <div class="card">
                <div class="badge">ADSTERRA SPONSORED PAGE</div>
                <h2>Loading Sponsor Experience...</h2>
                <p>Please keep this page open until the countdown reaches 0 to unlock Cinetra.</p>
                $fallbackLinkBlock
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
