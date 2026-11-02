# NutriPulse AI — Diario Nutricional Inteligente (Android)

Aplicación Android nativa desarrollada en **Kotlin** y **Jetpack Compose (Material Design 3)** para documentar y analizar tu alimentación diaria a **coste 0 €**.

## Características Principales

- **Escáner de Códigos de Barras (EAN-13 / QR) + Buscador:**
  - Lector de cámara integrado con **CameraX** y **ZXing** procesado directamente en el dispositivo.
  - Conexión en tiempo real con la API pública y gratuita de **Open Food Facts** (calorías, proteínas, carbohidratos, grasas y **Nutri-Score**).
- **Reconocimiento de Platos por Foto con IA:**
  - Integración con **Gemini 3.8 Flash Multimodal** mediante **Firebase AI Logic** y **Firebase App Check**.
  - Devuelve un desglose JSON estructurado con los ingredientes detectados, gramos estimados, calorías y macronutrientes editables antes de guardar.
- **Sincronización en la Nube y Seguridad:**
  - Inicio de sesión con **Google Sign-In** mediante **Jetpack Credential Manager**.
  - Base de datos **Firebase Firestore** aislada por usuario (`users/{userId}/mealLogs`, `dailyMetrics`, `profile`) con reglas de seguridad (`firestore.rules`) verificadas.
- **Seguimiento Diario y Calculadora de Metas:**
  - Anillo de calorías restantes/consumidas, barras de macronutrientes, control de hidratación (`+250 ml`), registro de peso corporal y calculadora automática *Mifflin-St Jeor*.

## Requisitos y Ejecución Local

1. Abre el proyecto en **Android Studio** (JDK 11+ / AGP 9.x).
2. El archivo `app/google-services.json` y `app/src/main/res/values/firebase_applet_config.xml` ya están configurados para el proyecto de Firebase.
3. Compila e instala en un dispositivo físico o emulador Android (API 24+):
   ```bash
   ./gradlew assembleDebug
   ```
