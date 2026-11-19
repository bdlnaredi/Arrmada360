# Arrmada360

A free, open-source Android app to control your whole media server from your phone, in the spirit of nzb360. **Every feature is unlocked. No subscriptions.**

## Supported apps

| Category | Apps | What you can do |
|---|---|---|
| Media managers | Sonarr, Radarr, Lidarr, Readarr | Library poster grid with filters and sorting, details pages, season/episode/album/book lists, monitor or unmonitor anything, automatic and interactive (manual) search with grab, add new shows/movies/artists/authors with profile, root folder and monitor options, calendar, queue (with remove and blocklist), history, wanted/missing, RSS sync, disk space, delete with files |
| Indexers | Prowlarr, Jackett | Search every indexer with category filters and sorting, send results to any download client (or Prowlarr's own), copy magnet links, open details. Prowlarr: list, test, enable and disable indexers |
| Download clients | qBittorrent, Transmission, Deluge, ruTorrent, SABnzbd, NZBGet | Live list with speeds, filters and sorting, pause/resume (one, many or all), delete with or without data, add magnet/URL, multi-select with long press. qBittorrent: force start, recheck, alternative speed toggle. SABnzbd: speed limits. Usenet history |
| Requests | Overseerr / Jellyseerr | Approve, decline, retry or delete requests. Discover trending, popular and upcoming titles, search, and request (including 4K) |
| Monitoring | Tautulli | Live streams with progress, transcode info and bandwidth, stop a stream with a message, play history, 30-day stats |
| Server | Unraid 7.2+ | System, CPU and RAM, array capacity, disks with temperatures, Docker containers (start, stop, restart) and VMs |

You also get:

- a dashboard with live download speeds, upcoming releases, pending requests, current streams and Unraid status
- search across all of your libraries at once
- share a magnet link to Arrmada360 from any app
- AMOLED black mode and adjustable poster size
- multiple instances of the same app (for example two Radarrs)
- settings backup and restore

## Install

Download **Arrmada360.apk** from the [latest release](../../releases/latest) on your phone and open it. Android will ask you to allow installs from your browser the first time.

Each push to `main` builds a new APK automatically through GitHub Actions.

## Setup

When you first open the app, pick the apps you use, then enter each one's address (for example `http://192.168.1.10:8989`) and its API key or login. Tap **Test** to check the connection.

The Unraid API needs Unraid 7.2 or newer. Create a key under **Settings → Management Access → API Keys**.

## Build it yourself

Open the folder in Android Studio and press Run, or run `./gradlew assembleRelease`.

## License

MIT
