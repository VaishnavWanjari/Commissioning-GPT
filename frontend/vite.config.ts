import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import electron from "vite-plugin-electron";
import renderer from "vite-plugin-electron-renderer";

// Vite + React renderer, plus Electron main/preload bundling.
// `npm run dev` serves the renderer for fast iteration in a browser;
// `npm run dev:electron` / `npm run build` bundle the Electron shell.
export default defineConfig(({ command }) => ({
  plugins: [
    react(),
    electron([
      {
        entry: "electron/main.ts",
        onstart: (args) => args.startup(),
        vite: { build: { outDir: "dist-electron" } },
      },
      {
        entry: "electron/preload.ts",
        onstart: (args) => args.reload(),
        vite: { build: { outDir: "dist-electron" } },
      },
    ]),
    renderer(),
  ],
  server: { port: 5173 },
  build: { outDir: "dist" },
  // When running purely in the browser during `dev`, electron plugins no-op.
  define: { __IS_DEV__: command === "serve" },
}));
