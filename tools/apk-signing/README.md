# Signing the APK the phones install

Every APK that reaches a phone must carry the **permanent key**:
`C:\Users\Security\.ncm-secrets\ncm-android-signing.p12` (password in
`ncm-android-signing-password.txt` beside it), certificate SHA-256
`caa0f998c71b9ba1aadd071726c04f44578524a83854ec1d03a669735db15559`.

Android will not install an APK over one signed with a different key. The only
way past is to uninstall first, which wipes the saved PIN on that phone.

## Why this is done by hand

CI signs with the runner's debug key, which is generated fresh on every runner —
so every CI build carries a different key. The GitHub token cannot write Actions
secrets, so the permanent key cannot be given to CI. The workflow therefore
publishes its build to the **`ci-build`** release, never to `latest-apk`.

## Steps

1. Push a change under `android/`. CI builds and parks the APK on `ci-build`.
2. Download it, then re-sign and verify in one go:

   ```
   python apksign.py ci.apk NCM-x.y.apk C:\Users\Security\.ncm-secrets\ncm-android-signing.p12 C:\Users\Security\.ncm-secrets\ncm-android-signing-password.txt
   ```

   It prints the certificate it signed with and the one it read back, and `OK`.
3. Check the fingerprint independently: `python apkcert.py NCM-x.y.apk`
4. Install on one phone first. Only then publish it to `latest-apk`.

`apksign.py` writes APK Signature Scheme v2 (enough for minSdk 24 and
targetSdk 35). Its verifier was checked against APKs signed by Google's own
`apksigner`: it recomputes the same content digest and accepts their signature.
It needs the Python `cryptography` package.
