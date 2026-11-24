# Firma de Finance

`finance-release.jks.enc` es la clave de firma de RELEASE, **cifrada** con AES-256
(`openssl enc -aes-256-cbc -pbkdf2 -iter 200000`). Sin la contraseña es inútil, por eso
puede estar en un repositorio público. La clave sin cifrar (`*.jks`) nunca se sube.

- **Contraseña:** vive solo en GitHub Secrets con el nombre `SIGNING_KEY_PASSWORD`
  (y en la copia de respaldo del dueño del proyecto). Es la misma para descifrar el archivo,
  para el almacén y para la clave.
- **Alias:** `finance`
- **Huella SHA-256 del certificado** (la usará el actualizador en la Fase 9 para aceptar solo APK legítimos):

```
CD:56:E0:E1:D9:36:7C:E8:B3:F8:8A:82:9A:2D:0F:83:93:11:D5:B0:C2:BC:DC:07:61:6F:BC:33:E9:0D:73:38
```

**No cambies esta clave.** Android solo deja actualizar una app si la versión nueva está
firmada con la misma clave. Si se pierde, habría que desinstalar la app (y sus datos) para
instalar una versión con otra clave. Guarda la copia de respaldo en dos lugares fuera de línea.

Cómo la usa GitHub Actions: descifra el archivo en una carpeta temporal del servidor,
compila `assembleRelease` con las variables `FINANCE_KEYSTORE_*` y borra el archivo al terminar.
