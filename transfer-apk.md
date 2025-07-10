# Easy APK Transfer to Samsung S25 Edge

## Method 1: Samsung Quick Share (Easiest)
If you have a Samsung laptop or Windows PC with Phone Link:
1. Right-click the APK file
2. Select "Share" → "Quick Share" or "Nearby Share"
3. Select your S25 Edge
4. Accept on your phone

## Method 2: Web Transfer (No USB needed)
1. On your computer, run:
   ```bash
   cd ~/src/simon-mobile
   python3 -m http.server 8080 --directory app/build/outputs/apk/debug/
   ```

2. On your S25 Edge:
   - Connect to the same WiFi as your computer
   - Open Chrome or Samsung Internet
   - Go to: `http://192.168.0.15:8080/app-debug.apk`
   - The download will start automatically

3. After download:
   - Pull down notifications
   - Tap the downloaded APK
   - Tap "Install"

## Method 3: Google Drive
1. Upload the APK:
   - Go to drive.google.com on your computer
   - Upload `app/build/outputs/apk/debug/app-debug.apk`

2. On your S25 Edge:
   - Open Google Drive app
   - Find and download the APK
   - Tap to install

## Before Installing - IMPORTANT:
1. Go to Settings → Security and privacy → Install unknown apps
2. Select your browser (Chrome/Samsung Internet)
3. Toggle ON "Allow from this source"

## APK Location on your computer:
`/home/rich/src/simon-mobile/app/build/outputs/apk/debug/app-debug.apk`

Size: 6.5 MB