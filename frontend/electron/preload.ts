import { contextBridge } from "electron";

// The renderer talks to the FastAPI backend over HTTP (see src/api/client.ts),
// so only minimal, safe metadata is exposed across the context bridge here.
contextBridge.exposeInMainWorld("commissioningGpt", {
  platform: process.platform,
  isElectron: true,
});
