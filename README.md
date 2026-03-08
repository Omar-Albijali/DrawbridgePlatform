# Drawbridge Platform

Multi-platform retail and e-commerce platform built with Kotlin Multiplatform, Spring Boot, and React.

## What You Will Run

- Backend API: `http://localhost:8080`
- Web app: `http://localhost:3000`

The web app proxies `/api` and `/uploads` to the backend, so both services should be running during normal web development.

## Project Modules

- `server`: Spring Boot backend
- `webApp`: React + Vite frontend
- `shared`: Kotlin Multiplatform shared models and logic
- `composeApp`: Compose Multiplatform UI module
- `androidApp`: Android application entry point
- `iosApp`: iOS application entry point

## Prerequisites

Install these before you start:

- JDK 21
- Node.js 20+ and npm
- Git
- IntelliJ IDEA or Android Studio
- Xcode if you will work on iOS

Recommended checks in PowerShell:

```powershell
java -version
node -v
npm -v
git --version
```

## Quick Start

Open the repository root in PowerShell and run the steps in order.

### 1. Clone the repository

```powershell
git clone <repository-url>
cd DrawbridgePlatform
```

If you already cloned it, just open the root folder:

```powershell
cd <path-to-DrawbridgePlatform>
```

### 2. Create local environment files

```powershell
./setup-env.cmd
```

This creates these files if they do not already exist:

- `server/.env`
- `webApp/.env`

Then open `server/.env` and set these required values:

- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`

The remaining defaults are acceptable for normal local development unless the team tells you otherwise.

### 3. Build the shared module

Build the shared JS package before running the web app for the first time:

```powershell
./gradlew.bat :shared:jsBrowserDevelopmentLibraryDistribution
```

### 4. Install frontend dependencies

```powershell
npm install
```

### 5. Start the backend

In one PowerShell window:

```powershell
./gradlew.bat :server:bootRun
```

Wait until the server finishes starting, then confirm it is reachable at `http://localhost:8080`.

### 6. Start the web app

In a second PowerShell window:

```powershell
npm run dev
```

Then open `http://localhost:3000`.

## First Run Checklist

Use this if something does not start as expected.

1. Confirm Java is version 21.
2. Confirm `server/.env` exists and contains `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET`.
3. Confirm the shared module was built successfully before the first web run.
4. Confirm `webApp/node_modules` exists after `npm install`.
5. Confirm the backend is running on port `8080` before testing API-driven pages.
6. Confirm the web app is running on port `3000`.

## Useful Commands

Use these when you need them. Run Gradle commands from the repository root.

Build the shared module again after shared Kotlin changes:

```powershell
./gradlew.bat :shared:jsBrowserDevelopmentLibraryDistribution
```

Build the backend:

```powershell
./gradlew.bat :server:build
```

Run backend tests:

```powershell
./gradlew.bat :server:test
```

Build the web app:

```powershell
npm run build
```

## IDE Setup

IntelliJ IDEA and Android Studio both work well for this repo.

### Recommended IntelliJ or Android Studio setup

1. Open the repository root as a Gradle project.
2. Set the project JDK to Java 21.
3. Let Gradle import all modules.
4. Install Node.js support if your IDE asks for it.

### Suggested run configurations

Create these run configurations so the shared package and frontend dependencies are prepared before the web app starts.

#### 1. Backend

- Type: Gradle
- Name: `server:bootRun`
- Working directory: repository root
- Tasks: `:server:bootRun`

#### 2. Web app combined configuration

- Type: npm
- Name: `webApp dev`
- Package.json: repository root `package.json`
- Command: `run`
- Script: `dev`
- Before launch step 1: Gradle task `:shared:jsBrowserDevelopmentLibraryDistribution`
- Before launch step 2: `npm install` from the repository root

This keeps the first web run consistent and prevents the frontend from starting before the shared package is ready.

#### 3. Optional full-stack compound configuration

Create a compound configuration that starts:

- `server:bootRun`
- `webApp dev`

Use this for day-to-day development when you want backend and frontend running together.

## Git Workflow

This repository prefers rebasing over merging for normal feature branch work. The goal is to keep history linear and easier to review.

### Branching

Create a short-lived feature branch for each task. Keep work isolated there instead of committing directly to `master`.

### Updating your branch

When `master` moves forward, update your branch with rebase. That reapplies your local commits on top of the latest remote history and avoids unnecessary merge commits.

Recommended approach:

```powershell
git pull --rebase origin master
```

If a rebase stops on conflicts, resolve the files, stage them, and continue the rebase. If the rebase goes wrong, abort it and return to the previous state.

### Pushing changes

Most pushes should be normal pushes. After a rebase, Git may reject the push because your local history was rewritten. In that case, prefer `--force-with-lease` instead of `--force`.

- `git push --force-with-lease` is the safe option because it refuses to overwrite remote commits you have not seen.
- `git push --force` is riskier because it can overwrite someone else’s work on the remote branch.

### Daily rule for this repo

- Rebase is the preferred integration method for feature branches.
- Avoid merge commits unless there is a specific reason to preserve them.
- Avoid rebasing branches that multiple people are actively pushing to without coordinating first.

## Troubleshooting

### Port 8080 is already in use

Another backend instance may still be running. Stop it, then start the server again.

### Port 3000 is already in use

Another Vite process may still be running in another terminal.

### `npm run dev` fails immediately

Run these again from the repository root:

```powershell
npm install
npm run dev
```

### Backend fails on startup

Check these first:

- Java version is 21
- `server/.env` exists
- required environment values are set


