# 📄 AI Resume Analyzer

AI Resume Analyzer is a mobile-first Android app built with Jetpack Compose and Kotlin.  
It analyzes resumes for best practices and matches them against job descriptions (JDs) to highlight gaps and improvements.

---

## ✨ Features

- Import resumes (TXT, PDF, DOCX) using Android Storage Access Framework (SAF)
- Paste resume text directly for quick analysis
- Automated scoring on:
  - ✅ Action verbs
  - 📊 Quantified achievements (#, %, £)
  - 📑 Section coverage (Experience, Education, Skills)
  - 🔗 Links (LinkedIn, GitHub, Portfolio)
- Job Description (JD) Match:
  - Compare resume vs JD keywords
  - Identify missing technical/soft skills
  - Tailored suggestions to improve alignment
- Built fully on-device → No data leaves your phone
- Works offline for TXT resumes (PDF/DOCX parsing requires libraries)

---

## 📸 Screenshots

> _(Add screenshots here once available — e.g., analysis results screen, JD match screen)_

---

## 🛠 Tech Stack

- **Android**: Jetpack Compose, Kotlin, Material 3
- **Shared Core**: Kotlin Multiplatform-ready scoring & JD matching logic
- **Libraries**:
  - [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) for PDF parsing
  - Lightweight custom DOCX parser (no heavy Apache POI)
- **Build Tools**: Gradle, GitHub Actions (CI)

---

## ⚙️ Environment

- `minSdk`: 24  
- `targetSdk`: 35  
- Java/Kotlin: JDK 17, Kotlin JVM target 17  

### Permissions
- None required unless enabling PDF/DOCX import (uses SAF `OpenDocument`).

---

## ⚡ How it Works

- **Scoring**: checks for resume best practices
  - Action verbs ✅  
  - Quantified achievements (#, %, £)  
  - Section coverage (Experience, Education, Skills)  
  - Links (LinkedIn, GitHub, Portfolio)  

- **JD Match**:
  - Cleans JD + resume text
  - Tokenizes into keywords/phrases
  - Compares overlap & missing terms
  - Produces a match score + tailored suggestions

---

## 📂 Project Structure

```
app/      # Android UI (Compose)
shared/   # Shared pure-Kotlin core (Scoring, JDMatch, models)
```

---

## 🚀 Build & Run

```bash
# Clone the repo
git clone https://github.com/<your-username>/AIResumeAnalyzer.git
cd AIResumeAnalyzer

# Build debug APK
./gradlew :app-android:assembleDebug

# Install on connected device
adb install app-android/build/outputs/apk/debug/app-android-debug.apk
```

---

## 🧪 Testing

- Unit tests for scoring & JD match logic live in `shared/`  
- Run with:

```bash
./gradlew :shared:test
```

---

## 🗺 Roadmap

- [ ] Export analysis results as PDF/CSV  
- [ ] Add ATS (Applicant Tracking System) compliance checks  
- [ ] Multi-language resume support  
- [ ] iOS client (via Kotlin Multiplatform)  
- [ ] Play Store release with screenshots & video demo  

---

## 🛠 Troubleshooting

- **PDF/DOCX import fails?** → Ensure file permissions granted via SAF  
- **App crashes on minSdk < 26?** → Update `minSdk` to 26 (PDFBox requires Android O)  
- **Gradle sync issues?** → Use latest Android Studio + Kotlin 2.x  

---

## 🤝 Contributing

1. Fork the repo  
2. Create a feature branch (`git checkout -b feature/my-feature`)  
3. Commit changes (`git commit -m "Add new feature"`)  
4. Push branch (`git push origin feature/my-feature`)  
5. Open a Pull Request  

---

## 📜 License

```
Apache License 2.0

Copyright (c) 2025 Qandil Tariq

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

## 🏷 Badges (Optional)

![Build](https://github.com/<your-username>/AIResumeAnalyzer/actions/workflows/android.yml/badge.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)
