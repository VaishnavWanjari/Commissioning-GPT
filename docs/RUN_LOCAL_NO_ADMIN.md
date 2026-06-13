# Running Commissioning-GPT locally **without administrator rights** (Windows)

You do **not** need an administrator password/username to run this app. Below
are two ways to run it. **Mode A is the easiest and recommended.**

---

## Mode A — Browser + Python only (recommended, no Node, no admin)

The Python backend serves a built-in web UI, so you only need a **per-user
Python** install. Everything stays in your user folder — no admin prompt.

### 1. Install Python without admin (one time)
1. Download Python 3.11+ from <https://www.python.org/downloads/windows/>.
2. Run the installer and **UNCHECK** "Install for all users" — this makes it a
   **per-user** install that needs **no administrator**.
3. **CHECK** "Add python.exe to PATH", then click *Install Now*.

> Alternative with no installer at all: `winget install Python.Python.3.12 --scope user`

### 2. Set up (one time)
Double-click **`setup-windows.bat`** in the project folder. It will:
- create a local virtual environment under `backend\.venv` (in your user space),
- install the Python dependencies,
- create `backend\.env` from the example.

Then open **`backend\.env`** and set your key:
```
OPENAI_API_KEY=sk-your-key-here
```

### 3. Run (every time)
Double-click **`run-windows.bat`**. It starts the server and opens your browser
at <http://127.0.0.1:8000>. Use the app; press `Ctrl+C` in the black window to stop.

That's it — upload documents, pick a procedure type, generate, and export to
Word / PDF / Excel. The downloaded files land in your normal *Downloads* folder.

---

## Mode B — Full Electron desktop app (no admin)

The desktop app is configured to **not** require admin:

- **Portable build** — a single `.exe` you can run from anywhere (no install):
  produces `Commissioning-GPT-Portable-<version>.exe`.
- **Per-user installer** — `nsis` is set to `perMachine: false`, so it installs
  into `%LOCALAPPDATA%` for the current user only (no UAC / admin prompt).

### Getting Node without admin (only needed to *build* the app)
- **Portable Node (no installer):** download the **Windows Binary (.zip)** from
  <https://nodejs.org/en/download/>, unzip it anywhere (e.g. `C:\Users\you\node`),
  and add that folder to your user PATH. No admin required.
- **Or:** `winget install OpenJS.NodeJS.LTS --scope user`

### Build & run
```bat
cd frontend
npm install
npm run build           REM creates frontend\release\ with the portable .exe and per-user installer
```
The Electron app still talks to the Python backend (Mode A step 2–3) on
`http://127.0.0.1:8000`. Start the backend first with `run-windows.bat`, or set
the `COMMISSIONING_GPT_BACKEND` environment variable to a command the app will
launch for you.

For quick iteration without packaging:
```bat
cd frontend
npm install
npm run dev:electron
```

---

## Optional: OCR for scanned PDFs / images (no admin)

OCR is optional. To enable it without admin, download a portable Tesseract build
(e.g. the UB-Mannheim installer offers a per-user option), then either add it to
your PATH or set `TESSERACT_CMD`. Without it, text-based PDFs and Office files
still work fully; only scanned/image pages are skipped.

---

## Troubleshooting

| Symptom | Fix |
|--------|-----|
| `Python was not found` | Re-run the Python installer, ensure **Add to PATH** is checked. Open a new terminal. |
| Browser shows "Backend offline" | Make sure the black `run-windows.bat` window is still open. |
| Banner: "OPENAI_API_KEY is not configured" | Edit `backend\.env`, set the key, then restart `run-windows.bat`. |
| Port 8000 already in use | Edit `PORT` in `backend\.env` (and open that port in the browser). |
| Corporate network blocks PyPI | Ask IT for an internal PyPI mirror, or use a machine with internet for `setup-windows.bat`. |
