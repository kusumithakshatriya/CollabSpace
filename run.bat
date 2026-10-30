@echo off
title CollabSpace
where mvn >nul 2>nul
if %errorlevel% neq 0 (
  echo Maven was not found. Install Java 17 and Maven, or run with Docker Desktop using: docker compose up --build
  pause
  exit /b 1
)
echo Starting CollabSpace at http://localhost:8080
call mvn spring-boot:run
pause
