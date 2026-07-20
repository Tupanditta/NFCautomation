# Se encarga de comprobar si el archivo tags.json es válido y sigue la estructura esperada
import logging
from mobile.exceptions import ConfigurationError

logger = logging.getLogger(__name__)

def validate_tags_dict(tags_dict):
  """
  Comprueba que el diccionario de tags no esté vacío y que 
  tanto las llaves como los valores sean strings.
  """
  if not tags_dict:
    raise ConfigurationError("El diccionario de tags está vacío")

  for key, value in tags_dict.items():
    if not isinstance(key, str) or not isinstance(value, str):
      raise ConfigurationError(f"Llave o valor no es un string: {key, value}")
  
  logger.info("Validación de tags: OK")
  return True