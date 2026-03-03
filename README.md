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

---

## 🚩 Project Status | حالة المشروع
I've updated the priority list for you. Every issue previously marked as Low (green) has been moved up to Medium (yellow).

### **Known Issues:**

### **1. Account Management**

* 🟢 ~~**Password change is a mocked browser alert** (no actual change).~~
* 🟢 ~~**"Forgot Password" link is a dead `#` link**.~~
* 🟢 ~~Duplicate email registration is not blocked.~~
* 🟢 ~~Email verification workflow is missing.~~
* 🟢 ~~"Remember Me" checkbox has no logic.~~
* 🟢 ~~**Avatar/Logo upload buttons do nothing**.~~
* 🟢 ~~**Server-side session invalidation on logout is missing**.~~
* 🟡 Two-Factor Authentication is missing.

### **2. Product Market**

* 🔴 **Product detail pages do not exist** (cannot see specs/descriptions).
* 🟠 Pagination is missing (performance risk for large catalogs).
* 🟠 Minimum Order Quantities (MOQ) are not displayed.
* 🟡 Category/Brand filters are hardcoded, not dynamic.
* 🟡 "Featured" sort option has no logic.
* 🟡 **Wishlist functionality is missing**.

### **3. Inventory Management**

* 🔴 **Edit (pencil) button has no click handler** (cannot adjust stock).
* 🔴 **Manual stock level adjustment is impossible**.
* 🟠 POS integration is completely missing.
* 🟡 Stock history/audit logs are missing.
* 🟡 Batch operations for multiple items are missing.
* 🟡 **Wholesalers can access the Retailer Inventory route via URL**.

### **4. Order Management**

* 🔴 **Shipping address is not saved to the order during checkout**.
* 🟠 "Download Invoice" button is a mocked alert.
* 🟠 No UI exists for entering tracking numbers or carrier info.
* 🟠 Return/Refund workflow is missing.
* 🟡 "Contact Support" button is a mocked alert.
* 🟡 Retailer delivery confirmation is missing.
* 🟡 **Re-order functionality is missing**.

### **5. Notifications**

* 🔴 **Actual notification delivery (Email/SMS/Push) is missing**.
* 🟠 Preference toggles do not save to the backend.
* 🟠 Notification bell/inbox is missing from the header.
* 🟡 Activity feed uses hardcoded mock notifications.

### **6. Dashboard**

* 🔴 **Bar and Pie charts use hardcoded static data** (not user data).
* 🟠 KPI trend percentages are commented out in code.
* 🟡 Date range filtering is missing.
* 🟡 **Manual data refresh button is missing**.

### **7. Support**

* 🔴 **Support page is a static text placeholder**.
* 🔴 **Ticket creation form is missing**.
* 🟠 Ticket history list is missing.
* 🟡 FAQ/Knowledge base is missing.

### **8. Payment Management**

* 🔴 **Real payment gateway integration is missing**.
* 🟠 Card validation is minimal (no Luhn algorithm or past-date checks).
* 🟠 Promo code "Apply" button has no handler.
* 🟡 Payment history screen is missing.

### **9. Product Management (Wholesaler)**

* 🟢 **~~"Manage Products" page is a static text placeholder~~**.
* 🟢 **~~Product creation and editing forms are missing~~**.
* 🟢 **~~Product deletion capability is missing~~**.
* 🟢 ~~Product image upload UI is missing~~.

---


