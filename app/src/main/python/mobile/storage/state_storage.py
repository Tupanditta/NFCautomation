import json
import os
import logging

logger = logging.getLogger(__name__)

def load_states(config_path):
    """Carga el archivo de estados."""
    if not os.path.exists(config_path):
        return {}
    with open(config_path, "r", encoding="utf-8") as f:
        return json.load(f)

def save_states(config_path, states):
    """Guarda el diccionario de estados en el JSON."""
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(states, f, indent=4)
    logger.info("Estados actualizados correctamente")
