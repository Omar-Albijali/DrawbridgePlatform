@echo off
setlocal

echo Creating local environment files...

if not exist "server\.env" (
  copy "server\.env.example" "server\.env" >nul
  echo Created server\.env
) else (
  echo Skipped server\.env (already exists)
)

if not exist "webApp\.env" (
  copy "webApp\.env.example" "webApp\.env" >nul
  echo Created webApp\.env
) else (
  echo Skipped webApp\.env (already exists)
)

echo.
echo Next: open server\.env and set DB_USER, DB_PASSWORD, JWT_SECRET
endlocal
