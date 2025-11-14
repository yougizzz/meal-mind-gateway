# Troubleshooting Auth Endpoint

## Quick Test Steps

### 1. Verify Gateway is Running
```powershell
# Check if gateway is responding
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method GET

# Check registered routes
Invoke-RestMethod -Uri "http://localhost:8081/actuator/gateway/routes" -Method GET
```

### 2. Test Auth Endpoint
```powershell
# Test login endpoint
$body = @{
    email = "test@example.com"
    password = "password123"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

### 3. Common Issues

#### Issue: Connection Refused / 503 Service Unavailable
**Cause:** Auth service is not running on port 8082

**Solution:**
- Start the auth service on port 8082
- Verify it's accessible: `Invoke-RestMethod -Uri "http://localhost:8082/auth/health" -Method GET`

#### Issue: 404 Not Found
**Cause:** Route not matching or path incorrect

**Solution:**
- Gateway route: `/api/auth/**` → forwards to `http://localhost:8082/auth/**` (prefix stripped)
- Make sure you're calling: `http://localhost:8081/api/auth/login`
- The auth service should handle: `http://localhost:8082/auth/login`

#### Issue: CORS Errors
**Cause:** CORS configuration issue

**Solution:**
- Check `CorsConfig.java` - it's configured to allow all origins
- Verify the request includes proper headers

#### Issue: Timeout
**Cause:** Auth service is slow or not responding

**Solution:**
- Check auth service logs
- Verify Redis connection (if auth service uses Redis)
- Check network connectivity

## Route Configuration

Current auth route configuration:
- **Gateway URL:** `http://localhost:8081/api/auth/**`
- **Forwarded to:** `http://localhost:8082/auth/**` (after stripping `/api` prefix)
- **No JWT validation** (public route)
- **No rate limiting** (only on protected routes)

## Debugging

### Enable Debug Logging
Add to `application.yml`:
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    com.mealmind.gateway: DEBUG
```

### Check Gateway Routes
```powershell
# View all registered routes
Invoke-RestMethod -Uri "http://localhost:8081/actuator/gateway/routes" -Method GET | ConvertTo-Json -Depth 10
```

### Test Direct Connection to Auth Service
```powershell
# Bypass gateway and test auth service directly
Invoke-RestMethod -Uri "http://localhost:8082/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"email":"test@example.com","password":"password123"}'
```

## Expected Behavior

1. Request: `POST http://localhost:8081/api/auth/login`
2. Gateway matches route `/api/auth/**`
3. Strips prefix `/api` → becomes `/auth/login`
4. Forwards to: `http://localhost:8082/auth/login`
5. Returns response from auth service

## Next Steps

If still not working:
1. Check gateway logs for errors
2. Verify auth service is running and accessible
3. Test direct connection to auth service (bypass gateway)
4. Check network/firewall settings
5. Verify port 8081 and 8082 are not blocked

