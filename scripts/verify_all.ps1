# Cadence Local Full-Stack Automated Verification Script (100% Free & Offline)
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " RUNNING CADENCE FULL-STACK LOCAL VERIFICATION" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. AI Service Pytest & Evals
Write-Host "`n[1/3] Running AI Service Tests (FastAPI + OR-Tools + Evals)..." -ForegroundColor Yellow
$pyExe = Join-Path $PSScriptRoot "..\ai-service\.venv\Scripts\python.exe"
if (-not (Test-Path $pyExe)) {
    $pyExe = "python"
}
& $pyExe -m pytest "ai-service\tests" -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "AI Service tests failed!" -ForegroundColor Red
    exit 1
}
& $pyExe "ai-service\evals\run_full_benchmark.py"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Evaluation benchmark failed!" -ForegroundColor Red
    exit 1
}

# 2. Spring Boot Backend Tests
Write-Host "`n[2/3] Running Backend Tests (Java 21 + Spring Boot + FSRS-4.5)..." -ForegroundColor Yellow
Push-Location "backend"
try {
    .\mvnw.cmd test -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Backend tests failed!" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

# 3. Next.js Web Build
Write-Host "`n[3/3] Running Web Production Build (Next.js 15 + TypeScript)..." -ForegroundColor Yellow
Push-Location "web"
try {
    npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Web build failed!" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host " ALL CADENCE VERIFICATION CHECKS PASSED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
