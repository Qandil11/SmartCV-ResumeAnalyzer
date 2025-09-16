# 📄 AI Resume Analyzer

AI Resume Analyzer is a **mobile-first Android app** built with Jetpack Compose and Kotlin.  
It analyzes resumes for best practices and matches them against job descriptions (JDs) to highlight gaps and improvements.

---

## ✨ Features

- 📂 Import resumes (TXT, PDF, DOCX) using Android Storage Access Framework (SAF)  
- 📋 Paste resume text directly for quick analysis  
- 🔎 Automated scoring on:
  - ✅ Action verbs
  - 📊 Quantified achievements (#, %, £)
  - 📑 Section coverage (Experience, Education, Skills)
  - 🔗 Links (LinkedIn, GitHub, Portfolio)
- 🤝 Job Description (JD) Match:
  - Compare resume vs JD keywords
  - Identify missing technical/soft skills
  - Tailored suggestions to improve alignment
- 🔐 Privacy-first: All analysis runs **fully on-device** (offline for TXT resumes; PDF/DOCX parsing requires libraries)

---

## 📸 Screenshots & Demo

<img width="280" alt="resume1" src="https://github.com/user-attachments/assets/e1e455e6-7141-477b-9c78-bba3b868bc17" />  
<img width="280" alt="resume2" src="https://github.com/user-attachments/assets/f5cb128a-c042-459d-8f55-1e9c222796d9" />

🎥 [YouTube Demo](https://youtube.com/shorts/p_aBx-PjTWk?feature=share)

---

## 🛠 Tech Stack

- **Android**: Jetpack Compose, Kotlin, Material 3  
- **Shared Core**: Kotlin Multiplatform-ready scoring & JD matching logic  
- **Libraries**:
  - [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) for PDF parsing  
  - Lightweight custom DOCX parser (no heavy Apache POI)  
- **CI/CD**: Gradle + GitHub Actions  

---

## ⚙️ Environment

- `minSdk`: 24  
- `targetSdk`: 35  
- Kotlin/JDK: Kotlin 2.x, JDK 17  

### Permissions
- None required unless importing PDF/DOCX (uses SAF `OpenDocument`).

---

## ⚡ How it Works

- **Scoring Engine**
  - ✅ Action verbs  
  - 📊 Quantified achievements (#, %, £)  
  - 📑 Section coverage (Experience, Education, Skills)  
  - 🔗 Links (LinkedIn, GitHub, Portfolio)  

- **JD Match**
  - Cleans JD + resume text  
  - Tokenizes into keywords/phrases  
  - Compares overlap & missing terms  
  - Produces a **match score** with tailored suggestions  

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

Unit tests for scoring & JD match logic live in `shared/`.

```bash
./gradlew :shared:test
```

---

## 🗺 Roadmap

- [ ] Export analysis results as PDF/CSV  
- [ ] ATS (Applicant Tracking System) compliance checks  
- [ ] Multi-language resume support  
- [ ] iOS client (via Kotlin Multiplatform)  
- [ ] Play Store release with screenshots & demo  

---

## 🛠 Troubleshooting

- ❌ **PDF/DOCX import fails?** → Ensure file permissions via SAF  
- ❌ **Crash on minSdk < 26?** → Update `minSdk` to 26 (PDFBox dependency)  
- ❌ **Gradle sync issues?** → Use latest Android Studio + Kotlin 2.x  

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
```

---

## 🏷 Badges

![Build](https://github.com/<your-username>/AIResumeAnalyzer/actions/workflows/android.yml/badge.svg)  
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)  
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)  
