# WAINK — fait par math_0235

Scanner de voitures façon GTA 6, avec viseur caméra en direct.
- **Mode GTA** : vrai modèle et vrai prix (WAINK IA), verrouillage/alarme/traceur simulés pour le fun.
- **Mode Réel** : modèle, année estimée, fiche technique, prix d'occasion ; scan du compteur pour lire les km ; vérification du numéro de châssis.

## Sécurité de la clé Gemini
La clé n'est **jamais** dans le code ni dans l'APK. Elle reste enfermée dans le coffre-fort Vercel (`api/scan.js`),
qui n'accepte que les scans WAINK (voiture, compteur, prix), avec une limite de scans.

## Installer
Lien direct de la dernière version : https://github.com/matheo7-frz/waink/releases/latest/download/WAINK.apk
