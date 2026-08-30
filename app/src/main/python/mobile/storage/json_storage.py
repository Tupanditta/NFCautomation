# Este módulo se encarga de leer los archivos JSON y convertirlos en diccionarios de Python
import json
import logging

logger = logging.getLogger(__name__)

def load_tags(config_path):
  """
  Carga el archivo de tags desde la ruta proporcionada.
  """
  with open(config_path, "r", encoding="utf-8") as f:
    tags = json.load(f)
  
  logger.info("Configuración de tags cargada correctamente")
  return tags

def load_workflows(config_path):
  """
  Carga el archivo de workflows desde la ruta proporcionada.
  """
  with open(config_path, "r", encoding="utf-8") as f:
    workflows = json.load(f)

  logger.info("Configuración de workflows cargada correctamente")
  return workflows

def save_tags(config_path, tags):
    """Guarda el diccionario de tags en el JSON."""
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(tags, f, indent=4)
    logger.info("Tags guardados correctamente")

def save_workflows(config_path, workflows):
    """Guarda el diccionario de workflows en el JSON."""
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(workflows, f, indent=4)
    logger.info("Workflows guardados correctamente")
