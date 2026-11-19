package com.arrmada360.app.data

import androidx.compose.ui.graphics.Color

enum class AuthKind { API_KEY, USER_PASS, PASSWORD_ONLY }

enum class Category(val title: String) {
    MEDIA("Media managers"),
    INDEXERS("Indexers"),
    DOWNLOADS("Download clients"),
    REQUESTS("Requests"),
    MONITORING("Monitoring"),
    SERVER("Server"),
}

enum class ServiceType(
    val label: String,
    val short: String,
    val color: Long,
    val defaultPort: Int,
    val auth: AuthKind,
    val category: Category,
    val blurb: String,
    val keyHint: String,
) {
    SONARR("Sonarr", "So", 0xFF35C5F4, 8989, AuthKind.API_KEY, Category.MEDIA, "TV shows",
        "Sonarr → Settings → General → API Key"),
    RADARR("Radarr", "Ra", 0xFFFFC230, 7878, AuthKind.API_KEY, Category.MEDIA, "Movies",
        "Radarr → Settings → General → API Key"),
    LIDARR("Lidarr", "Li", 0xFF22C55E, 8686, AuthKind.API_KEY, Category.MEDIA, "Music",
        "Lidarr → Settings → General → API Key"),
    READARR("Readarr", "Re", 0xFFE0464B, 8787, AuthKind.API_KEY, Category.MEDIA, "Books & audiobooks",
        "Readarr → Settings → General → API Key"),
    PROWLARR("Prowlarr", "Pr", 0xFFF97316, 9696, AuthKind.API_KEY, Category.INDEXERS, "Indexer manager & search",
        "Prowlarr → Settings → General → API Key"),
    JACKETT("Jackett", "Ja", 0xFF94A3B8, 9117, AuthKind.API_KEY, Category.INDEXERS, "Torrent indexer search",
        "Jackett dashboard → API Key (top right)"),
    QBITTORRENT("qBittorrent", "qB", 0xFF3B82F6, 8080, AuthKind.USER_PASS, Category.DOWNLOADS, "Torrent client",
        "Your qBittorrent Web UI username and password"),
    TRANSMISSION("Transmission", "Tr", 0xFFDC2626, 9091, AuthKind.USER_PASS, Category.DOWNLOADS, "Torrent client",
        "Leave blank if RPC authentication is off"),
    DELUGE("Deluge", "De", 0xFF5B8DEF, 8112, AuthKind.PASSWORD_ONLY, Category.DOWNLOADS, "Torrent client",
        "Deluge Web UI password (default: deluge)"),
    RUTORRENT("ruTorrent", "rT", 0xFF84CC16, 80, AuthKind.USER_PASS, Category.DOWNLOADS, "rTorrent web client",
        "Basic-auth login if your ruTorrent uses one. Needs the httprpc plugin."),
    SABNZBD("SABnzbd", "SB", 0xFFEAB308, 8080, AuthKind.API_KEY, Category.DOWNLOADS, "Usenet client",
        "SABnzbd → Config → General → API Key"),
    NZBGET("NZBGet", "NG", 0xFF10B981, 6789, AuthKind.USER_PASS, Category.DOWNLOADS, "Usenet client",
        "NZBGet → Settings → Security (ControlUsername / ControlPassword)"),
    SEERR("Overseerr / Jellyseerr", "Se", 0xFF818CF8, 5055, AuthKind.API_KEY, Category.REQUESTS, "Media requests",
        "Seerr → Settings → General → API Key"),
    TAUTULLI("Tautulli", "Ta", 0xFFE5A00D, 8181, AuthKind.API_KEY, Category.MONITORING, "Plex activity & stats",
        "Tautulli → Settings → Web Interface → API Key"),
    UNRAID("Unraid", "Un", 0xFFF15A2C, 80, AuthKind.API_KEY, Category.SERVER, "Array, Docker & VMs",
        "Unraid 7.2+: Settings → Management Access → API Keys → create a key (Admin role)");

    val tint: Color get() = Color(color)
    val isArr: Boolean get() = this == SONARR || this == RADARR || this == LIDARR || this == READARR
    val isDownloader: Boolean get() = category == Category.DOWNLOADS
    val isIndexer: Boolean get() = category == Category.INDEXERS
}
