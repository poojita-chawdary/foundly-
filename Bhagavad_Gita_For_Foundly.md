# 📖 Bhagavad Gita for Foundly
### *The Complete, No-Nonsense Guide to How Foundly Works, Where Everything Lives, and Why It Was Built This Way*

---

## 🌟 Table of Contents
1. [The Big Picture: What is Foundly?](#1-the-big-picture-what-is-foundly)
2. [The 3-Piece Architecture (Where Does Everything Live?)](#2-the-3-piece-architecture-where-does-everything-live)
3. [Why This Tech Stack? (Plain English Decisions)](#3-why-this-tech-stack-plain-english-decisions)
4. [The Grand Tour: File Map & What Lives Where](#4-the-grand-tour-file-map--what-lives-where)
5. [The Journey of a Click: Frontend to Backend & Database](#5-the-journey-of-a-click-frontend-to-backend--database)
6. [The "Render is Sleeping" Question: What Happens When the Backend is Down?](#6-the-render-is-sleeping-question-what-happens-when-the-backend-is-down)
7. [How Images and Uploads Actually Work](#7-how-images-and-uploads-actually-work)
8. [Security & Authentication in Plain English](#8-security--authentication-in-plain-english)
9. [Cheatsheet: URLs, Credentials & Common Commands](#9-cheatsheet-urls-credentials--common-commands)

---

## 1. The Big Picture: What is Foundly?

Imagine a college campus where hundreds of students lose ID cards, keys, water bottles, and chargers every single week. Usually, students post randomly on WhatsApp groups or Instagram stories where things get buried in minutes.

**Foundly** is a dedicated campus digital lost-and-found hub:
- A student who **loses** an item posts a "LOST" report with photos and description.
- A student or staff member who **finds** an item posts a "FOUND" report.
- The system automatically compares lost and found items in the background and suggests matches.
- An owner can submit a secure claim with private verification proof, and campus staff or the finder can approve handovers safely without exposing phone numbers or ID secrets to the public.

---

## 2. The 3-Piece Architecture (Where Does Everything Live?)

Rather than cramming everything into one clunky server that breaks easily, Foundly is split into **three dedicated services**, each doing the exact job it is best at:

```
                  ┌───────────────────────────────────────────────┐
                  │                 USER'S BROWSER                │
                  │   Phone, Laptop, Tablet visiting the website  │
                  └───────────────────────┬───────────────────────┘
                                          │
                        1. Web Page &     │ 2. API Requests
                        Button Clicks     │    (/api/reports, /api/auth)
                                          │
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │              LAYER 1: FRONTEND                │
                  │             Hosted on: VERCEL                 │
                  │       foundly-alpha-ivory.vercel.app          │
                  │                                               │
                  │  • Lightning-fast worldwide CDN delivery      │
                  │  • Serves HTML, React UI, styling, and images │
                  │  • Proxies /api/* requests over to Render     │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          │ Internal Forwarding
                                          │ (Invisible to user)
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │              LAYER 2: BACKEND                 │
                  │             Hosted on: RENDER                 │
                  │         foundly-xoro.onrender.com             │
                  │                                               │
                  │  • Brain of the operation (Java + Spring Boot)│
                  │  • Handles authentication, checks passwords   │
                  │  • Runs AI matching algorithm                 │
                  │  • Stores uploaded photos on local disk       │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          │ Encrypted Database Calls
                                          │ (JDBC over SSL)
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │              LAYER 3: DATABASE                │
                  │            Hosted on: SUPABASE                │
                  │       PostgreSQL Cloud DB (Singapore)         │
                  │                                               │
                  │  • Stores users, reports, claims, categories  │
                  │  • Keeps data safe, backed up, and persistent │
                  └───────────────────────────────────────────────┘
```

### Why did we choose these exact 3 hosts?

| Layer | Platform Chosen | Why this specific platform? |
|---|---|---|
| **Frontend** | **Vercel** | **Instant loading.** Vercel is the gold standard for React apps. It copies the user interface files to edge servers across the globe so pages open in milliseconds anywhere in the world. It also has automatic HTTPS and zero configuration. |
| **Backend** | **Render** | **Full Docker & Java support.** Java Spring Boot applications need a real Linux server with Java 21 and a filesystem to run and store images. Render lets us build and run our custom Docker container with zero server maintenance. |
| **Database** | **Supabase** | **Rock-solid PostgreSQL.** Instead of running a risky database inside a container that could lose data if restarted, Supabase provides an industrial-grade PostgreSQL database with daily backups, connection pooling, and SSL encryption. |

---

## 3. Why This Tech Stack? (Plain English Decisions)

### 🎨 Frontend: React + TypeScript + Vite
- **React**: Lets us build reactive interactive screens (searching, live filtering by category, instant status badges) without reloading the page every time you click a button.
- **TypeScript**: Catches spelling mistakes and bugs while typing code instead of finding them when a student clicks a broken button.
- **Vite**: The modern tool that bundles and packages all frontend code in seconds.
- **Pure CSS**: Instead of bloating the application with heavy third-party UI libraries that all look the same, Foundly uses custom crafted CSS tailored for a clean, premium campus aesthetic with dark/light harmony.

### 🧠 Backend: Java 21 + Spring Boot 3.5
- **Enterprise Reliability**: Java and Spring Boot power banking and university systems worldwide. It handles concurrency, transactions, and security checks cleanly.
- **Spring Security & BCrypt**: Protects student passwords with industry-standard salted BCrypt hashes. Prevents CSRF attacks, rate limits brute force abuse, and manages user sessions.
- **Flyway Migrations**: Tracks database changes version by version (like Git for your database tables) so the database never gets corrupted or out of sync.

### 🗄️ Database: PostgreSQL (via Supabase)
- **Relational Integrity**: If an item report belongs to a user, PostgreSQL guarantees that deleting or changing things never leaves orphaned "ghost" data.
- **JSON & Text Search**: Allows fast keyword matching across item titles, colors, and locations.

---

## 4. The Grand Tour: File Map & What Lives Where

Here is where every key piece of code lives in the project folder:

```
foundly/
├── frontend/                                   # EVERYTHING THE USER SEES & TOUCHES
│   ├── vercel.json                             # Tells Vercel how to forward /api/* to Render
│   ├── package.json                            # Frontend libraries (React, Vite, TypeScript)
│   ├── index.html                              # Web app entry page
│   └── src/
│       ├── App.tsx                             # Main router (decides which page to show)
│       ├── api.ts                              # Helper functions for talking to the backend
│       ├── styles.css                          # Beautiful styling, colors, cards, and animations
│       ├── pages/
│       │   ├── Dashboard.tsx                   # Main lost & found feed and search filters
│       │   ├── Detail.tsx                      # Single item page with image, map, and claim button
│       │   ├── ReportForm.tsx                  # "Report Lost" or "Report Found" form with photo picker
│       │   ├── Auth.tsx                        # Login and Signup forms
│       │   ├── Matches.tsx                     # Smart match suggestions view
│       │   └── Admin.tsx                       # Admin moderation panel (manage users, flags)
│       └── components/
│           ├── Navbar.tsx                      # Top header with user profile & navigation links
│           └── ItemCard.tsx                    # The visual card showing photo, badge, title, & place
│
├── backend/                                    # THE ENGINE ROOM (Spring Boot)
│   ├── pom.xml                                 # Java dependencies (Spring, PostgreSQL, Flyway, etc.)
│   └── src/main/
│       ├── java/campus/foundly/
│       │   ├── FoundlyApplication.java         # Starts the Spring Boot application
│       │   ├── auth/                           # Login, register, current user profile
│       │   │   ├── AuthController.java         # Endpoints: /api/auth/login, /api/auth/register
│       │   │   └── AuthService.java            # Account registration and password checks
│       │   ├── report/                         # Lost & Found report logic
│       │   │   ├── ReportController.java       # Endpoints: /api/reports (search, post, edit)
│       │   │   ├── ReportService.java          # Validates reports and saves them
│       │   │   ├── ImageService.java           # Decodes, validates, and stores photos on disk
│       │   │   └── MatchingService.java        # Algorithm matching LOST items with FOUND items
│       │   ├── claim/                          # The handover and verification flow
│       │   │   ├── ClaimController.java        # Endpoints: /api/claims
│       │   │   └── ClaimService.java           # Manages claimant proof and approval
│       │   ├── security/                       # Access control, passwords, CSRF defense
│       │   │   ├── SecurityConfig.java         # Protects routes, sets up login rules
│       │   │   ├── Access.java                 # Checks if a user is an ADMIN or item owner
│       │   │   └── AbuseFilter.java            # Blocks brute force spam and DDoS
│       │   ├── common/                         # Database connection & helpers
│       │   │   └── SupabaseDatabaseConfig.java # Connects Java to Supabase with SSL pooler
│       │   └── domain/                         # Java Data Models (User, ItemReport, Claim, Match)
│       └── resources/
│           ├── application.yml                 # Default backend settings
│           ├── application-supabase.yml        # Supabase PostgreSQL database settings
│           └── db/
│               ├── postgresql/                 # SQL schemas for PostgreSQL / Supabase
│               │   ├── V1__initial_schema.sql  # Creates users, reports, claims, categories tables
│               │   └── V2__seed_registrants.sql# Seeds admin user
│               └── migration/                  # MySQL / local Flyway migrations
│
├── deploy/                                     # DEPLOYMENT CONFIGURATIONS
│   └── render/
│       ├── backend.Dockerfile                  # Instructions for building Java into a Docker container
│       └── start-backend.sh                    # Startup script that sets up image storage folder
├── render.yaml                                 # Render service blueprint
├── .env                                        # Secrets & keys (Database URL, password, ports)
└── Bhagavad_Gita_For_Foundly.md                # THIS SCRIPTURE
```

---

## 5. The Journey of a Click: Frontend to Backend & Database

What actually happens when a student opens Foundly and clicks **"Report Lost Item"**?

```
[Student in Browser]
       │
       │ 1. Fills form: "Lost Student ID Card", selects photo, clicks "Submit"
       ▼
[React App on Vercel]
       │
       │ 2. JavaScript checks if fields are valid.
       │ 3. Browser sends HTTP request: POST https://foundly-alpha-ivory.vercel.app/api/reports
       ▼
[Vercel Edge Gateway (vercel.json)]
       │
       │ 4. Vercel notices the path starts with "/api/"!
       │ 5. Using the rule in vercel.json, it silently rewrites the target to:
       │    POST https://foundly-xoro.onrender.com/api/reports
       ▼
[Render Linux Container (Spring Boot Backend)]
       │
       │ 6. Spring Security AbuseFilter checks: "Is this IP spamming? No."
       │ 7. Spring Security checks: "Is this user signed in with valid session cookie? Yes."
       │ 8. ReportController receives JSON, passes to ReportService.
       │ 9. ReportService validates: Title max 120 chars, category exists, date is not in future.
       ▼
[Supabase PostgreSQL Database]
       │
       │ 10. Backend runs: INSERT INTO foundly.item_reports (...) VALUES (...);
       │ 11. Supabase saves the row and returns new ID: 1.
       ▼
[Render Backend: Matching & Photo Step]
       │
       │ 12. MatchingService immediately wakes up:
       │     "Does any existing FOUND report match this category and keywords?"
       │     If yes, creates a match suggestion.
       │ 13. Frontend receives ID: 1, then immediately sends the photo:
       │     POST https://foundly-xoro.onrender.com/api/reports/1/image
       │ 14. ImageService decodes JPEG/PNG, strips dangerous metadata,
       │     saves the sanitized image to /var/data/images/<uuid>.png
       │ 15. Updates report #1 with image_key in Supabase.
       ▼
[Browser receives response 200 OK]
       │
       └─► React redirects student to /items/1: "Your report is live with photo!"
```

---

## 6. The "Render is Sleeping" Question: What Happens When the Backend is Down?

This is the single most important operational detail of free-tier cloud hosting:

### Why does Render sleep?
Render's free tier gives us 750 free hours per month. The deal is: **If nobody makes a request to the backend for 15 minutes, Render puts the container to sleep** (suspends CPU and RAM to save resources).

### What happens in each scenario?

#### Scenario A: The backend is awake (someone visited in the last 15 minutes)
- Every click on the frontend responds instantly in ~100–300 milliseconds.

#### Scenario B: The backend is asleep ("Cold Start")
1. You open the website [foundly-alpha-ivory.vercel.app](https://foundly-alpha-ivory.vercel.app).
2. The homepage and HTML load **instantly** because Vercel never sleeps!
3. But when the page asks the backend for data (`/api/reports` or `/api/catalog`):
   - The request hits Render (`foundly-xoro.onrender.com`).
   - Render detects incoming traffic on a sleeping container.
   - Render triggers a **Wake Up**: It allocates a machine, pulls the Docker image, starts Java, connects to Supabase Hikari pool (~50–80 seconds).
4. **During these ~50-80 seconds:**
   - The browser shows a loading spinner.
   - If Vercel's proxy reaches its maximum wait timeout before Render finishes booting, you might see a `504 Gateway Timeout` or `Network Error`.
5. **The Fix / How to Handle It:**
   - Refresh the page after 60 seconds! Once Render wakes up, it stays awake for all subsequent visitors for at least 15 minutes.
   - For a production launch with constant traffic, upgrading the Render web service to the $7/month "Starter" plan keeps it awake 24/7 with zero cold starts.

---

## 7. How Images and Uploads Actually Work

Storing images in a web application can be tricky if not done securely. Here is how Foundly handles it:

1. **Upload Size & Format Guard**:
   - Only `image/jpeg` and `image/png` are accepted.
   - Maximum size allowed is **5 MB**.
   - Maximum dimensions allowed is **12 megapixels**.
2. **Sanitization (Safety First)**:
   - When a user uploads a photo, Java does not just save the raw file to disk (an attacker could hide malware inside an image file).
   - Java reads the pixels into a fresh in-memory image (`BufferedImage`), strips away all hidden EXIF metadata (which could reveal the user's home GPS coordinates), and re-encodes it as a fresh, clean PNG.
3. **Storage Location**:
   - The file is saved on Render's disk at `/var/data/images/<random-uuid>.png`.
   - The random UUID prevents users from guessing other users' image filenames.
4. **Serving Images**:
   - When the frontend displays `<img src="/api/reports/1/image" />`, the request goes through `ImageService.java`.
   - The backend checks if the item is public or readable, reads the file from disk, and streams the image bytes directly to the browser.

---

## 8. Security & Authentication in Plain English

Foundly is designed for college campuses where privacy matters:

1. **Passwords Never Stored Plaintext**:
   - When you set password `Foundly2026`, the database only stores:
     `$2a$10$REynuBVflnTRksNuvNL.LOTus1MFJUr4Q3fPv9ZV1HBxDxuzdhWxi`
   - This is a mathematical one-way hash. Even if an attacker stole the entire database, they cannot reverse the hash to find out your password.
2. **CSRF Protection (Cross-Site Request Forgery)**:
   - Every time a user opens the app, the backend generates a unique cryptographic token (`/api/auth/csrf`).
   - Any sensitive action (posting a report, claiming an item, signing in) must submit this token in the headers. Malicious websites cannot fake requests on your behalf.
3. **Roles & Permissions**:
   - `ADMIN` (You: `polurupoojithachowdary@gmail.com`): Can view all reports, manage users, moderate flags, and close disputes.
   - `USER` (Students): Can post their own lost/found items, view public items, and claim items that belong to them.
4. **Anti-Abuse Filter**:
   - The backend includes an `AbuseFilter` that limits login attempts to 20 per minute per IP address. If someone tries to run an automated password guessing bot, the system automatically blocks them with HTTP `429 Too Many Requests`.

---

## 9. Cheatsheet: URLs, Credentials & Common Commands

### 🔗 Live URLs
| Service | URL |
|---|---|
| **Website (Frontend)** | [https://foundly-alpha-ivory.vercel.app](https://foundly-alpha-ivory.vercel.app) |
| **Backend API** | [https://foundly-xoro.onrender.com](https://foundly-xoro.onrender.com) |
| **Backend Health Check** | [https://foundly-xoro.onrender.com/actuator/health](https://foundly-xoro.onrender.com/actuator/health) |
| **GitHub Repository** | [https://github.com/poojita-chawdary/foundly-](https://github.com/poojita-chawdary/foundly-) |
| **Supabase Project** | `afnbdrltoxyilkvxrgny` (ap-southeast-1) |

### 🔑 Administrator Login Credentials
- **Email**: `polurupoojithachowdary@gmail.com`
- **Password**: `Foundly2026`
- **Role**: `ADMIN` (Verified)

### 💻 Useful Commands

**Check Backend Health from PowerShell:**
```powershell
Invoke-WebRequest -Uri "https://foundly-xoro.onrender.com/actuator/health" | Select-Object StatusCode, Content
```

**Query Supabase Database from Terminal:**
```powershell
npx -y supabase db query --linked "SELECT id, title, type FROM foundly.item_reports;"
```

**Run Frontend Locally:**
```powershell
cd frontend
npm install
npm run dev
```

**Run Backend Locally (with Supabase profile):**
```powershell
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase
```

---

> *"Act with devotion, without attachment to the results; let your code be clean, your architecture sound, and your services resilient."*  
> — **Bhagavad Gita for Foundly**
