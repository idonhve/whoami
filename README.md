# whoami Sites frontend

Frontend exported for the owner-requested replacement Sites deployment. The previous Site is unavailable. Java backend and database remain on Render and TiDB.

Backend: https://idonhve-whoami-api.onrender.com

Build: set VITE_API_BASE to the backend HTTPS origin, install frontend dependencies, then run node scripts/build-sites.mjs. Deploy using the Sites hosting workflow. Secrets belong only to the backend.
