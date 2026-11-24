# Finance (MiControlFinanciero)

Aplicación Android para controlar gastos e ingresos personales: presupuestos por rubro,
gastos fijos y ocasionales, créditos y cuotas, recordatorios de pago y reportes de
"¿en qué se fue la plata?". Todo funciona sin Internet y los datos no salen del teléfono.

- **Paquete:** `com.micontrolfinanciero.app`
- **Android mínimo:** 8.0 (API 26)
- **Tecnología:** Kotlin, Jetpack Compose, Material 3, Hilt, Room (desde la Fase 3)
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
app/                 Actividad principal, navegación, barra inferior, Hilt
core/model           Modelos puros: Money (centavos), MoneyFormat, clasificaciones
core/domain          Reglas de negocio: BudgetMath (porcentajes, restante, balance)
core/designsystem    Colores, tipografía, formas, íconos y componentes Compose
core/database        Room + SQLCipher (Fase 3)
core/data            Repositorios (Fase 3)
core/security        Keystore, PIN, biometría, respaldo cifrado (Fase 8)
core/notifications   Recordatorios con WorkManager (Fase 6)
core/updates         Actualizaciones desde GitHub Releases (Fase 9)
core/reports         Cálculos y exportación PDF / Excel (Fase 7)
feature/dashboard    Inicio
feature/budget       Presupuesto, rubros, gastos e ingresos fijos
feature/expense      Registrar gasto o ingreso, historial
feature/credit       Créditos y obligaciones (Fase 5)
feature/reports      Reportes
feature/profile      Perfil y ajustes
feature/lock         Bloqueo con PIN / huella (Fase 8)
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
