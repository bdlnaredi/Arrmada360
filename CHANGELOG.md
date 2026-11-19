# Changelog

All notable changes to Arrmada360 are listed here, newest first.
Every successful build on `main` is published as **`Arrmada360.apk`** in the [latest release](../../releases/latest).
The app version is `1.0.<build number>`. The build number is shown in *Settings* and in the release title.

Builds #2, #5, #10, #15, #18 and #19 failed to compile in CI and were fixed straight away in builds #3, #6, #11, #16 and #20, so there is no APK for them.

---

## [1.0.23] – 2026-11-19

### Fixed
- **Namer search returned no results.** The app was "cleaning" the file name (dots to spaces, quality tags removed) before sending it to Namer, which breaks ThePornDB's file-name parsing.
  - It now searches with the **original file name** first, then automatically retries with a cleaned version.
  - Errors are no longer hidden: if Namer rejects the request or replies unexpectedly, you see the message. "No matches" shows what was searched and **Namer's raw reply**.

### Added
- **Match by fingerprint** (phash) in Namer's Match & rename. It works even when the file name is meaningless.

## [1.0.22] – 2026-11-19

### Changed
- **Upcoming now comes from your library.**
  - **Movies tab → Upcoming movies:** monitored Radarr movies releasing in the next 90 days.
  - **TV Shows tab → Upcoming episodes:** monitored Sonarr episodes airing in the next 90 days, each with show poster, episode number and date. Tap one to open the show.
- **New "Releasing soon" row:** new movies and shows coming out soon from Trakt, TMDB or Seerr (what the Upcoming row used to show).

## [1.0.21] – 2026-11-19

### Fixed
- **Upcoming shows** on the TV Shows tab were empty. The TMDB list used each show's *first* air date, so everything looked old and was filtered out, and the empty result never fell back to another source. Each source is now filtered on its own, TV uses TMDB's "premiering from today" search, Trakt includes new seasons, and Seerr falls back to a date-filtered discover list.

### Added
- **Namer** (ThePornDB renamer) under a new **Tools** category:
  - **Failed** files: size, best match %, *Match & rename* (search Any, Scenes, Movies or JAV, pick a result, confirm), and *Show why it failed*.
  - Delete a file, if Namer allows deleting.
  - **Queue** with size.
  - **Server** tab, with an artwork switch. Poster art is **hidden by default**.

## [1.0.20] – 2026-11-19

### Added
- **Local ⇄ remote connection switching** (like nzb360):
  - Every app now has a **🏠 local address** and an optional **🌍 remote address** (Tailscale, domain or reverse proxy). *Test* checks both.
  - **Automatic** mode: local on your **home Wi-Fi networks**, remote on any other Wi-Fi or mobile data. If you haven't added home networks, or Android hides the Wi-Fi name, the app quickly tests each local address on Wi-Fi and uses it when it answers.
  - **Always local** and **Always remote** modes.
  - It switches as soon as your network changes, and re-checks every connection.
  - **Settings → Connection switching**: current status, mode, home Wi-Fi list (*Add current Wi-Fi network*), and which address every app is using right now.
  - 🏠 / 🌍 indicator in the side menu. Background notifications use the same switching.

## [1.0.17] – 2026-11-19

### Added
- **In-app updates**: Arrmada360 checks this repo's latest build, shows what changed since your version, then downloads and installs the new APK in one tap with a progress bar.
  - Automatic check when the app opens (at most every 6 hours), with *Update now*, *Later* and *Skip this build*.
  - A background notification when a new build is out, if notifications are on.
  - **Settings → Updates & changelog**: current and latest build, download & install, full **changelog viewer**, and a link to the GitHub release.
  - *Check for updates automatically* switch in General settings and on the Updates page.
  - `arrmada360://updates` deep link.
- **What's New** after an update now shows the real changelog for the builds you just got.

## [1.0.16] – 2026-11-19

### Changed
- **Poster tags are readable on any artwork**: white bold text on a dark glass background, with a subtle shadow.
- **Status shown as icons instead of words** on posters: ✓ available or in your library (green), half circle for partially available (blue), download arrow for processing (amber), clock for requested (purple), and a block sign for declined (red). A small key sits above the Movies, TV and Seerr Discover grids.
- All coloured tags across the app now use white text on a stronger coloured background.

## [1.0.14] – 2026-11-19

### Added
- **RSS multi-select**: long-press an article or tap the checklist button. Then select all, download all, mark read, or download them all into a category.
- **Download RSS articles to a category** (qBittorrent) or label (ruTorrent): pick an existing one or create a new one, with an optional save path on qBittorrent. The category button sits on every article row and in the article details.

### Docs
- README now shows the official app logos (linked from dashboard-icons). The banner no longer uses initials.

## [1.0.12] – 2026-11-19

### Added
- **Multi-select for torrent clients**: long-press or *Select multiple*, with tick circles on every row.
  - Select all shown (respects filters), invert selection, select none.
  - Bulk **resume**, **pause**, **delete** (with or without files) and **set category**.
  - Bulk **force start** and **force recheck** (qBittorrent).
- **Categories (qBittorrent) and labels (ruTorrent)**:
  - set, change, create (with a save path on qBittorrent) and remove
  - *Change category* on a single torrent
  - *Manage categories* in the ⋮ menu
  - category filter chips with counts

## [1.0.11] – 2026-11-19

### Added
- **nzb360-style Server tab** on the dashboard:
  - **Server Info**: disk space from all your *arr apps, a 2-week **download history** bar chart, and *arr **health issues** ("No Server Issues" when everything is fine)
  - **Unraid** summary card
  - **Live downloads**
  - **Other Services**: recently downloaded **books** (Readarr) and **albums** (Lidarr), with "Enable … to Populate" and *Go to Settings* when the app isn't added
  - **External Server Use**: Tautulli active streams and Seerr pending requests
  - **Your apps** tiles with live online status
- **Full Unraid controls** (Dashboard · Array · Docker · VMs · Shares):
  - Dashboard: CPU, RAM and array gauges, disk warnings, unread notifications with dismiss, **Reboot** and **Shut down**
  - Array: **start/stop array**, **parity check** (check, check & correct, pause, resume, cancel) with progress, and per-disk temperature, errors, spin state, filesystem and usage
  - Docker: search, filters, start, stop, restart, pause, unpause, **Start all** and **Stop all**
  - VMs: start, stop, pause, resume, reboot, force stop
  - Shares: used and free space, cache setting
  - Confirmation dialogs for every disruptive action

### Changed
- **Removed the Home tab.** The dashboard is now Movies · TV Shows · Calendar · Server, and opens from *Dashboard* in the side menu.
- New `arrmada360://server` deep link.

## [1.0.9] – 2026-11-19

### Added
- **RSS for qBittorrent and ruTorrent**:
  - **Articles** from every feed: search, filter by feed, unread only, one-tap download, send to another client, description, open page, mark as read
  - **Feeds**: add, refresh, remove, error status
  - **Auto-download rules** (qBittorrent): must contain / must not contain, regex, episode filter, category, save path, feeds, enable/disable, **match preview**
  - qBittorrent **RSS reader** and **auto-download** switches

## [1.0.8] – 2026-11-19

### Added
- **Settings rebuilt in nzb360's style**, with grouped cards.
- **General settings**:
  - volume rocker navigation
  - gesture settings (side-menu swipe, download swipe actions, back to dashboard)
  - friendly service names
  - speeds as bits
  - auto-clear library search
  - What's New on update
  - open IMDb links with the browser or the IMDb app
  - **require fingerprint or PIN for settings**
  - local crash logs (view and share)
  - clear image cache
  - clear cached app data
- **Backup & restore**: create a `.zip` backup, restore from a file, copy or paste as text.
- **Notifications**: background checker (about every 15 minutes) for new Radarr movies, Sonarr episodes, Lidarr music, Readarr books, Seerr requests and finished downloads, plus *Check now*.
- **Deep links**: `arrmada360://dashboard`, `movies`, `tv`, `calendar`, `search`, `health`, `settings`, `service/<name>`.
- **Open on IMDb** button on movie and show pages.
- *Appearance* and *Discovery sources* pages.

## [1.0.7] – 2026-11-19

### Added
- **Movies and TV Shows discovery tabs**:
  - Trending hero carousel
  - Recently downloaded
  - **Recommended for you** (based on your library)
  - Most anticipated
  - Upcoming
  - Popular
  - **Popular people**
- Data comes from **Trakt → TMDB → Overseerr/Jellyseerr**, whichever is set up. Seerr works without a key.
- Title sheet with **Add to Radarr/Sonarr**, **Request with Seerr** (including 4K) and **Open on TMDB**, with in-library and requested badges.
- TMDB API key and Trakt Client ID in Settings.

## [1.0.6] – 2026-11-19

### Added
- **Bottom tab bars everywhere**:
  - Dashboard: Home, Movies, TV Shows, Calendar, Server.
  - Sonarr / Radarr / Lidarr / Readarr: **Library** (with Calendar and Missing), **Queue**, **Add**, **History**, **Server**.
  - Prowlarr: Search, Indexers, **Stats**, **History**, Server. Jackett: Search, Server.
  - Overseerr/Jellyseerr: Requests, Discover, **Users**, Server (version, **run jobs**).
  - Tautulli: Activity, History, Stats, **Users**, Server (Plex info, **libraries**).
  - Unraid: Overview, Docker, VMs, Server. Download clients: Torrents or Queue, History, Server.
- **\*arr Server tab**:
  - instance settings and View on web
  - **Manual import** (from a folder or a queued download)
  - RSS sync, Update library, Search all missing, Refresh downloads
  - **System status** page: health, disk space, scheduled tasks with *Run now*, and restart

## [1.0.4] – 2026-11-19

### Fixed
- Text was black on the dark background. All text now defaults to the light theme colour.

### Changed
- Movie and show titles from Sonarr, Radarr, Seerr, Tautulli and search are now **bold white**.

## [1.0.3] – 2026-11-19

### Added
- **Official logo for every app**, loaded from dashboard-icons, with initials shown while offline. Jellyseerr gets its own logo.
- **Colourful, animated redesign**:
  - background glow tinted to each app's colours
  - pill tabs and gradient buttons
  - cards that spring when pressed
  - animated progress bars and counters
  - shimmer placeholders while loading
  - live speed charts and ring gauges
  - colour filter chips with counts
- New dashboard: gradient hero card with live total speed, app tiles with live status, poster row of upcoming releases, live download cards.
- **Swipe actions** on downloads: right to pause or resume, left to delete.
- **Connection check** screen that tests every app and explains failures in plain English.
- Animated welcome screen with bouncing app logos.

## [1.0.1] – 2026-11-19

### Added
- First release of Arrmada360.
- First-run flow: pick the apps you use, then set up each one with a connection test.
- Side menu grouped by category, and a dashboard with download speeds, upcoming releases, requests, streams and Unraid status.
- **Sonarr, Radarr, Lidarr, Readarr**: library, details, season/episode/album/book monitoring, automatic and **interactive search**, add new titles, calendar, queue, history, wanted, disk space, RSS sync, delete.
- **Prowlarr and Jackett**: search all indexers and send results to any download client. Prowlarr: indexer list, test, enable.
- **qBittorrent, Transmission, Deluge, ruTorrent, SABnzbd, NZBGet**: live list, filters, sort, pause/resume (one, many or all), delete with or without data, add by link, multi-select, speed limits, Usenet history.
- **Overseerr/Jellyseerr**: approve, decline, retry and delete requests; discover, search and request.
- **Tautulli**: live streams, stop a stream, history, stats.
- **Unraid (7.2+)**: system info, array and disks, Docker start/stop, VMs.
- Universal search, magnet-link sharing into the app, multiple instances of the same app, self-signed HTTPS, AMOLED mode, settings export and import.
- GitHub Actions workflow that builds a signed APK and publishes it to the `latest` release.
