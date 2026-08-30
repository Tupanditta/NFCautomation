import json
import os

_translations = {}
_current_lang = "es"

def load_translations(config_dir):
    global _translations
    path = os.path.join(config_dir, "translations.json")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            _translations = json.load(f)

def set_current_language(lang):
    global _current_lang
    _current_lang = lang

def translate(key, category="logs", *args):
    """
    Traduce una clave usando el idioma actual.
    Soporta formateo de strings para los logs.
    """
    try:
        text = _translations.get(_current_lang, {}).get(category, {}).get(key, key)
        if args:
            return text.format(*args)
        return text
    except Exception:
        return key
