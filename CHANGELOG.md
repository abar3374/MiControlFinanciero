# Registro de cambios

Formato: cada versión lista lo agregado, cambiado y corregido. La más reciente va arriba.

## [0.9.0] - 2026-11-28 · Fase 9: actualizaciones

### Agregado
- Perfil → Actualizaciones: "Buscar actualización" consulta SOLO el repositorio oficial en GitHub.
  Canales Estable y Pruebas (beta). Muestra versión, notas y tamaño.
- Descarga con progreso y verificaciones antes de instalar: enlace oficial por HTTPS, huella
  SHA-256, mismo paquete, versión mayor a la instalada (nunca baja de versión) y misma firma que
  la app instalada (certificado oficial de Finance). Si algo no cuadra, se borra el archivo.
- Instalación con la ventana de Android (el usuario confirma); sin instalación silenciosa.
- Permisos nuevos: INTERNET (solo para actualizaciones) y REQUEST_INSTALL_PACKAGES. Solo HTTPS.
- CI: al subir la etiqueta vX.Y.Z se publica la versión estable firmada con su .sha256.
- 4 pruebas de selección de versiones, enlaces y huellas.

## [0.8.0] - 2026-11-28 · Fase 8: seguridad y respaldo

### Agregado
- Perfil → "Bloqueo, respaldo e historial":
  - Bloqueo automático configurable: siempre, 1, 5 o 15 minutos fuera de la app.
  - Ocultar pantalla (activado por defecto): impide capturas y oculta el contenido en recientes.
  - Respaldo cifrado (AES-256-GCM, clave derivada de tu contraseña con PBKDF2 210.000 iteraciones):
    todos los datos, la foto y los comprobantes en un archivo .fbk que guardas donde quieras.
  - Restaurar: muestra qué contiene el respaldo antes de reemplazar; se hace en una sola
    transacción (si falla, tus datos no cambian). Respaldos de versiones más nuevas se rechazan.
  - Historial de cambios (auditoría) de solo lectura: últimos 200.
- 3 pruebas del cifrado de respaldos (contraseña errada y archivo alterado se rechazan).

### Corregido
- Volver de la cámara, el selector de archivos o un comprobante abierto ya no bloquea la app
  a mitad del proceso (se perdía el archivo elegido).

## [0.7.0] - 2026-11-28 · Fase 7: reportes

### Agregado
- Pestaña Reportes con datos reales: día, semana, mes, trimestre, semestre y año, con flechas para
  ir al periodo anterior o siguiente.
- Filtros: categoría, clasificación, método de pago y gastos fijos u ocasionales (fijo = viene de un
  pago fijo o de un rubro fijo).
- Gráficas dibujadas en el teléfono: evolución de gastos e ingresos, por categoría, presupuesto vs
  real (mes), fijos vs ocasionales, por clasificación, por método de pago y comparativo de 6 meses.
  Se indica la categoría que más creció frente al periodo anterior.
- Exportar a PDF y a Excel (.xlsx con hojas Resumen, Por categoría, Movimientos y Comparativo meses),
  guardado donde el usuario elija. Sin librerías externas.
- 4 pruebas nuevas (periodos, totales, filtros y archivo Excel).

## [0.6.0] - 2026-11-28 · Fase 6: recordatorios

### Agregado
- Recordatorios de pagos fijos y cuotas: 7, 3, 1 días antes y el mismo día (configurables, más un
  día personalizado) y aviso de vencido. Hora del aviso configurable. Nunca se repite un aviso.
- Notificaciones privadas en la pantalla de bloqueo; al tocarlas abre Pagos del mes.
- Perfil: sección Recordatorios con permiso de notificaciones (Android 13+).
- Pruebas del planificador de recordatorios.

## [0.5.0] - 2026-11-28 · Fase 5: créditos e ingresos esperados

### Agregado
- Pagos fijos cada 1, 2, 3, 6 o 12 meses; créditos con entidad, tasa y valor original, y resumen
  de saldo estimado (cuotas restantes × cuota) con historial.
- Ingresos esperados del mes (salario y otros) con "Recibido" / "Deshacer".
- Comprobantes (foto o PDF) adjuntos a los gastos, guardados dentro de la app.
- Base de datos v4 con migración automática.

## [0.4.0] - 2026-11-24 · Pagos del mes y categorías

### Agregado
- Pagos del mes: pagos fijos (arriendo, servicios) y créditos con cuotas (como Sistecrédito).
  Cada mes vuelven a aparecer como pendientes; "Pagar" crea el gasto (concepto, categoría, rubro)
  y lo marca pagado; "Deshacer pago" anula ese gasto. Estados: pendiente, vence pronto (5 días),
  vencido y pagado. La fecha nunca marca un pago como hecho.
- Créditos: total de cuotas y cuotas ya pagadas; muestra "cuota X de N" y, al pagar la última,
  queda saldado y deja de aparecer desde el mes siguiente. El historial y los gastos se conservan.
- Base de datos v3 (migración automática desde v2): fixed_payment y fixed_payment_record.
- "+ Nueva categoría" dentro de los formularios de gasto, rubro y pago fijo.
- Presupuesto: renombrar, ocultar y volver a mostrar categorías (ocultar no borra gastos).
- Inicio: tarjeta "Pagos del mes" con lo que falta por pagar. Accesos desde Registrar y Presupuesto.
- 6 pruebas nuevas del calendario de pagos.

### Cambiado
- Eliminar un gasto que vino de un pago fijo deja ese mes otra vez pendiente.

## [0.3.0] - 2026-11-24 · Fase 4: presupuestos, gastos e ingresos

### Agregado
- Base de datos v2 con migración automática desde v1 (el perfil se conserva): categorías, rubros,
  gastos, ingresos y registro de auditoría.
- 10 categorías predeterminadas y categorías personalizables.
- Rubros: nombre, categoría, valor mensual, fijo u ocasional, clasificación, fecha de inicio, activo,
  descripción y observaciones. Editar, desactivar o eliminar (los gastos se conservan sin rubro).
- Registrar gasto: valor, "¿En qué gastaste?" obligatorio, lugar, categoría, rubro asignado
  automáticamente, clasificación, método de pago, fecha y hora, observaciones.
- Alertas al guardar un gasto cuando el rubro llega a 50, 75, 90 o 100 %.
- Registrar ingreso: valor, origen obligatorio con atajos (Salario…), fijo o variable, dónde se recibió.
- Movimientos: historial por mes con búsqueda por concepto, lugar o categoría y filtro por tipo;
  tocar un movimiento lo abre para editar o eliminar (con confirmación y auditoría).
- Presupuesto: presupuesto global del mes, restante, gastos sin rubro y avance de cada rubro.
- Inicio: disponible del presupuesto, ingresos vs. gastos y "te queda", gastos por categoría,
  actividad reciente y accesos rápidos a gasto e ingreso.
- 7 pruebas nuevas (validación de formularios y periodos mensuales).

### Pendiente de la Fase 4
- Adjuntar comprobante (foto o archivo) a un gasto.

## [0.2.0] - 2026-11-24 · Perfil, seguridad y Registrar

### Agregado
- Base de datos Room cifrada con SQLCipher; su clave se protege con el Android Keystore (versión 1 del esquema).
- Perfil: nombre, apellidos, celular (Colombia, 10 dígitos), correo opcional y foto (selector de fotos, sin permisos).
- Usuario y contraseña para entrar a la app; la contraseña se guarda como hash PBKDF2-SHA256 con sal.
- Desbloqueo con huella (BiometricPrompt, biometría fuerte), activable desde Perfil.
- Pantalla de bloqueo al abrir la app y al volver tras 1 minuto fuera; espera creciente tras 5 intentos fallidos.
- Registrar: tarjetas "Registrar gasto" y "Registrar ingreso" (los formularios llegan en la Fase 4).
- Inicio saluda con el nombre del perfil.
- Firma fija de RELEASE: clave cifrada en `signing/` y contraseña en GitHub Secrets.
- 9 pruebas nuevas (validación del perfil y hash de contraseñas).

## [0.1.0] - 2026-11-24 · Fase 1: arquitectura

### Agregado
- Proyecto Android modular: `app`, 9 módulos `core` y 7 módulos `feature`.
- Tema visual Finance (paleta blanco, gris claro, azul, azul oscuro y gris medio).
- Barra inferior con Inicio, Presupuesto, Registrar (botón central), Reportes y Perfil.
- Inicio con saludo según la hora y fecha en español (zona America/Bogota).
- Perfil con la configuración regional inicial (Colombia, COP, `$1.234.567,89`) y versión instalada.
- Pantallas con estado vacío que explican en qué fase llega cada función.
- `Money` (centavos en `Long`) y `MoneyFormat` con pruebas unitarias.
- `BudgetMath`: porcentaje consumido, presupuesto restante, balance ingresos − gastos y umbrales 50/75/90/100 %.
- Clasificaciones: frecuencia, naturaleza, métodos de pago, tipo de ingreso (fijo / variable).
- Copias de seguridad automáticas de Android desactivadas para proteger los datos.
- GitHub Actions: compilación, pruebas, lint y APK de desarrollo descargable.
