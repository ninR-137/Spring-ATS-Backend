Set-StrictMode -Version Latest

param(
	[switch]$RotateSecret
)

# Ensure we run from the script's directory (project root)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $scriptDir

# Update or add only the JWT_SECRET_KEY line in .env without overwriting other keys
$envPath = Join-Path $scriptDir '.env'
if (-not (Test-Path $envPath)) {
	New-Item -Path $envPath -ItemType File -Force | Out-Null
}

$content = Get-Content -Raw -Encoding UTF8 -ErrorAction SilentlyContinue $envPath
if ($null -eq $content) { $content = "" }

$lines = @()
if ($content -ne "") {
	$lines = $content -split "`r?`n"
}

$pattern = '^[ \t]*JWT_SECRET_KEY\s*=\s*(.+)\s*$'
$existingSecret = $null
$existingIndex = -1
$existingMatch = $null

for ($i = 0; $i -lt $lines.Length; $i++) {
	if ($lines[$i] -match $pattern) {
		$existingSecret = $matches[1].Trim()
		$existingIndex = $i
		$existingMatch = $lines[$i]
		break
	}
}

$secret = $existingSecret
if ($RotateSecret -or [string]::IsNullOrWhiteSpace($secret)) {
	# Generate a 32-byte cryptographically secure random secret and encode as Base64
	$bytes = New-Object 'System.Byte[]' 32
	[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
	$secret = [System.Convert]::ToBase64String($bytes)
}

$found = $false
for ($i = 0; $i -lt $lines.Length; $i++) {
	if ($i -eq $existingIndex) {
		$lines[$i] = "JWT_SECRET_KEY=$secret"
		$found = $true
		break
	}
}
if (-not $found) {
	$lines += "JWT_SECRET_KEY=$secret"
}

# Write back preserving other lines
$lines -join "`r`n" | Out-File -FilePath $envPath -Encoding UTF8
if ($RotateSecret) {
	Write-Host "Rotated JWT_SECRET_KEY in $envPath"
} elseif ($null -ne $existingSecret -and $existingSecret.Length -gt 0) {
	Write-Host "Reused existing JWT_SECRET_KEY from $envPath"
} else {
	Write-Host "Generated JWT_SECRET_KEY in $envPath"
}

# Export the secret into the current PowerShell session so Gradle/Java can read it
$env:JWT_SECRET_KEY = $secret
Write-Host "Exported JWT_SECRET_KEY for this session."

# Start the Spring Boot application using the Gradle wrapper
Write-Host "Starting application with .\gradlew.bat bootRun (logs will stream here)"
& .\gradlew.bat bootRun
