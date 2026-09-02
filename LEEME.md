# Paipay DataLogger Android

Aplicación Android nativa en Java para registrar acuicultura y lombricultura en
comunidades aisladas. La rama `dev` contiene el desarrollo de v1.6-dev.

## Arquitectura

```text
Android (Java)
  ├─ ciclos, formularios, recordatorios y semáforo
  ├─ Room: fuente local y cola offline
  ├─ WorkManager: reintentos cuando vuelve la red
  └─ Retrofit HTTPS
          ↓
Django REST Framework en Railway
          ↓
Neon PostgreSQL
```

Android no conoce credenciales de PostgreSQL, no usa Neon Auth y no escribe directamente en Neon. Django es la autoridad de autenticación, permisos, validaciones, auditoría y transacciones.

## Funcionalidad v1.6-dev

- Primer inicio de sesión online con correo y contraseña administrados por Django.
- Sesión local cifrada para poder abrir y trabajar sin conexión después del primer ingreso.
- Catálogos de piscinas y camas descargados desde Django y almacenados en Room.
- Sesión vinculada al UUID público de una comunidad. Cambiar de cuenta o
  comunidad exige no tener pendientes/conflictos y limpia la caché antes de
  descargar el nuevo tenant.
- Apertura y cierre explícitos de un único ciclo activo por piscina, incluso offline.
- Predicción cacheada por mediana de supervivencia de ciclos anteriores de la
  misma piscina; sin historia no se inventa una cifra.
- Jornadas solo de agua, solo de biometría o mixtas.
- Recordatorios informativos de agua semanal y biometría mensual, con dos días
  de tolerancia; nunca bloquean un registro adicional o tardío.
- Población estimada obligatoria en toda jornada finalizada.
- Bloque de agua: pH, nitrato, nitrito y amoníaco total. El equipo confirmado
  es un API Freshwater Master Test Kit con escala ppm; los cuatro campos aceptan
  escritura numérica manual dentro de los rangos validados por Django.
- Peces anónimos con peso en gramos y longitud total en centímetros.
- Borradores de jornada locales que sobreviven al cierre de la app.
- Movimientos excepcionales: mortalidad, cosecha/venta parcial, traslado,
  escape y ajuste. La siembra corresponde a la apertura del ciclo.
- Edición y anulación lógica offline de jornadas y movimientos propios.
- UUID para reintentos idempotentes y versión del servidor para detectar conflictos HTTP `409`.
- Comparación visible de las versiones local/remota ante un `409`, con decisión
  explícita entre descartar el cambio local o reaplicarlo sobre la versión remota.
- Historial comunitario cacheado; solo el autor puede editar/anular desde la app.
- Semáforo cacheado de la última jornada comunitaria con agua por piscina, sin importar su autor.
- Advertencia de poco espacio y protección de registros pendientes ante almacenamiento lleno.
- Resolución explícita de conflictos de ciclo: adoptar servidor o reintentar solo
  un cierre compatible; las jornadas/movimientos pendientes se reasignan al ciclo
  remoto adoptado para no perder trabajo de campo.
- Estructura de sensores horarios preparada en Django pero deshabilitada; la app
  todavía no registra oxígeno, temperatura ni turbidez.
- Lombricultura offline: selección de cama, apertura/cierre de ciclo, pH del
  suelo manual de 0 a 14 con incrementos de 0,01, conteo real, observaciones,
  historial, edición, anulación auditada y resolución de conflictos `409`.
- Sin recordatorios, movimientos, predicción ni semáforo para lombricultura en
  esta versión. Ensayos de laboratorio permanecen fuera del alcance.

## Modelo local

La base nueva se llama `paipay_datalogger_v14.db`. No reutiliza ni transforma las filas de v1.3 porque se confirmó que no existen datos reales que deban migrarse.

Entidades Room principales:

- `JornadaLocal`: cabecera, agua, población, autor, estado y versión.
- `ObservacionPezLocal`: detalle biométrico anónimo de una jornada.
- `MovimientoLocal`: cambio explícito de población independiente de una jornada.
- `PiscinaLocal`: catálogo de piscinas de peces y su especie permanente.
- `SemaforoLocal`: último resumen comunitario descargado para consulta offline.
- `ConflictoLocal`: instantánea remota y operación local original necesarias para
  resolver un `409` sin perder ninguna de las dos versiones.
- `CicloLocal`: apertura, cierre, versión, estado de sincronización e instantánea
  inmutable de predicción.
- `CamaLocal`: catálogo de camas de la comunidad y referencia al ciclo activo.
- `CicloLombriculturaLocal`: apertura/cierre y conteos reales del ciclo de cama.
- `RegistroLombriculturaLocal`: pH del suelo, conteo, autor, versión y cola offline.

El esquema Room actual es v5. Las migraciones `1 → 2`, `2 → 3`, `3 → 4` y
`4 → 5` conservan los registros existentes; la última añade camas, ciclos y
registros de lombricultura sin alterar las tablas acuícolas. El archivo físico
sigue llamándose `paipay_datalogger_v14.db` para actualizar instalaciones v1.4
sin crear una base paralela ni perder pendientes.

Estados locales relevantes:

- `BORRADOR`: incompleto y nunca enviado.
- `PENDIENTE_CREAR`: completo, debe crearse en Django.
- `PENDIENTE_EDITAR`: corrección con una versión base conocida.
- `PENDIENTE_ANULAR`: anulación lógica por enviar.
- `SINCRONIZADO`: confirmado por Django.
- `CONFLICTO`: Django tiene una versión más reciente; no se sobrescribe automáticamente.
- `ANULADO` / `ANULADO_LOCAL`: registro conservado como histórico, sin participar en análisis.

Room es la fuente visible para los formularios e historial. La interfaz no espera una respuesta de red para guardar.

Al tocar un registro en `CONFLICTO`, la app presenta los valores del teléfono,
los valores del servidor y sus diferencias. “Descartar mi cambio” sustituye la
copia local por la remota. “Reaplicar mi cambio” conserva los datos del teléfono,
actualiza únicamente la versión base y vuelve a poner la operación en cola; si
el servidor cambia otra vez antes del envío, se producirá otro `409`. Un registro
ya anulado en Django no puede reaplicarse porque la anulación es histórica. La
comparación se puede cerrar sin decidir con Atrás o tocando fuera del diálogo.

## Configuración local

Crear `local.properties` en la raíz del repositorio. No debe versionarse:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
API_BASE_URL=https://TU-SERVICIO.up.railway.app/
```

La URL debe terminar en `/`. El valor de ejemplo del proyecto no es un servidor funcional; antes de probar login debe apuntar al despliegue Django real.

Para probar contra Django ejecutándose en la misma computadora que el emulador:

```properties
API_BASE_URL=http://10.0.2.2:PUERTO/
```

`10.0.2.2` es la dirección especial con la que el AVD alcanza al anfitrión. Django
debe escuchar en `0.0.0.0:PUERTO` y aceptar `10.0.2.2` en `ALLOWED_HOSTS`. Solo el
source set `debug` autoriza HTTP hacia ese host; cualquier otro destino HTTP se
rechaza y la variante `release` conserva tráfico en texto claro deshabilitado.
En esta configuración local de depuración la app acepta que Android marque la
red del AVD como no validada si todavía puede alcanzar `10.0.2.2`; producción
continúa exigiendo `NET_CAPABILITY_VALIDATED`.

Requisitos recomendados:

- JDK 17.
- Android SDK Platform 35.
- Android SDK Build Tools compatibles con Android Gradle Plugin 8.11.1.
- No instalar Gradle manualmente: el wrapper versionado descarga y verifica
  Gradle 8.13.

## Dependencias reproducibles

Las versiones directas se fijan de forma exacta en `build.gradle` y
`app/build.gradle`. `app/gradle.lockfile` inmoviliza las resoluciones transitivas,
`gradle/verification-metadata.xml` acepta únicamente los artefactos cuyos hashes
fueron revisados y el wrapper valida también el SHA-256 de Gradle 8.13.

Cuando una actualización sea deliberada, se modifica primero la versión directa
y luego se regeneran los bloqueos y metadatos junto con las verificaciones:

```powershell
.\gradlew.bat --write-locks --write-verification-metadata sha256 testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat --offline testDebugUnitTest lintDebug assembleDebug
```

Antes de versionar se debe revisar el diff de los archivos de bloqueo y confirmar
en documentación oficial que todas las versiones sean finales (`stable`), no
`alpha`, `beta`, `RC` ni instantáneas. `androidx.security:security-crypto:1.1.0`
es la última versión estable, pero sus APIs están deprecadas; su sustitución por
Android Keystore directo requiere una migración de sesión separada y pruebas de
compatibilidad, no un cambio silencioso de dependencia.

## Compilación y pruebas

Con el wrapper incluido:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest
```

El APK de depuración queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Las 45 pruebas JVM cubren semáforo, estados, movimientos, predicción, mapeo del
contrato API, comparaciones de lombricultura y conectividad de depuración. Las pruebas en
`app/src/androidTest` validan Room y la sesión en un dispositivo o emulador; no
se ejecutan con `testDebugUnitTest`.

No se necesita conectar un teléfono para compilar. Las 14 pruebas instrumentadas
se ejecutaron en el AVD `Paipay_API_24` con Android 7/API 24 e incluyen la
migración Room `4 → 5`. Antes del uso de campo sigue siendo obligatoria una
prueba de aceptación en un teléfono físico, especialmente para funcionamiento
offline, almacenamiento, formularios biométricos grandes y reconexión.

También se verificó manualmente en ese AVD el recorrido integrado contra Django
local: login, creación de una jornada biométrica sin red, sincronización, edición
offline, respuesta `409` provocada por una corrección concurrente, comparación
de las versiones y reaplicación sobre la versión remota. La creación omite el
campo `version`; las correcciones sí envían la versión positiva conocida.

## Reglas que no deben romperse

1. No inferir mortalidad comparando cuántos peces se midieron en dos jornadas.
2. No cambiar la autoría local al sincronizar con otra cuenta.
3. No sincronizar borradores incompletos.
4. No borrar pendientes para liberar espacio.
5. No sobrescribir un conflicto de versión sin intervención del usuario.
6. No agregar especie a cada pez o movimiento; la especie pertenece permanentemente a la piscina.
7. No activar la ingestión de temperatura, oxígeno o turbidez hasta conocer el
   hardware y aprobar su aprovisionamiento; laboratorio sigue fuera.
8. No construir SQL con datos externos. Room debe recibirlos mediante parámetros
   DAO y toda escritura remota debe atravesar los serializers/ORM de Django.
9. No convertir una cama en piscina ni guardar lombrices como peces: sus ciclos y
   registros son agregados independientes.
10. No cambiar de comunidad con pendientes o conflictos. Sin pendientes, limpiar
    Room antes de descargar piscinas y camas de la nueva comunidad.

El diseño funcional y las razones de arquitectura se mantienen en `../decisiones.md`. El contrato del servidor está en `../PaiPayTech_Django/API.md`.

Para compartir el APK de esta versión se usa una copia fuera del repositorio:
`../distribucion/PaiPayTech-1.6.0-dev-railway.apk`. El archivo de compilación dentro
de `app/build/` y todo `*.apk` están ignorados por Git.

La copia actual pesa `9.844.952` bytes, usa firma debug con APK Signature Scheme
v2 y tiene SHA-256
`312343F28FCCE135B78A7764A75E9370A8E22319D5F59225A350846DC6AAAC56`.
Debe desplegarse primero el backend `1.6-dev`; una app nueva contra un backend
anterior no encontrará el catálogo de camas requerido durante el login.
