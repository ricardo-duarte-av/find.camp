# find.camp HTTP API

Reference for the find.camp REST API as it exists today, written for an
Android client. **41 endpoints** under `/api/v1/*` plus the NextAuth
catch-all at `/api/auth/*`.

- [Conventions](#conventions)
- [Authentication & Android integration notes](#authentication--android-integration-notes)
- [Endpoints](#endpoints)
  - [Auth](#auth)
  - [Spots](#spots)
  - [Reviews](#reviews)
  - [Saves (favourites)](#saves-favourites)
  - [Contributions & events](#contributions--events)
  - [Trips](#trips)
  - [Uploads](#uploads)
  - [Admin: spots](#admin-spots)
  - [Admin: contributions](#admin-contributions)
  - [Admin: reviews](#admin-reviews)
  - [Admin: types](#admin-types)
  - [Admin: amenities](#admin-amenities)
  - [Admin: users](#admin-users)
  - [Admin: AI](#admin-ai)
- [Counts](#counts)

---

## Conventions

- **Base URL** — same origin as the website (`https://find.camp` in
  prod). All paths in this doc are relative to that.
- **Content type** — request and response bodies are JSON
  (`application/json`) except for `POST /api/v1/uploads` (multipart) and
  `POST /api/v1/spots/:id/events` (accepts JSON or `text/plain` for
  `navigator.sendBeacon`).
- **Auth model** — Auth.js v5 (NextAuth) **database sessions** stored in
  Postgres. Authorisation is carried by the `next-auth.session-token`
  cookie set by the sign-in flow. There is no bearer-token alternative
  today — see the next section for what this means on Android.
- **Body limits** — JSON routes cap at **64 KB**, multipart upload caps
  at **10 MB** (one ~8 MB image plus form overhead). Over-limit
  requests return `413` immediately, before parsing.
- **Rate limiting** — in-memory sliding-window, per user (or per IP for
  `events`). Exceeded → `429` with a `Retry-After` header (seconds) and
  body `{ "error": "rate_limited" }`.
- **Error shape** — `{ "error": "<code>", ...optional fields }`. Common
  codes: `invalid` (with zod `issues[]`), `not_found`, `forbidden`,
  `not_authenticated`, `onboarding_required`, `bad_id`, `rate_limited`,
  `slug_taken`, `key_taken`, `in_use`, `internal`.
- **IDs** — spots, trips, reviews, contributions, photos and users are
  UUID v4. URLs use the **id**, not the slug, even when the public
  detail page route uses the slug.
- **Pagination** — only `GET /api/v1/spots/:id/reviews` paginates.
  Cursor-based: pass `?cursor=<value>` from the previous page's
  `nextCursor`. Other list endpoints return everything (capped) in one
  shot.

## Authentication & Android integration notes

### What's there today

Auth.js v5 mounts at `/api/auth/*` (catch-all handler). The only
configured provider is **Nodemailer magic-link** — the user enters
their email, receives a sign-in link, clicks it, and the server sets a
`__Secure-next-auth.session-token` cookie pointing at a row in the
Postgres `sessions` table. Database sessions mean revocation is
instant (delete the row), but it also means **every authenticated API
call on the website carries that cookie automatically** — there is no
bearer/JWT path.

Onboarding gate: `requireUser()` rejects accounts whose `onboardedAt`
is null. Fresh sign-ups must complete `/onboarding` (display name +
slug) before any mutation will succeed. On the API surface this
returns `403 { "error": "onboarding_required" }`.

### Why this is awkward for Android

Native Android doesn't share a cookie jar with the email client, so
clicking a magic link in Gmail opens the browser, sets a cookie there,
and the app can't see it. Two viable paths:

1. **Embed sign-in in a `WebView`.** Point it at `/sign-in`, let the
   user enter their email and click the magic link (which can be
   intercepted via an Android app link / intent filter on the
   verification URL). Persist the resulting session cookie via a
   custom `CookieJar` and attach it to every OkHttp request to
   `/api/v1/*`. CSRF: NextAuth's CSRF token comes from
   `GET /api/auth/csrf` and must accompany sign-in/sign-out POSTs.
2. **Add a token issuance endpoint server-side.** A short-lived bearer
   that the Android client can store in `EncryptedSharedPreferences`,
   exchange the magic-link verification for it, and send as
   `Authorization: Bearer …`. Cleaner for native but a server change —
   not available today.

### Image URLs

Server responses include image fields like `imgUrl` (spot list
summaries) and `path` (uploads). The convention:

- `imgUrl` may already be absolute (`https://...`) or relative
  (`/api/v1/uploads/<path>`). Prefix relative URLs with the base URL
  before passing to your image loader.
- `POST /api/v1/uploads` returns both `path` and `url`. The `url` is
  what you embed in subsequent submissions (it satisfies the
  `LocalUploadImageUrlSchema`); the `path` (what's stored on disk) is
  what spot/photo endpoints accept inside their JSON bodies.

### CSRF

`POST/PATCH/DELETE` to `/api/v1/*` do **not** require a CSRF token.
The cookie is `SameSite=Lax`, so cross-site form submissions can't
forge state-changing calls; same-origin calls (your own client, or
your Android WebView session) just work. Sign-in/sign-out under
`/api/auth/*` is the only place CSRF tokens matter.

---

## Endpoints

Notation: `:id` = path parameter. `?foo` = query parameter. Auth tier:
**🌍 public** / **🔑 user** / **👑 admin**.

---

### Auth

#### `* /api/auth/*` 🌍

NextAuth v5 catch-all (re-exported from `lib/auth.ts`). The handler
mounts the standard sub-paths the Auth.js docs describe:

| Sub-path | Method | Purpose |
| --- | --- | --- |
| `/api/auth/providers` | GET | List configured providers (returns the `nodemailer` entry). |
| `/api/auth/csrf` | GET | Issue a CSRF token (required for sign-in POST). |
| `/api/auth/signin/nodemailer` | POST | Body: `email`, `csrfToken`, `callbackUrl`. Triggers the magic-link email. |
| `/api/auth/callback/nodemailer` | GET | Magic-link verification target. Sets the session cookie. |
| `/api/auth/signout` | POST | Body: `csrfToken`. Deletes the session row + clears the cookie. |
| `/api/auth/session` | GET | Returns the current session (`{ user, expires }`) or `null`. |

Sign-in success / failure pages: `/sign-in`, `/verify-request` (set in
`pages` config).

---

### Spots

#### `GET /api/v1/spots` 🌍

List/search spots. Three mutually-exclusive query modes (precedence
top→bottom):

| Mode | Query | Returns |
| --- | --- | --- |
| Centre + radius | `center=lng,lat&radius=<m>` | Geo-ordered list. |
| Bbox | `bbox=minLng,minLat,maxLng,maxLat` | Rectangular slice. |
| Free-text | `q=<2+ chars>` | Trigram typeahead, capped at 25. |

Other params (all optional):

- `limit` int, 1–5000, default 5000.
- `types` CSV of type keys (e.g. `wild,paid`).
- `amenities` CSV of amenity keys.
- `minRating` 0–5.
- `maxPrice` 0–10000.
- `pinned` CSV of UUIDs that must always be returned even if outside
  the search area or filtered out (used to keep route stops visible).

Errors: `400 { "error": "invalid_query", "issues": [...] }`,
`400 { "error": "missing_query" }` if none of bbox/center/q given.

Response: `{ spots: SpotSummary[], capped: boolean }` where `capped`
is true if the result hit `limit`.

`SpotSummary`:

```ts
{
  id: string;        // UUID
  slug: string;
  name: string;
  region: string;
  country: string;   // ISO 3166-1 alpha-2
  type: string;      // type key
  lat: number;
  lng: number;
  priceEur: number;
  ratingAvg: number;
  reviewCount: number;
  imgUrl: string | null;       // absolute https or "/api/v1/uploads/..."
  amenities: string[];         // amenity keys
  addedByName: string | null;
  daysAgo: number;
}
```

#### `POST /api/v1/spots` 🔑

Create a spot. Rate-limit: **10 / min / user**. Body limit: 64 KB.

Body (zod `SpotCreateSchema` extended):

```ts
{
  name: string;            // 2–120
  slug?: string;           // kebab-case, derived from name if omitted
  region: string;          // 2–120
  country: string;         // ISO alpha-2 (uppercased server-side)
  type: string;            // amenity-style key, must exist in spot_type_meta
  lat: number;             // -90..90
  lng: number;             // -180..180
  priceEur: number;        // 0..10000
  description?: string;    // 0..4000, default ""
  descriptionNl?: string | null;
  imgUrl?: string | null;          // https://... or /api/v1/uploads/...
  address?: string | null;
  website?: string | null;         // http(s) URL, max 255
  email?: string | null;
  phone?: string | null;           // free-form, max 40
  capacity?: number | null;        // int 0..10000
  openSeasons?: "year-round" | "spring-autumn" | "summer-only" | "winter-only" | null;
  amenities: string[];             // amenity keys, validated against catalog
  photoPaths: string[];            // upload paths from POST /uploads, max 20
  draft?: boolean;                 // true → status='incomplete', no photos attached yet
}
```

Response (`201`): `{ spot: { id, slug, status } }`.

Errors: `400 invalid` / `unknown_type` / `unknown_amenity` /
`invalid_photo_path`, `401 not_authenticated`,
`403 onboarding_required`, `409 slug_taken`, `429 rate_limited`.

#### `GET /api/v1/spots/:id` 🌍

Single published spot by **UUID** (not slug). Drafts and unpublished
statuses return `404`.

Response: `{ spot: SpotSummary }` — same shape as items in the list
endpoints.

Errors: `400 bad_id`, `404 not_found`.

#### `PATCH /api/v1/spots/:id` 🔑

Wizard-friendly partial update for the original contributor or an
admin. Every field optional; `lat`/`lng` must be supplied together.

Body (zod):

```ts
{
  name?: string;
  type?: string;
  lat?: number; lng?: number;
  region?: string;
  country?: string;
  priceEur?: number;
  description?: string;
  descriptionNl?: string | null;
  imgUrl?: string | null;
  address?: string | null;
  website?: string | null;
  email?: string | null;
  phone?: string | null;
  capacity?: number | null;
  openSeasons?: OpenSeason | null;
  amenities?: string[];           // replace-on-write
  photoPaths?: string[];          // only persisted when finalize=true
  finalize?: boolean;             // flips status from 'incomplete' → 'draft'
                                  //   and writes photos
}
```

Response: `{ ok: true }`.

Errors: `400 invalid` / `bad_id` / `invalid_photo_path`,
`401 not_authenticated`, `403 forbidden` (not owner, not admin),
`404 not_found`.

---

### Reviews

#### `GET /api/v1/spots/:id/reviews` 🌍

Newest-first reviews for a spot. Cursor pagination.

Query: `limit` (1–100, default 20), `cursor` (string from previous
`nextCursor`).

Response:

```ts
{
  reviews: Array<{
    id: string;
    rating: 1 | 2 | 3 | 4 | 5;
    body: string;
    authorName: string | null;
    daysAgo: number;
    createdAt: string;            // ISO timestamp
  }>;
  nextCursor: string | null;
}
```

#### `POST /api/v1/spots/:id/reviews` 🔑

Leave a review. Rate-limit: **10 / min / user**.

Body: `{ rating: 1..5 (int), body: string (1..4000) }`.

Response (`201`): `{ review: { id } }`.

Errors: `400 invalid` / `bad_id`, `401`, `403 onboarding_required`,
`404 not_found`, `429 rate_limited`.

`spots.rating_avg` / `spots.review_count` are kept in sync by a
trigger; clients don't need to recompute.

---

### Saves (favourites)

#### `POST /api/v1/spots/:id/save` 🔑

Idempotent insert into `saves`. Spot must be `published`.

Response: `{ saved: true }`.
Errors: `400 bad_id`, `401`, `404 not_found`.

#### `DELETE /api/v1/spots/:id/save` 🔑

Idempotent delete. Always returns `{ saved: false }` (never errors on
"already gone").

---

### Contributions & events

#### `POST /api/v1/spots/:id/contributions` 🔑

Submit a photo upload, edit suggestion, or report against a spot.
Rate-limit: **20 / min / user**. Discriminated by `kind`:

```ts
// kind: "photo"
{ kind: "photo", payload: { path: string, caption?: string (max 280) } }

// kind: "edit"
{
  kind: "edit",
  payload: {
    patch: Partial<{
      name; type; lat; lng; priceEur; description; imgUrl;
      address; website; email; phone; descriptionNl;
      capacity; openSeasons;
    }>;                            // lat/lng must be paired
    amenities?: string[] | null;   // null = clear; undefined = unchanged
  }
}

// kind: "report"
{
  kind: "report",
  payload: {
    category: "closed" | "wrong_location" | "outdated" | "other";
    body: string (1..1000);
  }
}
```

Behaviour:
- For `photo` and `edit`, if the caller is the spot's original
  contributor or an admin the change applies immediately and the row
  is recorded as `applied` for audit.
- Otherwise the row is `open` (queued for moderator review).
- `report` always queues.

Response (`201`): `{ ok: true, applied: boolean, id: string }`.

Errors: `400 invalid` / `bad_id` / `invalid_photo_path` /
`unknown_type` / `unknown_amenity`, `401`, `403`, `404 not_found`,
`429 rate_limited`.

#### `POST /api/v1/spots/:id/events` 🌍

Beacon for contact-link clicks (`click_website`, `click_email`,
`click_phone`). Accepts JSON body or plain text — the website uses
`navigator.sendBeacon`. **No auth required**, but throttled per IP at
**120 / min**.

Body: `{ kind: "click_website" | "click_email" | "click_phone" }`.

Response: `{ ok: true }`. Self-clicks (spot owner clicking own
contact) are silently dropped.

Errors: `400 bad_kind` / `bad_body`, `404 not_found`, `429`.

---

### Trips

#### `GET /api/v1/trips` 🌍

List public trips. No pagination — bounded set.

Response: `{ trips: TripSummary[] }`.

`TripSummary`:

```ts
{
  id: string; slug: string;
  name: string; summary: string;
  days: number; km: number;
  heroPath: string | null;
  spotCount: number;
}
```

#### `POST /api/v1/trips` 🔑

Create a trip plus its stops in one transaction. Rate-limit:
**10 / min / user**. New trips default to `isPublic: false`.

Body:

```ts
{
  name: string;                // 2..120
  summary?: string;            // 0..500, default ""
  days?: number;               // 1..365, default 1
  km?: number;                 // 0..100000, default 0
  heroPath?: string | null;    // upload path or null
  isPublic?: boolean;          // default false
  stops?: Array<{
    spotId: string;            // UUID
    dayIndex?: number;         // 0..364, default 0
    position?: number;         // 0..50, default index in array
    notes?: string;            // 0..1000
  }>;                          // max 200
}
```

Response (`201`): `{ trip: { id, slug, isPublic } }`.

Errors: `400 invalid`, `401`, `403`, `409 slug_taken`, `429`.

#### `PATCH /api/v1/trips/:id` 🔑

Owner or admin update. Body fields all optional but at least one
required (else `400 no_updates`):

```ts
{ name?: string; summary?: string; days?: number; isPublic?: boolean }
```

Response: `{ ok: true }`. Errors: `400`, `401`, `403`, `404`.

#### `DELETE /api/v1/trips/:id` 🔑

Owner or admin delete. Cascades to `trip_stops`. Response:
`{ ok: true }`.

#### `GET /api/v1/me/trips` 🔑

Caller's trips at any visibility. Response: `{ trips: TripSummary[] }`
(each entry's `isPublic` is populated).

#### `POST /api/v1/trips/:id/stops` 🔑

Append a single spot to a trip (used by the "Add to trip" picker).
Auto-appends to the last day at the next `position`.

Body:

```ts
{
  spotId: string;             // UUID
  dayIndex?: number;          // 0..364; defaults to trip's last day
  notes?: string;             // 0..1000
}
```

Response: `{ ok: true, dayIndex: number, position: number }` —
or `{ ok: true, dedupe: true }` if the (trip, spot, dayIndex) combo
already exists.

Errors: `400 invalid` / `bad_id`, `401`, `403`, `404 not_found`.

---

### Uploads

#### `POST /api/v1/uploads` 🔑

Multipart upload, single field `file`. Rate-limit: **20 / min / user**.
Body cap: 10 MB.

Files land at `pending/<userId>/<uuid>.<ext>`. Allowed extensions:
`jpg | jpeg | png | webp | avif`.

Response: `{ path: string, url: string }` — `path` is what other
endpoints accept inside JSON bodies (`StoredPhotoPathSchema`); `url`
is the `/api/v1/uploads/<path>` form for image rendering.

Errors: `400 missing_file` / upload-validation codes (e.g.
`unsupported_type`, `too_large`), `401`, `403`, `429`.

#### `GET /api/v1/uploads/<path>` 🌍

Stream the file. Public read with immutable cache headers
(`Cache-Control: public, max-age=31536000, immutable`,
`X-Content-Type-Options: nosniff`). Path-traversal blocked at handler.

Errors: `403 forbidden` (traversal attempt), `404`.

#### `DELETE /api/v1/uploads/<path>` 🔑

Remove an unattached pending upload. Restricted to:
- the `pending/` bucket (attached photos are removed via spot-photo
  admin endpoints — see below)
- the embedded `ownerId` matching the caller

Idempotent: if the file was already gone, returns
`{ ok: true, alreadyMissing: true }`.

Errors: `403 forbidden`, `409 attached_photo`.

---

### Admin: spots

#### `PATCH /api/v1/admin/spots/:id` 👑

Update moderation flags. At least one field required.

```ts
{
  status?: "incomplete" | "draft" | "ai_checked" | "published" | "flagged";
  featured?: boolean;
  verified?: boolean;          // true → stamp verifiedAt now; false → clear
}
```

Response: `{ ok: true }`. Errors: `400 invalid` / `no_updates`,
`401`, `403`, `404`.

#### `DELETE /api/v1/admin/spots/:id` 👑

Delete the spot (FK cascade handles photos, reviews, amenities,
saves, trip stops). Response: `{ ok: true }`.

#### `POST /api/v1/admin/spots/:id/photos` 👑

Attach a pending upload to the spot. Body: `{ path: string }` (must
match `StoredPhotoPathSchema`).

Response: `{ ok: true, photo: { id, url } }` — the photo gets the next
`position`.

Errors: `400 invalid` / `invalid_photo_path` / `bad_id`, `404`.

#### `PATCH /api/v1/admin/spots/:id/photos` 👑

Reorder photos. Body: `{ orderedIds: string[] }` (UUIDs, max 100).
Must contain exactly the spot's existing photo set; mismatch returns
`409 photo_set_changed`. Duplicates → `400 duplicate_ids`.

Response: `{ ok: true }`.

#### `DELETE /api/v1/admin/spots/:id/photos/:photoId` 👑

Remove a photo and reindex remaining `position`s.

Response: `{ ok: true }`. Errors: `400 bad_id`, `404 not_found`.

---

### Admin: contributions

#### `GET /api/v1/admin/contributions` 👑

List for the moderation queue.

Query:
- `kind` = `all | photo | edit | report` (default `all`).
- `status` = `all | open | applied | dismissed` (default `open`).
- `limit` int 1..500, default 200.

Response: `{ contributions: Array<{ id, kind, status, payload, created_at, resolved_at, spot_id, spot_slug, spot_name, user_id, user_name, user_email }> }`.

#### `PATCH /api/v1/admin/contributions/:id` 👑

Apply or dismiss. Body: `{ action: "apply" | "dismiss" }`.

`apply` re-validates the payload (per-kind schemas evolve), runs the
kind-specific apply logic in a transaction, and stamps the row
`applied`. `dismiss` just stamps `dismissed`.

Response: `{ ok: true, status: "applied" | "dismissed" }`.

Errors: `400 invalid` / `stale_payload` / `invalid_photo_path` /
`bad_id`, `404`, `409 already_resolved`.

#### `DELETE /api/v1/admin/contributions/:id` 👑

Hard delete (no soft-delete). Response: `{ ok: true }`.

---

### Admin: reviews

#### `PATCH /api/v1/admin/reviews/:id` 👑

Edit text + rating. Body: `{ rating: 1..5, body: string (1..4000) }`.

Response: `{ ok: true, review: { id } }`. Errors: `400 invalid` /
`bad_id`, `404 not_found`.

#### `DELETE /api/v1/admin/reviews/:id` 👑

Response: `{ ok: true }`. Errors: `400 bad_id`, `404 not_found`.

---

### Admin: types

Types are admin-curated (color/icon/i18n labels). Key is **immutable**
once created (referenced by `spots.type`).

#### `POST /api/v1/admin/types` 👑

```ts
{
  key: string;                  // ^[a-z][a-z0-9_-]*$, 1..32
  color: string;                // #rrggbb
  icon: string;                 // 1..32
  position?: number;            // 0..999, default 0
  labels: Record<localeCode, { label: string }>;   // localeCode 2..8 chars
}
```

Response (`201`): `{ ok: true, key }`. Errors: `400 invalid`,
`409 key_taken`.

#### `PATCH /api/v1/admin/types/:key` 👑

Edit `color` / `icon` / `position` / `labels` (all optional but at
least one required).

Response: `{ ok: true }`. Errors: `400 invalid` / `no_updates`,
`404 not_found`.

#### `DELETE /api/v1/admin/types/:key` 👑

Refused if any spot still uses this type:
`409 { error: "in_use", count: number }`. Otherwise `{ ok: true }`.

---

### Admin: amenities

Same shape as types but no `color`/`icon` fields.

#### `POST /api/v1/admin/amenities` 👑

```ts
{
  key: string;                  // ^[a-z][a-z0-9_-]*$, 1..32
  position?: number;            // 0..999, default 0
  labels: Record<localeCode, string>;
}
```

Response (`201`): `{ ok: true, key }`. Errors: `400 invalid`,
`409 key_taken`.

#### `PATCH /api/v1/admin/amenities/:key` 👑

Body: `{ position?: number; labels?: ... }` (at least one).

Response: `{ ok: true }`. Errors: `400 invalid`, `404 not_found`.

#### `DELETE /api/v1/admin/amenities/:key` 👑

`409 in_use` if referenced by any `spot_amenities` row, else
`{ ok: true }`.

---

### Admin: users

#### `PATCH /api/v1/admin/users/:id` 👑

Change role. Self-modification is rejected
(`400 cant_modify_self`).

Body: `{ role: "user" | "admin" }`.

Response: `{ ok: true }`. Errors: `400 invalid` / `bad_id` /
`cant_modify_self`, `401`, `403`.

---

### Admin: AI

#### `GET /api/v1/admin/ai/settings` 👑

Returns the stored AI provider config including the API key (admin is
the only caller and the form needs to render the existing value).

Response: `{ settings: { baseUrl, apiKey, model, models, schedules } }`.

`models`: `{ translate, vision, safety, description, summary, text }`.
`schedules`: `{ translate, vision, summarize?: { hour, minute, enabled } }`.

#### `PUT /api/v1/admin/ai/settings` 👑

Replace the full config atomically. Body shape mirrors the GET
response. Legacy single `model` field falls back to `models.translate`
when omitted; legacy `vision` model falls back to whichever of
`safety`/`description` is set.

Response: `{ ok: true }`. Errors: `400 invalid`.

#### `POST /api/v1/admin/ai/models` 👑

Probe an LM Studio `/api/v1/models` endpoint to populate a dropdown in
the AI-settings form (so the admin can verify a `baseUrl + apiKey`
combo before saving it).

Body: `{ baseUrl: string, apiKey?: string }`. 10s timeout.

Response: `{ models: string[] }` (deduped, sorted).

Errors: `400 invalid`, `502 unreachable` (with `detail`),
`502 upstream_error` (with `status` and short `detail`).

#### `POST /api/v1/admin/ai/translate` 👑

On-demand translation, sharing the codepath with the nightly job. 180s
timeout (`maxDuration`).

Body: `{ text: string (1..8000), from: string (2..8), to: string (2..8) }`.

Response: `{ text: string }`.

Errors: `400 invalid` / `ai_not_configured`,
`502 upstream_error` (with `status`),
`504 upstream_timeout`.

#### `POST /api/v1/admin/ai/run-now` 👑

Manual trigger for the nightly AI review job. Body:
`{ task?: "translate" | "vision" | "summarize" }` — omit to run all
three. 600s timeout (`maxDuration`).

Response: `{ ok: true, stats: { translation?, vision?, summarize? } }`
(per-pass counts).

Errors: `400 invalid` / `ai_not_configured`, `500 internal` (with
`detail`).

Side effect: first call boots the in-process scheduler.

---

## Counts

| Tier | Count |
| --- | --- |
| 🌍 Public (no auth) | 6 |
| 🔑 User (signed-in) | 13 |
| 👑 Admin | 22 |
| **Total `/api/v1/*` endpoints** | **41** |
| Plus NextAuth catch-all `/api/auth/*` | (1 mount, ~6 sub-paths) |

Source-of-truth check:
`find app/api -name route.ts | xargs grep -cE '^export (async )?function (GET|POST|PUT|PATCH|DELETE)'`
→ 41 method exports across `/api/v1/*`. The `/api/auth/[...nextauth]`
route re-exports `{ GET, POST }` from NextAuth, which then dispatches
to its built-in sub-paths.
