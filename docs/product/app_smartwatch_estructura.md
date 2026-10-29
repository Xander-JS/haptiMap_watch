# HAPTImAP WATCH

## Navegación Táctil Autónoma para Smartwatches Wear OS

# 1. Resumen Ejecutivo

HaptiMap Watch es una aplicación de navegación peatonal desarrollada exclusivamente para Wear OS, diseñada para funcionar de manera Standalone respecto al smartphone.

Su objetivo principal es permitir que el usuario se desplace utilizando principalmente feedback háptico, reduciendo la necesidad de consultar constantemente la pantalla del reloj.

La aplicación utiliza:

		- GPS/GNSS integrado en el smartwatch.
 
		- Conectividad WiFi/LTE.
 
		- Google Maps Platform para mapas, rutas y lugares.
 
		- Un motor propio de navegación.
 
		- Un motor háptico para transmitir instrucciones mediante vibraciones.
 
		- Un módulo inteligente encargado de optimizar rutas y seleccionar oportunidades relevantes de puntos de interés (POIs).

El sistema contempla dos perfiles principales de navegación:

		- Modo Focus: navegación directa hacia un destino.
 
		- Modo Tour: navegación orientada a la exploración, incorporando POIs relevantes durante el trayecto.

Además, incorpora un Modo Búsqueda, que permite introducir destinos mediante voz o seleccionar rápidamente categorías de lugares.

# 2. Principios Fundamentales del Producto

HaptiMap Watch se construye sobre cinco principios:

### 2.1 Standalone respecto al smartphone

La aplicación no requiere que el usuario tenga un teléfono conectado para ejecutar la navegación.

Toda la lógica principal de la aplicación se ejecuta en el smartwatch.

Standalone no significa funcionamiento completamente offline.

La aplicación depende de conectividad para acceder a determinados servicios externos, principalmente Google Maps Platform.

### 2.2 Conectividad como requisito fundamental

La conexión WiFi o LTE permite obtener:

		- Mapas.
 
		- Rutas.
 
		- Información de lugares.
 
		- Actualizaciones de POIs.
 
		- Nuevos cálculos de ruta.
 
		- Información necesaria para las funciones inteligentes.

En caso de pérdida temporal de conexión, el sistema debe intentar mantener la navegación utilizando la información de la ruta previamente cargada.

Sin conexión:

		- Una ruta ya cargada puede continuar ejecutándose.
 
		- No se podrán garantizar nuevas búsquedas.
 
		- No se podrán garantizar nuevos cálculos de ruta.
 
		- Las recomendaciones de nuevos POIs quedarán suspendidas.
 
		- El sistema deberá informar internamente del estado de conectividad.

### 2.3 Interacción principalmente háptica

La pantalla complementa la navegación, pero no constituye el canal principal de instrucciones.

El usuario debe poder comprender las maniobras fundamentales mediante patrones de vibración.

### 2.4 Inteligencia como capa de optimización

La inteligencia del sistema no sustituye al motor de navegación.

Google Maps proporciona información geográfica, rutas y lugares.

HaptiMap utiliza esa información para:

		- Evaluar rutas.
 
		- Optimizar alternativas.
 
		- Priorizar POIs.
 
		- Determinar posibles desvíos.
 
		- Adaptar las recomendaciones al contexto del trayecto.

### 2.5 Diseño orientado a bajo consumo

El sistema debe adaptar:

		- Frecuencia de GPS.
 
		- Actualización del mapa.
 
		- Renderizado.
 
		- Consultas externas.
 
		- Procesamiento interno.

al contexto de navegación para reducir el consumo energético del smartwatch.

# 3. Arquitectura General del Sistema

                         INTERNET
 
                            │
 
             ┌──────────────┴──────────────┐
 
             │                             │
 
       Google Routes API             Google Places API
 
             │                             │
 
             └──────────────┬──────────────┘
 
                            │
 
                       WiFi / LTE
 
                            │
 
                ┌───────────▼───────────┐
 
                │     HAPTImAP WATCH    │
 
                │        Wear OS        │
 
                ├───────────────────────┤
 
                │                       │
 
                │   Location Engine     │
 
                │   Navigation Engine   │
 
                │   Intelligence Layer  │
 
                │   POI Manager         │
 
                │   Haptic Engine       │
 
                │   UI / State Manager  │
 
                │   Connectivity Layer  │
 
                │   Route Cache         │
 
                │                       │
 
                └───────────┬───────────┘
 
                            │
 
                  ┌─────────┴─────────┐
 
                  │                   │
 
                GPS/GNSS          Haptic Motor
 
                  │                   │
 
                  └─────────┬─────────┘
 
                            │
 
                          USER

# 4. Servicios Externos

## 4.1 Google Maps Platform

Google Maps Platform será la fuente principal de información geográfica.

La integración contempla principalmente:

### Google Routes API

Responsable de:

		- Cálculo de rutas peatonales.
 
		- Distancias.
 
		- Duración estimada.
 
		- Segmentos de navegación.
 
		- Maniobras.
 
		- Recálculo de rutas.

### Google Places API

Responsable de:

		- Búsqueda de establecimientos.
 
		- Información de POIs.
 
		- Categorías.
 
		- Ubicación.
 
		- Distancia.
 
		- Información contextual disponible.

La aplicación no debe delegar toda la lógica de producto a estos servicios.

Google proporciona los datos necesarios y HaptiMap decide cómo utilizarlos.

# 5. Capas Principales de la Aplicación

## 5.1 Location Engine

Gestiona la ubicación física del usuario.

Responsabilidades:

		- Obtener posición GPS/GNSS.
 
		- Determinar precisión.
 
		- Detectar cambios de posición.
 
		- Detectar desplazamiento.
 
		- Proporcionar información al motor de navegación.
 
		- Ajustar la frecuencia de actualización según el contexto.

Debe aplicar estrategias de muestreo adaptativo para reducir consumo energético.

## 5.2 Navigation Engine

Es responsable de ejecutar la navegación.

Recibe:

		- Ruta calculada.
 
		- Posición actual.
 
		- Segmento activo.
 
		- Maniobra siguiente.
 
		- Estado de navegación.

Responsabilidades:

		- Seguir la ruta.
 
		- Determinar el siguiente evento.
 
		- Calcular distancia hasta la maniobra.
 
		- Detectar desviaciones.
 
		- Solicitar recálculo cuando sea necesario.
 
		- Determinar llegada al destino.

El Navigation Engine no depende directamente de la interfaz gráfica.

# 6. Intelligence Layer

La inteligencia constituye una capa independiente del Navigation Engine.

Su función es analizar información y tomar decisiones de optimización, no controlar directamente el hardware.

## 6.1 Optimización de rutas

Cuando existen diferentes alternativas de navegación, el sistema puede evaluar factores como:

		- Distancia.
 
		- Tiempo estimado.
 
		- Número de maniobras.
 
		- Complejidad del trayecto.
 
		- Presencia de POIs relevantes.
 
		- Desvío necesario.
 
		- Contexto del Modo Focus o Modo Tour.

La inteligencia selecciona la alternativa que mejor se adapte al objetivo del usuario.

## 6.2 Inteligencia sobre POIs

En Modo Tour, el sistema analiza los POIs cercanos a la trayectoria.

Puede evaluar:

		- Distancia desde la ruta.
 
		- Distancia adicional necesaria.
 
		- Relevancia del POI.
 
		- Categoría.
 
		- Posición respecto al recorrido.
 
		- Impacto sobre el tiempo total del trayecto.

El objetivo no es mostrar todos los lugares disponibles, sino seleccionar aquellos que realmente puedan aportar valor al recorrido.

# 7. Modo Búsqueda

El Modo Búsqueda constituye el punto inicial de interacción.

Su diseño debe estar optimizado para una pantalla pequeña.

## 7.1 Entrada por voz

El usuario puede introducir:

		- Direcciones.
 
		- Nombres de establecimientos.
 
		- Lugares concretos.

La aplicación transforma la entrada de voz en una búsqueda de destino.

## 7.2 Catálogo de categorías

Para búsquedas rápidas se dispone de categorías predefinidas:

TIENDA

FERRETERIA

RESTAURANTE

CAFETERIA

FARMACIA

TRANSPORTE

SUPERMERCADO

Estas categorías funcionan como accesos rápidos para consultar POIs cercanos.

## 7.3 Flujo de búsqueda

Modo Búsqueda

      │
 
      ├── Voz
 
      │    ↓
 
      │  Destino
 
      │
 
      └── Categoría
 
           ↓
 
       Google Places
 
           ↓
 
       POIs cercanos
 
           ↓
 
        Selección
 
           ↓
 
    Perfil de navegación
 
       ┌────┴────┐
 
       ↓         ↓
 
    Focus      Tour

# 8. Modo Focus

## Propósito

Proporcionar una navegación directa, clara y eficiente hacia el destino seleccionado.

## Comportamiento

El sistema:

		- Obtiene el destino.
 
		- Solicita una ruta peatonal.
 
		- Analiza las alternativas disponibles.
 
		- Selecciona la ruta apropiada.
 
		- Inicia la navegación.
 
		- Proporciona instrucciones hápticas.
 
		- Detecta desviaciones.
 
		- Recalcula automáticamente cuando sea necesario.
 
		- Notifica la llegada.

En este modo se eliminan las sugerencias secundarias de POIs.

### Principio

El usuario quiere llegar al destino. El sistema debe evitar distracciones.

# 9. Modo Tour

## Propósito

Transformar la navegación en una experiencia de exploración.

El usuario mantiene un destino principal, pero el sistema puede identificar POIs relevantes que puedan incorporarse al recorrido.

## 9.1 Radio dinámico

El sistema no utiliza únicamente la posición GPS actual como centro permanente de búsqueda.

Utiliza el segmento activo de la ruta como referencia.

Conceptualmente:

          POI
 
           ●
 
           │
 
           │ radio permitido
 
           │

───────────●────────────────

        Ruta activa
 
           ▲
 
           │
 
        Usuario

El sistema evalúa lugares próximos al trayecto y determina si su incorporación representa un desvío razonable.

## 9.2 Evaluación inteligente del POI

Un POI puede ser descartado si:

		- Se encuentra demasiado lejos.
 
		- Requiere un desvío excesivo.
 
		- Aumenta demasiado el tiempo del recorrido.
 
		- Tiene baja relevancia.
 
		- Ya fue rechazado anteriormente durante el trayecto.

Puede ser priorizado si:

		- Está próximo.
 
		- Requiere poco desvío.
 
		- Es relevante para el contexto.
 
		- Aporta valor al recorrido.
 
		- La desviación resulta razonable.

# 10. Sistema de Sugerencias de POI

Cuando el sistema identifica una oportunidad relevante:

POI detectado

      ↓

Evaluación inteligente

      ↓

¿Es relevante?

┌──┴──┐

NO     SÍ

│      │

│   Alerta háptica

│      │

│   Esperar respuesta

│      │

│   ┌──┴──┐

│ Acepta  Timeout

│   │       │

│   ↓       ↓

│ Desvío   Continuar

│

└──────────────→ Ruta principal

El usuario puede aceptar la sugerencia mediante el control físico disponible en el dispositivo.

Si no responde dentro del intervalo configurado, la sugerencia se descarta automáticamente.

El intervalo recomendado inicialmente será de 5 a 10 segundos.

# 11. Motor Háptico

El Haptic Engine traduce eventos de navegación en patrones físicos.

Su responsabilidad es abstraer el hardware de vibración para que el resto de la aplicación trabaje con eventos semánticos.

Por ejemplo:

TURN_LEFT

TURN_RIGHT

CONTINUE

TURN_IMMINENT

ARRIVAL

OFF_ROUTE

POI_SUGGESTION

El motor convierte posteriormente estos eventos en patrones específicos de vibración.

# 12. Lenguaje Háptico

| Evento | Patrón | Significado |
| --- | --- | --- |
| Giro izquierda | 2 pulsos medios | Girar a la izquierda |
| Giro derecha | 3 pulsos medios | Girar a la derecha |
| Continuar | 1 pulso suave | Continuar |
| Giro inminente | Pulsos progresivos | Maniobra próxima |
| Llegada | Pulso largo + doble toque | Destino alcanzado |
| Ruta perdida | Pulso largo intenso | Recálculo |
| Sugerencia POI | Ráfaga diferenciada | Existe una oportunidad de exploración |

Los patrones deberán mantenerse consistentes durante toda la experiencia.

# 13. Anticipación de Maniobras

La navegación debe anticipar las maniobras en lugar de notificarlas únicamente cuando el usuario llegue al punto exacto.

Como referencia inicial:

		- Primera indicación: aproximadamente 15–20 metros antes.
 
		- Refuerzo progresivo: durante los últimos metros.
 
		- Intensificación: cuando la maniobra sea inminente.

Estos valores deberán ajustarse mediante pruebas reales de uso.

La distancia de anticipación no será necesariamente fija: podrá variar según velocidad, tipo de maniobra y contexto.

# 14. Detección de Desvíos

El sistema debe comparar continuamente:

Posición GPS actual

        ↓

Ruta esperada

        ↓

¿Está dentro del margen?

      ┌──┴──┐
 
     SÍ     NO
 
      │      │
 
      │   Desvío
 
      │      ↓
 
      │  Alerta háptica
 
      │      ↓
 
      │ Recálculo de ruta
 
      │      ↓
 
      └── Nueva ruta

El recálculo debe ejecutarse automáticamente cuando exista conectividad.

Si se pierde la conexión durante el trayecto, el sistema continuará utilizando la ruta previamente cargada mientras sea posible y deberá gestionar el estado de navegación hasta recuperar conectividad.

# 15. Modos de Visualización

La aplicación contempla dos estrategias de visualización.

## Modo A — Eco / AOD

Diseñado para reducir consumo energético.

Muestra únicamente:

		- Dirección de la próxima maniobra.
 
		- Distancia aproximada.
 
		- Estado esencial de navegación.

La pantalla puede permanecer en un estado visual minimalista y activarse ante eventos importantes.

## Modo B — Mapa Continuo

Modo de navegación visual completo.

Muestra:

		- Mapa.
 
		- Posición del usuario.
 
		- Ruta.
 
		- Próxima maniobra.
 
		- Información contextual.

El mapa se actualiza dinámicamente siguiendo la posición del usuario.

# 16. Gestión Energética

El sistema debe controlar el consumo mediante:

		- Muestreo GPS adaptativo.
 
		- Reducción del renderizado cuando sea posible.
 
		- Consultas externas únicamente cuando sean necesarias.
 
		- Uso eficiente de actualizaciones de POIs.
 
		- Gestión inteligente del estado de pantalla.
 
		- Conservación de la ruta cargada localmente.

La optimización energética será una parte transversal del sistema y no una funcionalidad aislada.

# 17. Conectividad

La conectividad se considera una dependencia fundamental de HaptiMap Watch.

Estados principales:

CONNECTED

│

├── Navegación normal

├── Búsqueda

├── Routing

└── POIs



DISCONNECTED

│

├── Ruta previamente cargada → continuar

├── Nueva búsqueda → no disponible

├── Nueva ruta → no disponible

└── Nuevos POIs → no disponible

La aplicación deberá detectar y gestionar los cambios de conectividad sin interrumpir innecesariamente una navegación activa.

# 18. Persistencia y Caché de Ruta

Una vez calculada una ruta, la información necesaria para continuar la navegación deberá conservarse temporalmente en el dispositivo.

Esto permite:

		- Mantener la navegación ante una pérdida temporal de Internet.
 
		- Mantener las instrucciones de la ruta actual.
 
		- Evitar depender constantemente de una nueva consulta.

Sin embargo, la ruta almacenada no debe considerarse una solución completa de navegación offline.

El sistema necesitará conectividad para generar nuevos cálculos y acceder a información actualizada.

# 19. Onboarding

Durante el primer uso se presenta un tutorial breve.

## Tutorial háptico

El usuario aprende:

		- Giro izquierda.
 
		- Giro derecha.
 
		- Continuar.
 
		- Alerta de ruta.
 
		- Sugerencia de POI.
 
		- Llegada.

Cada patrón debe reproducirse de forma individual para que el usuario pueda asociar:

Patrón → Significado → Acción

El tutorial debe poder repetirse posteriormente desde configuración.

# 20. Máquina de Estados Principal

La aplicación puede estructurarse alrededor de estados de navegación claramente definidos:

IDLE

│

▼

SEARCHING

│

▼

DESTINATION_SELECTED

│

▼

ROUTE_LOADING

│

▼

NAVIGATION_ACTIVE

│

├── OFF_ROUTE

│      ↓

│   RECALCULATING

│      ↓

│   NAVIGATION_ACTIVE

│

├── POI_DETECTED

│      ↓

│   POI_SUGGESTION

│      ├── ACCEPTED → DETOUR

│      └── TIMEOUT  → NAVIGATION_ACTIVE

│

└── ARRIVAL

        ↓
 
      COMPLETED

Esta máquina de estados permite mantener separada la lógica de navegación de la interfaz.

# 21. Separación de Responsabilidades

La arquitectura deberá mantener una separación clara:

### Google Maps Platform

Proporciona:

		- Mapas.
 
		- Rutas.
 
		- Lugares.
 
		- Información geográfica.

### Location Engine

Proporciona:

		- Posición.
 
		- Movimiento.
 
		- Precisión.

### Navigation Engine

Gestiona:

		- Ejecución de la ruta.
 
		- Maniobras.
 
		- Desvíos.
 
		- Llegada.

### Intelligence Layer

Gestiona:

		- Optimización.
 
		- Evaluación de alternativas.
 
		- Priorización de POIs.
 
		- Decisiones de exploración.

### Haptic Engine

Gestiona:

		- Patrones de vibración.
 
		- Intensidad.
 
		- Cadencia.
 
		- Eventos táctiles.

### UI Layer

Gestiona:

		- Pantallas.
 
		- Interacciones.
 
		- Información visual.
 
		- Estados visuales.

Ninguna de estas capas debería asumir responsabilidades pertenecientes a otra.

# 22. Hoja de Ruta Técnica

## Fase 1 — Base Wear OS

		- Configuración del proyecto.
 
		- Arquitectura base.
 
		- Acceso a GPS/GNSS.
 
		- Gestión de permisos.
 
		- Sistema de estados.
 
		- Conectividad.

## Fase 2 — Integración Google Maps

		- Integración de Google Routes API.
 
		- Integración de Google Places API.
 
		- Consulta de rutas peatonales.
 
		- Consulta de POIs.
 
		- Gestión de errores y conectividad.
 
		- Caché temporal de rutas.

## Fase 3 — Motor de Navegación

		- Seguimiento de ruta.
 
		- Detección de maniobras.
 
		- Anticipación.
 
		- Detección de desvíos.
 
		- Recálculo automático.
 
		- Detección de llegada.

## Fase 4 — Motor Háptico

		- Abstracción del hardware.
 
		- Patrones de vibración.
 
		- Eventos semánticos.
 
		- Calibración.
 
		- Tutorial háptico.

## Fase 5 — Modo Focus

		- Flujo completo punto a punto.
 
		- Navegación directa.
 
		- Supresión de POIs.
 
		- Optimización del recorrido.

## Fase 6 — Modo Tour

		- Descubrimiento de POIs.
 
		- Radio dinámico.
 
		- Evaluación de desvíos.
 
		- Sistema de sugerencias.
 
		- Timeout.
 
		- Aceptación mediante control físico.

## Fase 7 — Intelligence Layer

		- Evaluación de rutas.
 
		- Optimización.
 
		- Ranking de POIs.
 
		- Contextualización de recomendaciones.
 
		- Mejora progresiva de decisiones.

## Fase 8 — Optimización

		- Consumo energético.
 
		- GPS adaptativo.
 
		- Actualización de mapas.
 
		- Gestión de memoria.
 
		- Gestión de conectividad.
 
		- Optimización del renderizado.

# 23. Objetivo Final del Producto

HaptiMap Watch no busca convertirse simplemente en un mapa pequeño colocado en la muñeca.

Su objetivo es crear una experiencia de navegación donde:

El usuario conoce hacia dónde avanzar sin tener que mirar constantemente el reloj.

La pantalla proporciona contexto.

El GPS proporciona posición.

Google Maps proporciona información geográfica.

La inteligencia decide qué información es relevante.

El Navigation Engine ejecuta el recorrido.

Y el Haptic Engine convierte todo ello en un lenguaje táctil comprensible.

El resultado esperado es una experiencia de navegación autónoma, discreta, intuitiva y centrada en la interacción háptica, diseñada específicamente para las limitaciones y posibilidades de un smartwatch Wear OS.