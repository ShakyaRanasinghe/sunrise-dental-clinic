import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The Spring Boot API has no CORS configured, so in dev we proxy /api to it.
// VITE_API_TARGET overrides the backend origin (default localhost:8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.VITE_API_TARGET || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
});
