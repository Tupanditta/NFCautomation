# Se encarga de comprobar si el archivo workflows.json es válido y sigue la estructura esperada
import logging
from mobile.exceptions import ConfigurationError

logger = logging.getLogger(__name__)

def validate_workflows_dict(workflows_dict):
  """
  Función para comprobar que el diccionario creado a partir del 
  workflow.json es correcto estructuralmente.
  """
  if not workflows_dict:
    raise ConfigurationError("El diccionario de workflows está vacío")

  for workflow_id, workflow in workflows_dict.items():

    if not isinstance(workflow_id, str):
      raise ConfigurationError(f"Error en el valor de workflow_id: {workflow_id}")

    if not isinstance(workflow, dict):
      raise ConfigurationError(f"Error en el valor del workflow: {workflow}")

    if "mobile_actions" not in workflow:
      raise ConfigurationError(f"Falta la llave 'mobile_actions' en el workflow: {workflow_id}")

    mobile_actions = workflow["mobile_actions"]
    if not isinstance(mobile_actions, list):
      raise ConfigurationError(f"El valor de 'mobile_actions' debe ser una lista en: {workflow_id}")

    for action in mobile_actions:

      if not isinstance(action, dict):
        raise ConfigurationError(f"Error en el valor de la acción: {action}")

      if "action" not in action:
        raise ConfigurationError(f"Falta la llave 'action' en: {action}")

      if not isinstance(action["action"], str):
        raise ConfigurationError(f"El nombre de la acción debe ser un string: {action}")

      if "params" in action:
        if not isinstance(action["params"], dict):
          raise ConfigurationError(f"Los parámetros ('params') deben ser un diccionario en: {action}")

  logger.info("Validación de workflows: OK")
  return True #devuelvo True si todo está correctamente