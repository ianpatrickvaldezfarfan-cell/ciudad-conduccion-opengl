# Ciudad interactiva con conducción en OpenGL

Simulación 3D en la que se conduce un automóvil en tercera persona por una ciudad de 11 × 11 celdas para completar cinco entregas. Incluye iluminación día/noche, farolas y faros con cálculo de luz, semáforos animados, minimapa, tráfico autónomo y un panel de información dentro de la ventana.

Proyecto de la materia **Programación Gráfica** – UAGRM, FICCT.

**Tecnologías:** Java · Maven · LWJGL 3.3.6 (GLFW + OpenGL) · OpenGL 3.3 Core · GLSL 330

---

## Ejecución

**Requisitos:** JDK 17 o superior, Apache Maven y una GPU con soporte para OpenGL 3.3.

Desde la carpeta que contiene el `pom.xml`:

```bash
mvn compile exec:exec      # ejecuta la aplicación
mvn test                   # ejecuta las pruebas automáticas
```

> El `pom.xml` incluye las librerías nativas de **Windows**. En macOS o Linux hay que cambiar el classifier a `natives-macos` / `natives-linux` (y en macOS agregar `-XstartOnFirstThread`).

---

## Controles

| Tecla | Acción |
|---|---|
| W / ↑ | Acelerar |
| S / ↓ | Frenar y reversa |
| A, D / ←, → | Girar |
| Espacio | Freno fuerte |
| C | Cámara: tercera persona → aérea → panorámica |
| Q / E | Girar la vista panorámica |
| N | Día / noche |
| F | Encender / apagar faros |
| M | Mostrar / ocultar minimapa |
| R | Reiniciar el recorrido |
| H | Mostrar / ocultar ayuda |
| P | Pausa |
| ESC | Salir |

---

## Diseño de la ciudad

La ciudad se define como una matriz de caracteres (`MAPA_BASE` en `MapaCiudad.java`). Cada celda mide 10 × 10 unidades. El suelo, los límites, las colisiones, las farolas, los semáforos, los destinos, la cámara panorámica y la escala del minimapa se calculan a partir de esta misma matriz.

```
     0 1 2 3 4 5 6 7 8 9 10
 0   . . . . . . . . . 1 .
 1   . e . e . P . E . e .
 2   . . T . . . T . . . .
 3   5 p . E . e . E . p .
 4   . . . . T . . . T . .
 5   . e . P . E . e . e 2
 6   . . . 4 . . T . . . .
 7   . E . e . E . P . e .
 8   . . T . . . T . . . .
 9   S e . p . E . E . e .
10   . . . . . . . 3 . . .
```

| Símbolo | Significado |
|---|---|
| `.` | Calle |
| `E` / `e` | Manzana con edificio (`e` = con farola) |
| `P` / `p` | Parque (`p` = con farola) |
| `T` | Cruce con semáforo |
| `S` | Salida del auto |
| `1`–`5` | Destinos de entrega |

**Sectores:** Centro Histórico (el mapa original 7 × 7, filas y columnas 0–6), Distrito Este, Barrio Sur y Zona Sureste.

| Elemento | Cantidad |
|---|---|
| Manzanas con edificio | 19 |
| Parques | 6 |
| Farolas | 14 |
| Cruces con semáforo | 7 |
| Destinos | 5 (Panadería, Hospital, Escuela, Biblioteca, Estación) |

---

## Funciones

**Conducción**
- Física con aceleración, resistencia, freno y reversa; todo el movimiento usa `deltaTime`.
- Colisiones círculo–rectángulo con deslizamiento por las paredes.
- Tres cámaras con transición suave.
- Cinco entregas con marca dorada; pantalla de victoria al completarlas.

**Iluminación**
- Día y noche con luz ambiental y direccional (difusa de Lambert con normales).
- 14 farolas como luces puntuales con atenuación por distancia.
- Dos faros tipo foco que siguen la posición y orientación del auto.

**Funciones urbanas**
- Parques con árboles, bancos y fuente; edificios con ventanas que se encienden de noche; pasos peatonales.
- Semáforos animados por tiempo (verde → amarillo → rojo).
- Minimapa con viewport, scissor y proyección ortográfica propios; se adapta al redimensionar.
- Reinicio completo del recorrido con R.

**Mejoras opcionales**
- Tráfico autónomo de 5 vehículos que siguen las calles y respetan los semáforos.
- HUD con velocidad, luces, cámara, sector y destino; ayuda, pausa y pantalla de victoria.
- Letreros 3D con el nombre de cada sector.

---

## Estructura del código

```
src/main/java/com/graphics/
├── AppConduccion.java        # Aplicación principal: ventana, entrada y ciclo
└── juego/
    ├── motor/                # Renderizador (shaders), Matriz, Fuente
    ├── ciudad/               # MapaCiudad, DibujoCiudad, Letreros
    ├── conduccion/           # Auto, ModeloAuto, Camara
    ├── iluminacion/          # Iluminacion (día/noche, farolas, faros)
    ├── funciones/            # Entregas, Semaforos, Minimapa, Hud, PanelHud
    └── trafico/              # VehiculoAutonomo, Trafico
src/test/java/com/graphics/
└── ConduccionTest.java       # 10 pruebas de la lógica del juego
```

---

## Limitaciones

- No hay sombras: la luz de farolas y faros atraviesa los edificios.
- Colisiones simplificadas (el auto es un círculo; cada manzana, un rectángulo).
- La cámara no tiene colisión.
- Los semáforos no detienen al jugador (solo al tráfico autónomo).

El detalle completo está en el informe: [`docs/Informe_CiudadConduccion.pdf`](docs/Informe_CiudadConduccion.pdf).
