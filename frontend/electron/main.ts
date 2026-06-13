import { app, BrowserWindow, Menu, shell } from "electron";
import { spawn, ChildProcess } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const isDev = !!process.env.VITE_DEV_SERVER_URL;

let mainWindow: BrowserWindow | null = null;
let backend: ChildProcess | null = null;

/**
 * In development the Python backend is expected to be started separately
 * (`uvicorn app.main:app --reload`). In a packaged build we attempt to spawn a
 * bundled backend if one is present; otherwise the UI still loads and shows a
 * clear "backend offline" banner. See README for packaging the backend.
 */
function startBackend() {
  if (isDev) return;
  const backendCmd = process.env.COMMISSIONING_GPT_BACKEND;
  if (!backendCmd) return;
  try {
    backend = spawn(backendCmd, { shell: true, stdio: "inherit" });
  } catch (err) {
    console.error("Failed to start backend:", err);
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1360,
    height: 880,
    minWidth: 1024,
    minHeight: 680,
    title: "Commissioning-GPT",
    backgroundColor: "#0f1726",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL);
  } else {
    mainWindow.loadFile(path.join(__dirname, "../dist/index.html"));
  }

  // Open external links in the user's browser, not inside the app.
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });

  mainWindow.on("closed", () => (mainWindow = null));
}

app.whenReady().then(() => {
  startBackend();
  createWindow();
  Menu.setApplicationMenu(buildMenu());

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

app.on("before-quit", () => {
  if (backend) backend.kill();
});

function buildMenu() {
  return Menu.buildFromTemplate([
    {
      label: "File",
      submenu: [{ role: "quit" }],
    },
    {
      label: "View",
      submenu: [
        { role: "reload" },
        { role: "toggleDevTools" },
        { type: "separator" },
        { role: "resetZoom" },
        { role: "zoomIn" },
        { role: "zoomOut" },
        { type: "separator" },
        { role: "togglefullscreen" },
      ],
    },
    {
      label: "Help",
      submenu: [
        {
          label: "Documentation",
          click: () =>
            shell.openExternal(
              "https://code.claude.com/docs/en/claude-code-on-the-web"
            ),
        },
      ],
    },
  ]);
}
