# Paipay DataLogger

Aplicación Android (Java) de registro de datos con **sincronización diferida** para el
recinto **Paipayales**, cantón Santa Lucía, provincia del Guayas.

Proyecto de Prácticas Comunitarias — ESPOL
**PG15-PY26-04** · *Acuicultura y TICs: Expansión Tecnológica en el Cantón Santa Lucía y Daule*
Componente: **Desarrollo de Aplicación Móvil con Sincronización Diferida (JAVA)**

---

## 1. Qué resuelve

En Paipayales la señal es intermitente y el 16 % de la población rural no tiene acceso a
internet. Una app que exija conexión para guardar un dato no sirve en campo.

La solución es **guardar primero, subir después**:

```
   Productor en la piscina              Punto con señal
   ┌───────────────────┐                ┌──────────────────────┐
   │ Formulario visual │  ──guarda──►   │ SQLite local (Room)  │
   │ (sin internet)    │                │ sincronizado = 0     │
   └───────────────────┘                └──────────┬───────────┘
                                                   │ [Sincronizar]
                                                   │ + confirmación
                                                   ▼
                                        ┌──────────────────────┐
                                        │  Neon Data API       │
                                        │  (PostgreSQL)        │
                                        └──────────────────────┘
```

El dato **nunca se borra del teléfono**: sincronizar es copiar, no mover.

---

## 2. Las cuatro tareas del proyecto, y dónde están en el código

| Tarea | Implementación |
|---|---|
| **Diseño de Interfaz Intuitiva (UI/UX)** — formularios de fecha, piscina, peso, talla y agua | `ui/registro/` + `res/layout/fragment_form_*.xml` |
| **Biometría individual** — un registro por pez, agrupados en muestreos por día | `ui/registro/BiometriaFragment.java`, `util/FechaUtil.codigoMuestreo()` |
| **Sistema de Sincronización** — de base local a remota al detectar internet | `sync/SincronizacionRepositorio.java`, `sync/SincronizacionWorker.java`, `ui/sync/SincronizacionFragment.java` |
| **Consolidación de Fuentes** — datos in situ + ensayos de laboratorio | `ui/registro/LaboratorioFragment.java` + vista `v_consolidado_fuentes` en `sql/04_vistas_analisis.sql` |
| **Semáforo de Alertas** (backend y frontend en Java) | `domain/EvaluadorSemaforo.java` (lógica) + `ui/semaforo/` (interfaz) |

---

## 3. Puesta en marcha

### 3.1 Requisitos

- Android Studio Ladybug (2024.2) o posterior
- JDK 17 (viene incluido en Android Studio)
- Android SDK 35 · minSdk 24 (Android 7.0, cubre los equipos de gama baja del recinto)

### 3.2 Preparar Neon

1. Crear un proyecto en <https://console.neon.tech>.
2. En la barra lateral, **Data API** → seleccionar la rama → marcar
   **Use Managed Better Auth** y **Grant public schema access** → *Enable Data API*.
3. Ejecutar en el **SQL Editor**, en este orden:
   - `sql/01_esquema_neon.sql`
   - `sql/02_permisos_y_rls.sql`
   - `sql/03_datos_iniciales.sql`
   - `sql/04_vistas_analisis.sql`

   Los otros dos scripts son casos especiales y **no** forman parte del montaje normal:

   | Script | Cuándo |
   |---|---|
   | `00_reiniciar_desde_cero.sql` | El esquema quedó a medias y prefieres empezar limpio. **Borra todos los datos.** Ejecutarlo antes del `01`. |
   | `05_migracion_medicion_individual.sql` | Actualizar una base con datos que NO quieres perder, creada con el esquema anterior. Nunca después de un reinicio. |

   > `01` usa `CREATE TABLE IF NOT EXISTS`, que **no modifica** una tabla ya existente. Si
   > el esquema cambió, volver a ejecutar `01` no añade las columnas nuevas y `04` fallará
   > al crear vistas que las usan. Para eso están `00` (borrar y recrear) y `05` (migrar).
4. Volver a **Data API** y pulsar **Refresh schema cache**.
   *(Si se salta este paso, la app recibirá errores 404 aunque las tablas existan.)*
5. Anotar dos URL:
   - **API URL** (página Data API) — termina en `/rest/v1/`
   - **Auth URL** (página Auth → Configuration) — termina en `/auth/`
6. En **Auth → Configuration**, agregar el valor de `NEON_ORIGIN` a los orígenes confiables.

### 3.3 Crear el primer usuario

Neon Auth administra las cuentas; no se crean por SQL:

```bash
curl -X POST 'https://TU-ENDPOINT.neonauth.REGION.aws.neon.tech/neondb/auth/sign-up/email' \
  -H 'Content-Type: application/json' \
  -H 'Origin: https://paipay.espol.edu.ec' \
  -d '{"email":"productor@paipayales.ec","password":"${PAIPAY_PASSWORD}",
       "name":"Productor Paipayales","callbackURL":"https://paipay.espol.edu.ec"}'
```

### 3.4 Configurar el proyecto Android

1. Abrir la carpeta `PaipayDataLogger` con Android Studio (*Open*, no *Import*).
2. Copiar `local.properties.example` como `local.properties` y completar:

```properties
NEON_DATA_API_URL=https://ep-xxxxx.apirest.us-east-2.aws.neon.tech/neondb/rest/v1/
NEON_AUTH_URL=https://ep-xxxxx.neonauth.us-east-2.aws.neon.tech/neondb/auth/
NEON_ORIGIN=https://paipay.espol.edu.ec
```

3. *File → Sync Project with Gradle Files*.
   Android Studio descargará Gradle 8.9 y generará `gradlew` / `gradle-wrapper.jar`
   la primera vez (el binario del wrapper no viaja en el repositorio).
4. *Run* ▶ en un dispositivo o emulador con Android 7.0 o superior.

> `local.properties` está en `.gitignore`. Las credenciales entran al código por
> `BuildConfig`, nunca escritas en un `.java`.

---

## 4. Arquitectura

```
ec.edu.espol.paipay.datalogger
│
├── data/
│   ├── local/          Room: entidades, DAOs y base SQLite del teléfono
│   ├── remote/         Retrofit: Neon Auth + Neon Data API, cookies y JWT
│   └── repo/           Repositorios: escritura de registros y sesión
│
├── domain/             Semáforo de alertas: umbrales y diagnósticos (Java puro)
│
├── sync/               Motor de sincronización diferida (manual y automático)
│
├── ui/
│   ├── login/          Credenciales, solo al sincronizar
│   ├── main/           Contenedor con navegación inferior
│   ├── registro/       Formularios: peces, agua, laboratorio
│   ├── semaforo/       Tarjetas de alerta por piscina
│   ├── historial/      Registros guardados y su estado
│   └── sync/           Botón [Sincronizar] y diálogo de confirmación
│
└── util/               Fechas, red, ejecutores, identificadores
```

### Por qué Data API y no JDBC directo

Conectar el teléfono directo a Postgres exigiría empaquetar la contraseña de la base
dentro del APK. Cualquiera puede descompilar un APK. Con la Data API:

- la app solo conoce un JWT temporal (~15 min) del propio productor;
- Postgres aplica **Row Level Security** del lado del servidor;
- si se pierde un teléfono, se revoca esa sesión y nada más.

### Por qué la app no pide login al abrir

Registrar datos en campo no exige cuenta ni señal: la app abre directo al semáforo. Exigir
un ingreso al arrancar contradiría el motivo de existir de la app, porque un productor sin
cobertura no podría ni siquiera anotar lo que acaba de medir.

Las credenciales se piden en el momento de **subir**, que es cuando la base principal
necesita saber quién responde por el dato. Por eso `registrado_por` no se rellena al
guardar sino al sincronizar, con el correo de quien se identificó para esa subida.

### Cómo se logra iniciar sesión una sola vez

Los JWT de Neon caducan en minutos, así que guardarlos no sirve para una app que puede
pasar días sin conexión. Lo que se guarda es la **cookie de sesión** de Neon Auth,
cifrada con AES-256 (`CookieJarPersistente`). El JWT se pide justo antes de sincronizar
(`TokenManager`) y se descarta. El productor escribe su contraseña una vez y no vuelve a
verla, aunque reinicie el teléfono.

---

## 5. Semáforo de Alertas

Especie objetivo: **Vieja Azul** (*Andinoacara rivulatus*).

El semáforo evalúa el **ciclo del nitrógeno** más el pH. En una piscina el nitrógeno
sigue siempre el mismo camino:

```
   los peces excretan       las bacterias           y este
       AMONIO        ──►    lo oxidan a    ──►    a NITRATO
      (tóxico)              NITRITO               (poco tóxico)
                          (muy tóxico)
```

| Parámetro | 🟢 Óptimo | 🟡 Precaución | 🔴 Crítico |
|---|---|---|---|
| pH | 6.5 – 8.5 | 6.0–6.4 y 8.6–9.0 | < 6.0 o > 9.0 |
| Amonio (mg/L) | < 0.5 | 0.5 – 1.0 | > 1.0 |
| Nitrito (mg/L) | < 0.5 | 0.5 – 1.0 | > 1.0 |
| Nitrato (mg/L) | < 50 | 50 – 100 | > 100 |
| Amoníaco libre NH₃ (mg/L) | ≤ 0.02 | 0.02 – 0.05 | > 0.05 |

Hay tres reglas que ningún parámetro leído por separado puede dar:

- **pH × amonio.** Lo que mata no es el amonio total que marca el kit, sino la fracción
  en forma de amoníaco libre (NH₃), que se dispara con el pH: 0.6 % a pH 7, 5 % a pH 8,
  36 % a pH 9. La misma lectura es unas sesenta veces más peligrosa a pH 9 que a pH 7.
- **Estado del ciclo.** Amonio alto con nitrito bajo es una piscina cuyo filtro biológico
  aún no arranca; los dos altos a la vez significa que no da abasto con la carga.
- **Mortalidad.** Si la población estimada cae más de un 10 % entre muestreos se avisa, y
  más de un 25 % es rojo — aunque el agua salga verde el día de la visita, porque el
  productor pudo llegar después del episodio.

*Nota: la temperatura y el oxígeno disuelto ya no se registran. La fórmula del amoníaco
libre asume 25 °C, temperatura representativa del recinto; el error que introduce es
pequeño al lado del efecto del pH, que es el que domina.*

El estado global de una piscina es el **peor** de sus parámetros, y las piscinas en rojo
se muestran primero.

Los umbrales están centralizados como constantes en `EvaluadorSemaforo`; el equipo de
Acuicultura (FIMCM) puede ajustarlos ahí sin tocar nada más.

---

## 6. Flujo del botón [Sincronizar]

1. Se cuentan los registros pendientes en SQLite.
2. Se muestra el diálogo:
   > *Antes de sincronizar, asegúrate de tener una conexión de Internet estable.
   > Se van a subir los siguientes datos a la Base de Datos principal:*
   > - 12 registros de biometría de peces (peso y talla)
   > - 8 mediciones de calidad de agua (pH, amonio, nitrito, nitrato)
   > - 3 ensayos de laboratorio
3. Solo tras confirmar, se suben por lotes de 50.
4. Cada lote se marca como sincronizado **únicamente** si el servidor respondió 2xx.

La subida es **idempotente**: el `POST` lleva `Prefer: resolution=merge-duplicates`, que
hace UPSERT sobre la columna `uuid`. Volver a sincronizar tras una caída de señal no
duplica filas.

Si un lote falla, los demás siguen intentándose y se informa un resultado parcial.

---

## 7. Decisiones de UI/UX

La marca Paipay se aplicó según su instructivo: marrón `#433116` y verdes `#95B53D` /
`#AEBD38`, con los grises como color secundario.

Decisiones tomadas pensando en productores con baja alfabetización digital:

- **Botones de 60 dp y texto de 18 sp** — se usan al sol, en el borde de la piscina, a veces con las manos mojadas.
- **La fecha nunca se escribe**, se elige en un calendario.
- **La piscina nunca se escribe**, se elige de una lista. Evita que la base termine con "P1", "p-1" y "Piscina 1" como si fueran tres piscinas.
- **Vista previa del semáforo mientras se escribe** — el productor ve el color antes de guardar; el dato deja de ser un número y se vuelve una decisión.
- **Factor de condición en vivo** en el formulario de peces, con interpretación en palabras.
- **Cada registro muestra "Pendiente" o "Sincronizado"** — nadie tiene que confiar a ciegas en que sus datos llegaron.
- **Los mensajes de error dicen qué hacer**, no qué falló.
- **Se mide un pez a la vez.** Al guardar, el formulario conserva fecha y piscina y queda listo para el siguiente; la confirmación es el contador del muestreo subiendo, no un diálogo que corte el ritmo.
- **Cualquier registro se puede corregir** desde el historial, incluso si ya se subió: vuelve a quedar pendiente y la siguiente sincronización actualiza la fila en Neon en vez de duplicarla.
- **El semáforo es la pantalla de inicio.** Lo primero al abrir la app es saber si alguna piscina está en rojo.

---

## 8. Base de datos

Local (teléfono, Room/SQLite, **versión 2**):
`registro_biometria` · `registro_agua` · `ensayo_laboratorio` · `piscina`

Remota (Neon/PostgreSQL): las mismas tablas más `perfil_productor`, con RLS activo.

En biometría, **una fila es un pez**. Los peces medidos el mismo día forman un muestreo
identificado por `codigo_muestreo` (`M-04082026`). Ese código lo calcula Postgres como
columna generada a partir de `fecha_muestreo`, y la app lo deriva igual, así que no pueden
discrepar.

Vistas de análisis listas para el trabajo de Ciencia de Datos:

| Vista | Para qué sirve |
|---|---|
| `v_semaforo_actual` | Estado vigente por piscina, recalculado en el servidor |
| `v_crecimiento_mensual` | Evolución de peso, talla y K de Fulton, **con desviación estándar** |
| `v_muestreo_biometria` | Un renglón por muestreo: peces medidos, mínimo, máximo y dispersión |
| `v_consolidado_fuentes` | **Cruce campo + agua + laboratorio en una sola fila** |
| `v_desfase_sincronizacion` | Cuántas horas tarda un dato en llegar del campo al servidor |

La desviación estándar solo es calculable desde que cada pez es una fila: es la razón de
fondo del cambio. Dos piscinas con el mismo peso medio pero distinta dispersión están en
situaciones muy diferentes — mucha dispersión suele indicar competencia por el alimento o
siembra desigual.

---

## 9. Pendientes conocidos

- Los logotipos ya son los oficiales (`images/Paipay-logo.png` y `images/Paipay-letra.png`,
  exportados a `res/drawable-*/`). Se entregaron en PNG; si más adelante aparece el SVG
  de FADCOM, conviene reimportarlo como Vector Asset para que escale sin pérdida.
- La tipografía Studio Gothic Alternative no se incluye por licencia; la app usa la
  tipográfica del sistema. Si el equipo consigue la licencia, va en `res/font/`.
- La sincronización aún no tiene pruebas. `EvaluadorSemaforo` sí está cubierto
  (`EvaluadorSemaforoTest`, 13 casos sobre los bordes de cada umbral); lo que falta es
  `SincronizacionRepositorio`, que necesita un servidor HTTP simulado y una base Room
  en memoria.
