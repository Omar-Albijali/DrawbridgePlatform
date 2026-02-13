# Drawbridge Platform | منصة Drawbridge

A comprehensive multi-platform retail and e-commerce solution built with Kotlin Multiplatform.

حل متكامل للتجارة الإلكترونية والتجزئة متعدد المنصات مبني باستخدام تقنية Kotlin Multiplatform.

---

## 🌐 Overview | نظرة عامة

**Drawbridge Platform** is designed to provide a unified experience across Web, Mobile (Android & iOS), and Desktop. It leverages a shared Kotlin core for business logic and DTOs, ensuring consistency across all clients.

تم تصميم **Drawbridge Platform** لتوفير تجربة موحدة عبر الويب (Web)، تطبيقات الجوال (Android & iOS)، وسطح المكتب (Desktop). تعتمد المنصة على نواة Kotlin مشتركة لمنطق الأعمال (Business Logic) وDTOs، مما يضمن التناسق بين جميع المنصات.

---

## ✨ Features | المميزات

- 🔐 **Authentication & User Management**: Secure login, registration, and profile management.
  **إدارة الهوية والمستخدمين**: تسجيل دخول آمن، إنشاء حساب، وإدارة الملفات الشخصية.
- 📦 **Inventory & Catalog**: Full control over products, categories, and stock levels.
  **المخزون والكتالوج**: تحكم كامل في المنتجات، التصنيفات، ومستويات المخزون (Inventory).
- 🛒 **Cart & Orders**: Seamless shopping experience and robust order processing.
  **السلة والطلبات**: تجربة تسوق سلسة ومعالجة قوية للطلبات (Orders).
- 💳 **Payment Integration**: Support for multiple payment methods and transaction tracking.
  **تكامل الدفع**: دعم خيارات دفع متعددة وتتبع العمليات (Transactions).
- 🎫 **Support System**: Integrated ticketing system for customer support.
  **نظام الدعم**: نظام تذاكر مدمج لدعم العملاء (Support Tickets).
- 📱 **Multi-platform UI**: Beautiful and responsive interfaces for all devices.
  **واجهة متعددة المنصات**: واجهات جميلة ومتجاوبة لجميع الأجهزة.

---

## 🏗️ Project Structure | هيكل المشروع

- **`server`**: Spring Boot backend application providing the REST API.
  **الجزء الخاص بالخادم (Backend)**: تطبيق مبني بـ Spring Boot يوفر REST API.
- **`webApp`**: React frontend application for the web experience.
  **تطبيق الويب (Frontend)**: تطبيق React لتجربة المتصفح.
- **`shared`**: Kotlin Multiplatform module containing shared logic, DTOs, and types.
  **الوحدة المشتركة (Shared)**: تحتوي على المنطق البرمجي المشترك، DTOs، والأنواع.
- **`composeApp`**: Shared UI code for Android, Desktop, and iOS using Compose Multiplatform.
  **واجهة الاستخدام المشتركة**: كود الواجهات المشترك لـ Android و Desktop و iOS.
- **`iosApp`**: The native iOS entry point using SwiftUI.
  **تطبيق iOS**: نقطة انطلاق التطبيق لنظام iOS باستخدام SwiftUI.

---

## 🚀 Getting Started | البدء بالعمل

### Team Local Standard (Recommended) | إعداد موحد للفريق

Use the same local ports for everyone:
استخدموا نفس المنافذ المحلية للجميع:

- **Server**: `http://localhost:8080`
- **Web App**: `http://localhost:3000`

### 1) Create local env files | إنشاء ملفات الإعداد المحلية

Linux / macOS:

```bash
./setup-env.bash
```

Windows:

```cmd
setup-env.cmd
```

Then open `server/.env` and set:
ثم افتح `server/.env` واضبط القيم التالية:

- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`

The rest can stay as default for local team development.
وباقي القيم يمكن أن تبقى افتراضية للتطوير المحلي للفريق.

### 2) Run backend | تشغيل الخادم

Linux / macOS:

```bash
./gradlew :server:bootRun
```

Windows:

```cmd
gradlew.bat :server:bootRun
```

### 3) Run web app | تشغيل تطبيق الويب

Linux / macOS:

```bash
cd webApp
npm install
npm run start
```

Windows:

```cmd
cd webApp
npm install
npm run start
```

### 4) Build when needed | البناء عند الحاجة

Linux / macOS:

```bash
./gradlew :shared:jsBrowserDevelopmentLibraryDistribution
./gradlew :server:build
cd webApp
npm run build
```

Windows:

```cmd
gradlew.bat :shared:jsBrowserDevelopmentLibraryDistribution
gradlew.bat :server:build
cd webApp
npm run build
```

### Build Commands | أوامر البناء

Use the following commands to build the different parts of the project:
استخدم الأوامر التالية لبناء الأجزاء المختلفة من المشروع:

#### 1. Shared Module | الوحدة المشتركة

Linux / macOS:

```bash
./gradlew :shared:jsBrowserDevelopmentLibraryDistribution
```

Windows:

```cmd
gradlew.bat :shared:jsBrowserDevelopmentLibraryDistribution
```

#### 2. Server | الخادم

Linux / macOS:

```bash
./gradlew server:build
```

Windows:

```cmd
gradlew.bat :server:build
```

#### 3. Web Application | تطبيق الويب

Linux / macOS:

```bash
cd webApp
npm run build
```

Windows:

```cmd
cd webApp
npm run build
```

---

## 🛠️ Development | التطوير

### Prerequisites | المتطلبات الأساسية
- **JDK 17+**
- **Node.js & npm**
- **Android Studio** or **IntelliJ IDEA**
- **Xcode** (for iOS development)

---

## 📄 License | الترخيص
Internal project for Drawbridge Platform.
مروع داخلي لمنصة Drawbridge.
