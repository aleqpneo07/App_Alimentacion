# NutriPulse AI — Diario Nutricional Inteligente a Coste 0 €

Aplicación Android nativa para documentar y analizar tu alimentación diaria a coste cero, combinando el escaneo de códigos de barras/QR mediante la base de datos libre **Open Food Facts**, el análisis visual de platos por foto con **Gemini 3.8 Flash** (vía Firebase AI Logic) y el seguimiento sincronizado en la nube con **Firebase Firestore** y **Google Sign-In**.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Resumen de decisiones confirmadas para tu proyecto a coste 0 €:**
> Todos los servicios seleccionados operan dentro de capas gratuitas oficiales sin necesidad de servidores de pago ni suscripciones externas.

- **Almacenamiento y Sincronización Confirmados**: **Firebase Firestore + Autenticación con Cuenta de Google** (mediante Jetpack Credential Manager). Tu historial de comidas, hidratación, evolución de peso y objetivos se sincronizan en tiempo real de forma segura y privada por usuario.
- **Alcance Funcional Confirmado**:
  1. **Escaneo de Productos (EAN-13 / QR)**: Lector de cámara integrado con CameraX y decodificación en el dispositivo + consulta instantánea a la API abierta de **Open Food Facts** (nombre, marca, Nutri-Score, calorías y macronutrientes por 100 g o porción).
  2. **Análisis de Platos por Foto con IA**: Captura desde cámara o galería analizada con **Gemini 3.8 Flash Multimodal** (`Firebase.ai`), devolviendo un JSON estructurado con los ingredientes detectados, peso estimado en gramos, calorías totales y desglose de proteínas, carbohidratos y grasas.
  3. **Seguimiento Diario e Hidratación**: Registro organizado por comidas (*Desayuno, Comida, Cena y Snacks*), anillos de progreso calórico y de macronutrientes, contador rápido de vasos de agua y gráfica/registro de peso corporal con calculadora automática de metas (TDEE / Harris-Benedict).
- **Identidad Visual Confirmada**: **Modo Oscuro Deportivo (Athletic Dark Mode)** con superficies obsidiana profundo y acentos de alto contraste en naranja energético y verde neón.

---

## 1. Overview & Core Concept

- **Qué Hace**: Permite registrar cualquier alimento en segundos ya sea apuntando con la cámara al código de barras/QR de un producto de supermercado, fotografiando un plato casero para que la IA calcule sus calorías y macros, o buscando/añadiendo alimentos manualmente, consolidando todo en un panel diario interactivo.
- **Público Objetivo / Persona**: Usuarios que buscan llevar un control preciso, visual y sin fricciones de su nutrición, macronutrientes, hidratación y peso corporal sin pagar suscripciones mensuales de apps comerciales.
- **Valor Principal**: Unifica en una sola app nativa las funciones que suelen ser de pago en apps como *Yazio* o *Calorie Counter* (reconocimiento fotográfico por IA, escáner ilimitado de códigos y copia de seguridad en la nube) utilizando exclusivamente tecnologías gratuitas.

---

## 2. User Experience & Visual Design

### Flujos Principales de Usuario
1. **Inicio de Sesión Seguro (Google Sign-In)**: Pantalla de bienvenida deportiva donde inicias sesión con un toque mediante tu cuenta de Google (`CredentialManager`).
2. **Dashboard Diario ("Hoy")**:
   - Cabecera con selector de fecha y anillo central de **Calorías Restantes / Consumidas**, acompañado de barras de progreso para **Proteínas, Carbohidratos y Grasas**.
   - Tarjeta interactiva de **Hidratación Diaria** (+250 ml por toque) y acceso rápido al control de **Peso Corporal**.
   - Secciones desplegables para **Desayuno, Comida, Cena y Snacks** con el listado de alimentos registrados y botón rápido `+` en cada comida.
3. **Flujo 1 — Escáner de Código de Barras / QR (Open Food Facts)**:
   - Al pulsar **"Escanear Código"**, se abre el visor de cámara en tiempo real (o entrada manual de código EAN si estás en emulador sin cámara física) y consulta al instante `world.openfoodfacts.org`.
   - Muestra la ficha nutricional del producto con selector de gramos/porción antes de guardarlo en la comida elegida.
4. **Flujo 2 — Escáner de Plato por Foto con IA (Gemini Flash)**:
   - Al pulsar **"Foto con IA"**, puedes tomar una fotografía o elegir una imagen de un plato preparado.
   - El modelo `gemini-3.8-flash` analiza visualmente el plato y presenta una tarjeta desglosada con los ingredientes detectados, gramos estimados y macronutrientes editables antes de confirmar el registro.
5. **Flujo 3 — Objetivos y Evolución ("Progreso y Metas")**:
   - Configuración de tus datos físicos (peso actual, altura, edad, nivel de actividad y objetivo: *Definición, Mantenimiento o Volumen*) que calcula automáticamente tus metas diarias de kcal y macros.

### Identidad Visual y Tema (Modo Oscuro Deportivo)
- **Dirección Estética**: *High-Contrast Athletic Dark* — inspirado en relojes deportivos de alto rendimiento y paneles de telemetría fitness.
- **Paleta de Colores**:
  - `--background` / Superficie Base: Carbón Obsidiana (`#0B0E14`) y tarjetas elevadas en Gris Grafito (`#151A24`).
  - `--primary` / Acento Principal: Naranja Deportivo Vibrante (`#FF6B00`) para el anillo calórico principal, botones de acción (FAB) e indicadores activos.
  - `--secondary` / Acento Neón: Verde Voltio Neón (`#00F59B`) para objetivos cumplidos y barra de proteínas.
  - `--tertiary` / Acentos de Macros e Hidratación: Cian Eléctrico (`#00D2FF`) para hidratación/carbohidratos y Ámbar Intenso (`#FFB800`) para grasas.
- **Tipografía y Jerarquía**: Tipografía geométrica deportiva con números tabulares de gran tamaño para el marcador de calorías restantes, subtítulos claros en mayúsculas compactas para los macronutrientes y alta legibilidad en listas de alimentos.
- **Micro-interacciones y Feedback**: Barras y anillos circulares animados con transiciones fluidas (`spring`), estados de carga con esqueletos/indicadores durante el análisis de IA y mensajes claros ante códigos no encontrados o errores de red.

---

## 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Base de Datos de Productos con Open Food Facts REST API v2**
  - *Enfoque Elegido*: Integración directa vía Retrofit con la API abierta de Open Food Facts (`https://world.openfoodfacts.org/api/v2/product/{barcode}.json` y búsqueda por texto).
  - *Por Qué*: Es 100% gratuita, colaborativa, no requiere claves de API de pago y cuenta con millones de productos de supermercados españoles y europeos.
  - *Alternativas Consideradas*: APIs comerciales como Edamam o FatSecret (descartadas por límites estrictos en sus planes gratuitos o requerir tarjetas de crédito).
- **Decisión 2: Estimación Visual de Platos con Gemini 3.8 Flash + Esquema JSON Estructurado**
  - *Enfoque Elegido*: Uso de `Firebase.ai.generativeModel("gemini-3.8-flash")` con `responseSchema` estricto (`Schema.obj`) y protección de Firebase App Check.
  - *Por Qué*: Garantiza respuestas JSON limpias y deterministas (nombre del plato, lista de ingredientes con peso estimado, calorías, proteínas, carbohidratos y grasas) en pocos segundos y a coste 0 € dentro de Firebase AI Logic.
- **Decisión 3: Persistencia en la Nube con Firebase Firestore + Google Sign-In**
  - *Enfoque Elegido*: Colecciones aisladas por usuario (`users/{userId}/mealLogs`, `users/{userId}/dailyStats`, `users/{userId}/profile`) protegidas por reglas de seguridad (`firestore.rules`) verificadas localmente.
  - *Por Qué*: Evita que pierdas tus registros al cambiar de móvil o reinstalar la app, manteniendo privacidad total por cuenta de Google.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Diagrama de Arquitectura y Componentes

```
┌───────────────────────────────────────────────────────────────────────────┐
│                   INTERFAZ JETPACK COMPOSE (Modo Oscuro)                  │
│                                                                           │
│  ┌──────────────────┐   ┌───────────────────────┐   ┌──────────────────┐  │
│  │ AuthGate Screen  │   │   Dashboard Diario    │   │ Progreso y Metas │  │
│  │ (Google Sign-In) │   │ (Anillos Kcal/Macros, │   │ (Calculadora Kcal│  │
│  └────────┬─────────┘   │  Comidas, Agua, Peso) │   │  & Historial)    │  │
│           │             └───────────┬───────────┘   └────────┬─────────┘  │
│           │                         │                        │            │
│           │        ┌────────────────┴────────────────┐       │            │
│           │        ▼                                 ▼       │            │
│           │  ┌─────────────────────────┐   ┌─────────────────────────┐    │
│           │  │ Escáner Código (EAN/QR) │   │ Analizador Foto con IA  │    │
│           │  │ (CameraX + Buscador OFF)│   │ (Cámara/Galería + Revisión│   │
│           │  └────────────┬────────────┘   └────────────┬────────────┘    │
└───────────┼───────────────┼─────────────────────────────┼─────────────────┘
            │               │                             │
            ▼               ▼                             ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     CAPA DE VIEWMODELS & REPOSITORIOS                     │
│                                                                           │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │ NutritionRepository │  │ OpenFoodFactsClient │  │ PlateAiAnalyzer   │  │
│  │ (Firestore Flows &  │  │ (Retrofit HTTP GET  │  │ (Gemini 3.8 Flash │  │
│  │  CRUD por Usuario)  │  │  por EAN o Texto)   │  │  Structured JSON) │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
              ▼                        ▼                       ▼
┌─────────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│   Firebase Firestore    │ │ Open Food Facts API  │ │  Firebase AI Logic   │
│ (users/{uid}/mealLogs,  │ │ (Base de datos libre │ │  (Gemini Multimodal  │
│  dailyMetrics, profile) │ │  coste 0 €)          │ │   + App Check 0 €)   │
└─────────────────────────┘ └──────────────────────┘ └──────────────────────┘
```

### Modelo de Datos y Estado (Firestore & Dominio)
1. **Perfil y Objetivos (`users/{userId}/profile/settings`)**:
   - `userId`, `weightKg`, `targetWeightKg`, `heightCm`, `age`, `activityLevel`, `goalType` (DEFICIT, MAINTAIN, SURPLUS), `dailyCalorieGoal`, `proteinGoalG`, `carbsGoalG`, `fatGoalG`, `waterGoalMl`, `updatedAt`.
2. **Registros de Comidas (`users/{userId}/mealLogs/{logId}`)**:
   - `userId`, `dateKey` (`YYYY-MM-DD`), `mealType` (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`), `foodName`, `brandOrSource` (`OpenFoodFacts`, `Gemini AI`, `Manual`), `barcode` (opcional), `servingGrams`, `calories`, `proteinG`, `carbsG`, `fatG`, `ingredientsSummary` (opcional, generado por IA), `createdAt`, `updatedAt`.
3. **Métricas Diarias (`users/{userId}/dailyMetrics/{dateKey}`)**:
   - `userId`, `dateKey`, `waterMl`, `loggedWeightKg`, `updatedAt`.

### Mapeo de Componentes Interactivos y Gestión de Estado
- **Autenticación y Seguridad**: `MainActivity` inicializa `configureAppCheck(this, intent)` antes de la UI y gestiona el estado de sesión con `FirebaseAuth.AuthStateListener`. Ninguna consulta a Firestore se ejecuta si `currentUser == null`.
- **Escaneo de Códigos y Búsqueda**: El visor CameraX captura frames para detectar códigos EAN-13/QR (además de permitir introducir el código o buscar por nombre manualmente, ideal también para probar en el emulador). Al recibir respuesta de Open Food Facts, el usuario puede ajustar los gramos consumidos (recalculando calorías y macros en vivo) antes de guardar en Firestore.
- **Análisis Fotográfico con IA**: La imagen seleccionada se redimensiona de forma segura (< 20 MB) y se envía a `gemini-3.8-flash` con un `withTimeout(60_000L)`. El JSON devuelto rellena un formulario interactivo donde el usuario puede revisar los ingredientes identificados, ajustar las cantidades si lo desea y añadir el plato con un toque a su comida del día.
