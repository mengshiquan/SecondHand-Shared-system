---
name: run-dev
description: Start/stop/restart the SecondHand project dev environment (frontend Vite + backend Spring Boot). Triggers when user asks to run, start, launch, restart, or stop the app.
---

# Run Dev Environment — 校园二手平台

## Prerequisites Check
```bash
# Verify Java 17 exists
ls D:/java/jdk17 2>/dev/null && echo "JDK OK" || echo "JDK MISSING"

# Verify MySQL is running
netstat -ano | grep 3306 > /dev/null && echo "MySQL OK" || echo "MySQL NOT RUNNING"

# Verify Maven wrapper
ls backend/mvnw 2>/dev/null && echo "Maven OK" || echo "Maven MISSING"
```

## Start Backend
```bash
# Terminal 1 — Backend (port 8080)
export JAVA_HOME=D:/java/jdk17
cd backend
./mvnw spring-boot:run
```
Wait for `Started SecondHandApplication` log line before testing APIs.

## Start Frontend
```bash
# Terminal 2 — Frontend (port auto-assigned, typically 5173)
cd frontend
npm run dev
```
Dev server URL printed in output. Check network tab for exact port.

## Stop All
```bash
# Kill Spring Boot
pkill -f "spring-boot:run" 2>/dev/null
# Kill Vite
pkill -f "vite" 2>/dev/null
```

Or find processes:
```bash
netstat -ano | grep -E "8080|5173|5174|5175"
# Then: taskkill /PID <pid> /F
```

## Port Reference
| Service | Default Port | Notes |
|---------|-------------|-------|
| Backend (Spring Boot) | 8080 | Configured in `application.yml` |
| Frontend (Vite) | 5173 | Auto-increments to 5174/5175 if busy |
| Frontend proxy to backend | `/api` → `localhost:8080` | Configured in `vite.config.js` |
| MySQL | 3306 | Standard |

## Vite Config (`frontend/vite.config.js`)
```javascript
server: {
  host: true,        // Allows LAN/ngrok access
  proxy: { '/api': 'http://localhost:8080' },
  allowedHosts: true // Required for ngrok/remote access
}
```

## Quick Verify
```bash
# Check backend health
curl http://localhost:8080/api/product/list?pageNum=1&pageSize=10 2>/dev/null

# Check frontend
curl http://localhost:5173 2>/dev/null | head -1
```

## Common Issues
- **Backend won't start**: Check MySQL is running, `JAVA_HOME` set to `D:/java/jdk17`
- **Frontend proxy error**: Backend must be running on 8080 before frontend can proxy `/api`
- **Port already in use**: `netstat -ano | grep <port>` then `taskkill /PID <pid> /F`
- **CORS errors**: Backend `CorsConfig.java` allows `http://localhost:5173`
- **Hot reload not working**: Clear `node_modules/.vite` cache and restart
