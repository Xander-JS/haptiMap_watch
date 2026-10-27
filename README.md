# HaptiMap Watch

Navegación peatonal háptica y autónoma para Wear OS.

## Configuración local

1. Abre el proyecto en Android Studio.
2. En `local.properties` (en la raíz, no se sube a Git) añade tu clave:

   ```
   MAPS_API_KEY=tu_clave_de_google_maps
   ```

   Sin clave el proyecto compila igual; solo fallarán las llamadas a Google.
3. Sincroniza Gradle y ejecuta `app` en un emulador o reloj Wear OS.

## Estructura de módulos

| Módulo | Responsabilidad |
| --- | --- |
| `app` | Punto de entrada, ensamblado de dependencias |
| `core:model` | Modelos y estados compartidos |
| `core:location` | Location Engine (GPS/GNSS, muestreo adaptativo) |
| `core:navigation` | Navigation Engine (ruta, maniobras, desvíos) |
| `core:haptics` | Haptic Engine (eventos → vibración) |
| `core:network` | Google Routes/Places y conectividad |
| `core:intelligence` | Optimización de rutas y ranking de POIs |
| `feature:search` | Modo Búsqueda (voz y categorías) |
| `feature:navigation-ui` | Pantallas Eco/AOD y Mapa Continuo |
| `feature:onboarding` | Tutorial háptico |

Regla de dependencias: `feature` → `core`, nunca al revés; los módulos `core` no dependen de UI.
