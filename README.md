# WAINK — fait par math_0235

Scanner de voitures façon GTA 6, avec viseur caméra en direct.
- **Mode GTA** : vrai modèle et vrai prix (WAINK IA), verrouillage/alarme/traceur simulés pour le fun.
- **Mode Réel** : modèle, année estimée, fiche technique, prix d'occasion ; scan du compteur pour lire les km ; vérification du numéro de châssis.

WAINK IA tourne avec Gemini (gratuit) : crée une clé sur aistudio.google.com/apikey.

## Obtenir l'APK
1. Crée un dépôt GitHub (ex. `waink`) et envoie tout le contenu de ce dossier (y compris le dossier caché `.github`).
2. (Optionnel) Settings → Secrets and variables → Actions → nouveau secret `GEMINI_API_KEY` avec ta clé : elle sera intégrée à l'APK.
   Sinon, colle la clé dans les réglages de l'appli (roue dentée).
3. Onglet Actions → « Construire l'APK » → télécharge l'artefact WAINK-apk et installe WAINK.apk.
