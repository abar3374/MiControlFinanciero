# Finance (MiControlFinanciero)

Aplicación Android para controlar gastos e ingresos personales: presupuestos por rubro,
gastos fijos y ocasionales, créditos y cuotas, recordatorios de pago y reportes de
"¿en qué se fue la plata?". Todo funciona sin Internet y los datos no salen del teléfono
(la app solo se conecta a GitHub cuando el usuario toca "Buscar actualización").

- **Manual de uso:** [docs/MANUAL.md](docs/MANUAL.md)
- **Cómo publicar una versión:** [docs/PUBLICAR.md](docs/PUBLICAR.md)
- **Descargar:** [Releases](https://github.com/abar3374/MiControlFinanciero/releases) (la versión estable
  más reciente está marcada como *Latest*).

- **Paquete:** `com.micontrolfinanciero.app`
- **Android mínimo:** 8.0 (API 26)
- **Tecnología:** Kotlin, Jetpack Compose, Material 3, Hilt, Room + SQLCipher, WorkManager, Coroutines/Flow
- **Estado:** ver [PROJECT_STATUS.md](PROJECT_STATUS.md)

## Cómo abrir y ejecutar el proyecto

La guía completa, paso a paso, está en [docs/FASE_1.md](docs/FASE_1.md). Resumen:

1. Instala [Android Studio](https://developer.android.com/studio) (versión estable más reciente).
2. *File › Open* y elige la carpeta `MiControlFinanciero`.
3. Espera a que termine "Gradle Sync" (la primera vez descarga varios cientos de MB).
4. Elige un emulador o tu teléfono y presiona **Run ▶**.

Desde la terminal:

```bash
./gradlew assembleDebug          # compila el APK de desarrollo
./gradlew :core:model:test :core:domain:test testDebugUnitTest   # pruebas
```

El APK queda en `app/build/outputs/apk/debug/`.

## Estructura

```
app/                 Actividad principal, navegación, barra inferior, Hilt, WorkManager
core/model           Modelos puros: Money (centavos), MoneyFormat, clasificaciones, ajustes
core/domain          Reglas de negocio puras y probadas: presupuesto, validaciones, calendario de
                     pagos, recordatorios, reportes, Excel, cifrado de respaldos, actualizaciones
core/designsystem    Colores, tipografía, componentes Compose y gráficas (Canvas)
core/database        Room + SQLCipher (base de datos cifrada), migraciones automáticas
core/data            Repositorios: Room, archivos privados, respaldo y auditoría
core/security        Clave de la base de datos en Android Keystore, huella (BiometricPrompt)
core/notifications   Recordatorios de pago con WorkManager
core/updates         Actualizaciones verificadas desde GitHub Releases
core/reports         Exportación de reportes a PDF
feature/dashboard    Inicio
feature/budget       Presupuesto, rubros y categorías
feature/expense      Registrar gasto o ingreso, comprobantes, historial
feature/credit       Pagos del mes, pagos fijos, créditos e ingresos esperados
feature/reports      Reportes con filtros, gráficas y exportación
feature/profile      Perfil, recordatorios, seguridad, respaldo, auditoría, actualizaciones
feature/lock         Pantalla de bloqueo (contraseña o huella)
```

## Reglas del proyecto

- El dinero se guarda como `Long` en centavos. Nunca `Float` ni `Double`.
- Todo gasto debe decir **en qué** se gastó (campo obligatorio), también en "Otros".
- Una obligación o un ingreso esperado no es un pago ni un ingreso recibido: el usuario confirma.
- Una compra a crédito no es un gasto; cada cuota pagada sí lo es.
- No se suben al repositorio claves, contraseñas, tokens ni certificados.
- Ninguna información financiera se envía a servidores externos.

## Seguridad

Si encuentras un problema de seguridad, no lo publiques como *issue*: escríbele
directamente al dueño del repositorio.
