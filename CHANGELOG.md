# Changelog

All notable changes to Arrmada360 are listed here, newest first.
Every successful build on `main` is published as **`Arrmada360.apk`** in the [latest release](../../releases/latest).
The app version is `1.0.<build number>`. The build number is shown in *Settings* and in the release title.

Builds #2, #5, #10, #15, #18, #19, #29, #33, #35, #38 and #44 failed to compile in CI and were fixed straight away in builds #3, #6, #11, #16, #20, #30, #34, #36 and #39, so there is no APK for them.

---

## [1.0.44] – build #44

### Added
- **New first-run experience:**
  1. **Welcome:** a floating logo wallpaper and every supported app.
  2. **Which of these do you use?** Pick your apps from a grid, with select all or clear all.
  3. **Getting things set up:** a short animated setup with a little about Arrmada360.
  4. **You're all set:** connect each chosen app right there, or explore first.
- **Welcome card on the dashboard:** an animated hand shows the gestures. Swipe right on the card for the menu, or left for downloads, or tap either circle.
- **Finish connecting your apps:** a dashboard card for apps you picked but haven't connected yet.
- **Tips carousel:** 8 tips (search, sharing, long-press, people, the Server tab, home and away, widgets, notifications) with Next and Hide.
- **One-time screen tips** on the library, movie and show pages, downloads, Unraid and Stasharr.
- **Downloads button** in the dashboard's top bar.
- **Help & guides** (*Settings → Help*): replay the welcome tour, show all tips again, a "getting around" guide, and a per-app guide to the address and API key, with an Add button.

## [1.0.42] – build #42

### Added
- **Cast & crew pages:** tap anyone in a movie or show's cast or crew to see their photo, biography, birthday and birthplace, and their full filmography.
  - Filter by movies or TV, acting or crew, popular, newest or oldest, and titles not in your library.
  - Titles you own show a library badge and open straight to their page. Everything else has a + to add it to Radarr or Sonarr.
- **Cast for TV shows:** Sonarr shows now have a cast row, from TMDB or Seerr.
- "Popular people" on the Movies and TV tabs opens the same person page.
- **Stasharr scene page:** tap a scene to see:
  - its screenshot, title, studio, date and code
  - quality, size and play count
  - a 1–5 star rating you can set
  - the synopsis, performers with photos and age in the scene, the studio logo, tags, and file details
  - Open, Organize and Delete buttons

## [1.0.40] – build #40

### Added
- **Trailers:** a Trailer button on every movie and show page, which opens YouTube.
- **Where to watch:** streaming, free, rent and buy services for your country, via TMDB and JustWatch. You can set the country in *Discovery sources*.
- **Radarr collections:** see every franchise you own, how complete it is, and add the missing movies one at a time or all at once.
- **Quality & duplicates** (Server tab of Radarr and Sonarr):
  - items below your quality cutoff
  - low-quality movies (SD, 720p or 1080p and below)
  - movies with no subtitles
  - titles duplicated across instances
  - select and search for upgrades in bulk
- **Speed scheduler:** limited download and upload speeds between set times, full speed the rest of the day. Works with qBittorrent, Transmission, Deluge, SABnzbd and NZBGet.
- **Seeding manager:**
  - finished torrents with their ratio and seeding time
  - rules by ratio and days, filtered by category
  - bulk removal, optionally with the files
  - optional automatic clean-up with a notification
- **Unraid live graphs:** CPU, RAM and per-thread charts every 3 seconds, plus a 24-hour history the app samples in the background.
- **Unraid logs:** a viewer for every Unraid log file and for each container's own log, with search, an errors-only filter and sharing.
- **Daily summary:** one morning notification listing what downloaded, what's airing today, health warnings, Kuma monitors that are down, and low disk space.
- **Voice search:** a microphone in the dashboard search bar. Saying "add Dune Part Two" searches for it.
- **Family mode:** a read-only profile with only the apps you choose, protected by a PIN. Adding, editing and deleting are hidden, and Settings is locked.
- **What is this?** Share a video clip or screenshot from WhatsApp, Telegram or your gallery to Arrmada360, or tap the clip icon in the search bar.
  - Gemini (free key) recognises movies and TV; trace.moe recognises anime.
  - Shows the title, year, season and episode with a confidence score, and lets you view or add it.
- **IMDb and TMDB links** open in Arrmada360 when you share them or open them with the app.

## [1.0.34] – build #34

### Added
- **New apps:** Whisparr (v2 and v3), Uptime Kuma and Stasharr / Stash. Whisparr and Stash artwork is hidden until you turn on **Show artwork** in the Server tab.
- **Radarr and Sonarr detail pages:** ratings, runtime, age rating and studio; a movie file card and cast row; per-item history; an **Edit** sheet for quality profile, availability, series type and tags.
- **Interactive search everywhere:**
  - whole Sonarr seasons, episodes, movies, and the Missing and Calendar lists
  - sort and filter results by protocol, quality or text
  - asks before grabbing a rejected release
- **Bulk library actions:** long-press to select, then monitor, unmonitor, search, refresh, change quality, add or remove tags, or delete.
- **Home-screen widgets:** Downloads, Coming up and Shortcuts.
- **Quick Settings tile** to pause or resume every download client.
- **Richer notifications:** poster and backdrop images, **Approve / Decline** for Seerr requests, and **Open** buttons.
- **App lock** with fingerprint, face or screen lock, and a choice of how soon it locks again.
- **Year in review** (*Dashboard → Server*).
- **Light theme**, **follow-phone theme** and **Material You** colours.
- **Tablet and Android TV layout:**
  - the side menu always shows on wide screens
  - a two-pane library and detail view
  - focus highlights for TV remotes
  - an Android TV launcher banner
- **Offline cache:** last-known data with an "Offline" banner when a server is unreachable. You can turn it off in *Settings → General*.

### Changed
- **Tautulli redesigned:**
  - artwork-backed stream cards with expandable details
  - history grouped by day with a user filter
  - stats with a plays chart, poster rows and rankings
  - ranked users

## [1.0.28] – build #28

### Changed
- **New Settings home**, laid out like nzb360's but in Arrmada360's style:
  - Large title with a **What's New** pill (version, and a dot when an update is ready).
  - **App settings** card: General, Notifications, Backup & restore, Appearance, Discovery sources, Updates.
  - **Your server** card: apps online with a *Test* button, Connection check and Connection switching.
  - **Services**: a **startup screen** picker, then a two-column grid of your apps with a dashed **Add a service** tile. Tap to edit, long-press to reorder or remove.
  - **Indexers** grid with **Add an indexer**.
  - **Help**: help center (README), logging center (crash log), send feedback (GitHub issues), plus GitHub, Changelog and privacy links.

### Added
- **Startup screen**: choose whether the app opens on the Dashboard or straight into any app.
- **Cleaner side menu**:
  - Logo and wordmark at the top, with a thin rule and an edit button that opens your apps in Settings.
  - One flat list: Dashboard, then each app with its own logo. No category headers, tiles or status dots.
  - The current screen has a soft highlight and an accent bar.
  - A small red dot shows only when an app is offline.
  - Footer: a Settings button and a Connection card (🏠 Local / 🌍 Remote, apps online) that opens Connection switching.
- **Dashboard search bar**: the search button is now a search bar in the top bar ("Search movies, TV…") that **searches automatically as you type**. Results show matches already in your libraries first, then new titles you can add.
- **Status marks now say whether a title is actually downloaded**:
  - ✓ green: **downloaded** (on your server)
  - ◐ blue: **partly downloaded** (e.g. some episodes)
  - ⌛ orange: **in your library but not downloaded yet**
  - Seerr's ⬇ processing and 🕒 requested icons are unchanged.
  - The legend and the title sheet ("Open in Radarr") show the same states.
- **Removed** the connection-check button from the dashboard, and *Search everything* and *Connection check* from the side menu. **Connection check** now lives in **Settings**.

## [1.0.24] – 2026-11-19

### Changed
- **Namer artwork switch is easy to find.** *Show ThePornDB artwork* is now a real on/off switch at the top of the **Failed** list, inside the **Match & rename** sheet (it takes effect immediately) and in the **Server** tab. It is still off by default.

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
