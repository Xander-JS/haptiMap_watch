# HaptiMap Watch

Navegación peatonal háptica y autónoma para Wear OS.

## Configuración local

1. Abre el proyecto en Android Studio.
2. Configura la clave de Google Maps Platform (ver abajo). Sin clave el proyecto compila igual:
   `local.defaults.properties` (versionado) aporta el marcador `DEFAULT_API_KEY`, y las búsquedas
   muestran "la clave de API no es válida".
3. Sincroniza Gradle y ejecuta `app` en un emulador o reloj Wear OS.

### Clave de API (Places API (New))

1. En Google Cloud Console, en un proyecto con facturación, habilita **Places API (New)**.
2. Crea una clave de API y restríngela:
   - **Restricción de aplicación:** apps Android, paquete `com.xanderjs.haptimap` + la huella
     SHA-1 del certificado con el que firmas. Para debug:
     `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
   - **Restricción de API:** solo Places API (New) (y Routes API a partir del paso 05).
3. Añádela a `local.properties` (en la raíz, ignorado por Git):

   ```
   MAPS_API_KEY=tu_clave
   ```

La app envía en cada petición `X-Android-Package` y `X-Android-Cert` (SHA-1 de la firma, leída
en tiempo de ejecución), así que la misma restricción vale para debug y release con solo añadir
la huella de cada certificado. Detalles en `docs/steps/04-google-places.md`.

## Estructura de módulos

| Módulo | Responsabilidad |
| --- | --- |
| `app` | Punto de entrada, inyección de dependencias (Hilt) y navegación entre pantallas |
| `core:model` | Modelos compartidos (Kotlin puro, sin Android) |
| `core:location` | Location Engine (GPS/GNSS, muestreo adaptativo) |
| `core:navigation` | Navigation Engine: máquina de estados de la sesión, ruta, maniobras, desvíos (Kotlin puro, sin Android) |
| `core:haptics` | Haptic Engine (eventos → vibración) |
| `core:network` | Conectividad, Google Places (búsqueda de lugares) y Google Routes |
| `core:intelligence` | Optimización de rutas y ranking de POIs |
| `core:designsystem` | Tokens, tema y componentes (Atomic Design) — ver `DESIGN_SYSTEM.md` |
| `feature:search` | Modo Búsqueda (voz y categorías) |
| `feature:navigation-ui` | Pantallas Eco/AOD y Mapa Continuo |
| `feature:onboarding` | Permiso de ubicación precisa y tutorial háptico |

Regla de dependencias: `feature` → `core`, nunca al revés; los módulos `core` no dependen de UI.
Capas, diagrama de módulos y máquina de estados: ver `ARCHITECTURE.md`.
