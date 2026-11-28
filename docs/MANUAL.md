# Manual de uso de Finance

Finance te ayuda a saber **en qué se va tu plata**. Todo queda guardado y cifrado en tu
teléfono; la app no envía tu información a ningún servidor.

---

## 1. Instalar

1. En el teléfono, abre <https://github.com/abar3374/MiControlFinanciero/releases>.
2. Descarga el archivo `finance-<versión>-<número>.apk` de la versión marcada como **Latest**.
3. Ábrelo. Si Android lo pide, permite "Instalar apps desconocidas" para tu navegador.
4. Toca **Instalar**.

Las versiones siguientes se instalan **encima** de la anterior y conservan tus datos
(todas van firmadas con la misma clave).

## 2. Primeros pasos

1. **Perfil → Datos personales:** nombre, apellidos, celular y foto (opcional).
2. **Perfil → Usuario y contraseña:** crea tu usuario y una contraseña. Desde ese momento
   la app pide la contraseña al abrir.
3. **Perfil → Huella:** actívala para entrar con tu huella. La contraseña sigue sirviendo.

> Si olvidas la contraseña no hay forma de recuperarla (los datos están cifrados).
> Por eso conviene tener un **respaldo** (sección 9).

## 3. Presupuesto

- **Presupuesto → Nuevo rubro:** nombre, categoría, valor mensual, fijo u ocasional y
  clasificación (necesidad, obligación, ocio, ahorro…).
- La suma de los rubros activos es tu **presupuesto del mes**.
- **Categorías:** puedes crear, renombrar u ocultar categorías. Ocultar no borra los gastos.

## 4. Registrar gastos e ingresos

**Registrar → Registrar gasto**

- **¿En qué?** es obligatorio, también en "Otros". Ejemplo: *Sistecrédito – cuota noviembre*.
- Valor, fecha y hora, categoría, rubro, método de pago y observaciones.
- **Comprobante:** foto o PDF de la factura (se guarda dentro de la app).
- Al acercarte al tope de un rubro verás avisos al 50 %, 75 %, 90 % y 100 %.

**Registrar → Registrar ingreso:** salario (fijo) u otros ingresos (variables).

**Registrar → Ver movimientos:** historial con búsqueda y filtros. Puedes editar o eliminar;
cada cambio queda en el historial de auditoría.

## 5. Pagos e ingresos del mes

**Registrar → Pagos del mes** (o la tarjeta en Inicio).

- **Pagos fijos:** arriendo, servicios, internet… cada 1, 2, 3, 6 o 12 meses, con su día de pago.
- **Créditos:** número de cuotas, cuotas ya pagadas, entidad, tasa y valor original.
  Verás "cuota X de N" y el saldo estimado. Al pagar la última cuota queda **saldado** y deja de
  aparecer; su historial se conserva.
- **Pagar** crea el gasto del mes y lo marca pagado. **Deshacer pago** anula ese gasto.
- La fecha de vencimiento **nunca** marca algo como pagado: tú confirmas.
- Estados: pendiente, vence pronto (5 días), vencido y pagado.
- **Ingresos esperados** (salario, arriendo que recibes…): toca **Recibido** cuando llegue la plata.
- Para pagos quincenales crea dos pagos fijos (día 15 y día 30).

## 6. Recordatorios

**Perfil → Recordatorios**

- Avisos 7, 3 y 1 día antes y el mismo día (puedes quitar o agregar un día personalizado).
- Elige la hora del aviso. Si un pago se vence sin marcarlo, recibes un aviso de "vencido".
- En Android 13 o más nuevo debes permitir las notificaciones.
- En la pantalla de bloqueo el aviso no muestra montos. Al tocarlo se abre Pagos del mes.

## 7. Reportes

**Reportes**

- Periodo: día, semana, mes, trimestre, semestre o año. Usa las flechas para moverte.
- **Filtros:** categoría, clasificación, método de pago y fijos u ocasionales.
- Gráficas: evolución, por categoría, presupuesto vs real (mes), fijos vs ocasionales,
  por clasificación, por método de pago y comparativo de 6 meses.
- **Exportar PDF o Excel:** eliges dónde guardar el archivo. El archivo exportado **no** va
  cifrado: compártelo con cuidado.

## 8. Seguridad

**Perfil → Bloqueo, respaldo e historial**

- **Bloqueo automático:** siempre, 1, 5 o 15 minutos fuera de la app.
- **Ocultar pantalla** (activado): impide capturas y oculta el contenido en "recientes".
- **Historial de cambios:** lista de lo que se creó, modificó o eliminó (solo lectura).

## 9. Respaldo y cambio de teléfono

1. **Crear respaldo:** escribe una contraseña para el respaldo (mínimo 8 caracteres) y elige
   dónde guardar el archivo `.fbk` (por ejemplo, Google Drive o una memoria).
2. Guarda también la contraseña del respaldo: **sin ella no se puede abrir**.
3. **Restaurar** (en el teléfono nuevo, después de instalar Finance): elige el archivo `.fbk`,
   escribe su contraseña, revisa la fecha y cantidad de datos y confirma.
   Se reemplaza todo, incluidos usuario y contraseña de la app.

El respaldo va cifrado con AES-256; incluye datos, foto y comprobantes.

## 10. Actualizaciones

**Perfil → Actualizaciones**

- **Canal Estable:** versiones probadas. **Canal Pruebas:** versiones nuevas antes de ser estables.
- **Buscar actualización → Descargar → Instalar.** Antes de instalar, Finance comprueba que el
  archivo viene del repositorio oficial, que su huella SHA-256 es correcta y que está firmado por
  Finance. Android te pedirá confirmar; la primera vez te pedirá permitir que Finance instale apps.
- Nunca se instala una versión anterior a la que tienes.

## 11. Preguntas frecuentes

- **¿Por qué "Restante" no es lo mismo que mi saldo del banco?** Porque es presupuesto menos lo
  gastado en tus rubros; la app no se conecta a tu banco.
- **Compré algo a crédito, ¿lo registro como gasto?** No. Crea un **crédito** en Pagos del mes;
  cada cuota que pagues sí será un gasto.
- **¿Puedo borrar un gasto?** Sí, se anula y queda en el historial de auditoría. Si venía de un
  pago fijo, ese mes vuelve a quedar pendiente.
- **¿La app usa internet?** Solo cuando buscas o descargas una actualización.
