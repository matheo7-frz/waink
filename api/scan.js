// WAINK — coffre-fort de la clé Gemini (fait par math_0235)
// La clé est dans les variables d'environnement Vercel : elle n'est jamais dans le code ni dans l'APK.
// Le serveur n'accepte que les 3 scans WAINK : voiture, compteur, prix.

const CAR = `Tu es un expert automobile du marché belge. Regarde la voiture principale sur la photo.
Réponds UNIQUEMENT en JSON, en français :
{"voiture":bool,"marque":str,"modele":str,"generation":str,"annee_min":int,"annee_max":int,"annee_probable":int,"carrosserie":str,"moteurs":str,"puissance":str,"couleur":str,"prix_neuf":int,"prix_occasion":int,"confiance":"faible"|"moyenne"|"élevée","remarque":str}
L'année se déduit du design (phase, restylage). prix_occasion en euros pour cette année probable avec un kilométrage moyen. Si ce n'est pas une voiture, "voiture":false et explique dans "remarque".`;
const KM = `Lis le compteur kilométrique (odomètre total, pas le trip) sur cette photo de tableau de bord.
Réponds UNIQUEMENT en JSON : {"lisible":bool,"km":int,"unite":"km"|"miles","remarque":str}`;
const PRICE = (c, y, km) => `Voiture : ${c}, année ${y}, ${km} km, marché belge actuel.
Réponds UNIQUEMENT en JSON : {"prix_min":int,"prix_max":int,"commentaire":str} (prix en euros, occasion entre particuliers).`;

const hits = new Map(); // limite simple : 30 scans / 10 min par adresse
function limited(ip) {
  const now = Date.now(), win = 10 * 60 * 1000;
  const list = (hits.get(ip) || []).filter(t => now - t < win);
  list.push(now); hits.set(ip, list);
  return list.length > 30;
}
const clean = (s, n) => String(s ?? '').replace(/[^\p{L}\p{N} .,'()\-]/gu, '').slice(0, n);

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.status(204).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'POST seulement' });

  const ip = (req.headers['x-forwarded-for'] || '').split(',')[0] || 'x';
  if (limited(ip)) return res.status(429).json({ error: 'Trop de scans, attends un peu' });

  const key = process.env.GEMINI_API_KEY;
  if (!key) return res.status(500).json({ error: 'Clé non configurée sur le serveur' });

  const b = req.body || {};
  let prompt, image = null;
  if (b.type === 'car' || b.type === 'km') {
    if (typeof b.image !== 'string' || b.image.length > 2_000_000 || !/^[A-Za-z0-9+/=]+$/.test(b.image))
      return res.status(400).json({ error: 'Image invalide' });
    prompt = b.type === 'car' ? CAR : KM; image = b.image;
  } else if (b.type === 'price') {
    const y = parseInt(b.year, 10), km = parseInt(b.km, 10);
    prompt = PRICE(clean(b.car, 120), y > 1900 && y < 2100 ? y : 'inconnue', km >= 0 && km < 3_000_000 ? km : 'kilométrage moyen');
  } else return res.status(400).json({ error: 'Type inconnu' });

  const parts = [{ text: prompt }];
  if (image) parts.push({ inline_data: { mime_type: 'image/jpeg', data: image } });
  const model = process.env.GEMINI_MODEL || 'gemini-flash-latest';
  try {
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key },
      body: JSON.stringify({ contents: [{ parts }], generationConfig: { responseMimeType: 'application/json', temperature: 0.2 } })
    });
    if (r.status === 429) return res.status(429).json({ error: 'Quota Gemini atteint' });
    if (!r.ok) return res.status(502).json({ error: 'Gemini a refusé' });
    const j = await r.json();
    const t = (j.candidates?.[0]?.content?.parts || []).map(p => p.text || '').join('').replace(/^```json|```$/g, '').trim();
    return res.status(200).json(JSON.parse(t));
  } catch (e) {
    return res.status(502).json({ error: 'Réponse illisible' });
  }
}
