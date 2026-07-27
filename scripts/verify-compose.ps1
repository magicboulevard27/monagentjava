$ErrorActionPreference = "Stop"

function Show-Usage {
    @"
Usage: verify-compose.ps1 [base-url]

Runs local Docker Compose validation:
1. docker compose config
2. health and info smoke checks against the running app

Optional:
  base-url    Base URL for the app smoke checks (default: http://127.0.0.1:8080)
"@ | Write-Host
}

if ($args.Count -gt 0) {
    switch ($args[0]) {
        "-h" { Show-Usage; exit 0 }
        "--help" { Show-Usage; exit 0 }
        "help" { Show-Usage; exit 0 }
    }
}

$baseUrl = if ($args.Count -gt 0 -and -not $args[0].StartsWith("-")) {
    $args[0]
} elseif ($env:BASE_URL) {
    $env:BASE_URL
} else {
    "http://127.0.0.1:8080"
}

docker compose config | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/actuator/health" | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/actuator/info" | Out-Null
