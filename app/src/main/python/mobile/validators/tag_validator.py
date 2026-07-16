#Se encarga de comprobar si el archivo tags.json es válido y sigue la estructura esperada
from mobile.exceptions import ConfigurationError

def validate_tags_dict(tags_dict):

  for key, value in tags_dict.items():
    if isinstance(key, str) and isinstance(value, str):
      continue
    else:
      raise ConfigurationError(f"Key or Value is not str: {key, value}")
  
  return True