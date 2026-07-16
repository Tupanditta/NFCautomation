#Se encarga de comprobar si el archivo workflows.json es válido y sigue la estructura esperada

from mobile.exceptions import ConfigurationError

def validate_workflows_dict(workflows_dict):
  """
  Función para comprobar que el diccionario
  creado a partir del worflow.json es correcto
  estructuralmente, según la estructura deseada
  """
  for workflow_id, workflow in workflows_dict.items():

    if not isinstance(workflow_id, str):
      raise ConfigurationError(f"Workflow_id value error: {workflow_id}")

    if not isinstance(workflow, dict):
      raise ConfigurationError(f"Workflow value error: {workflow}")

    if "mobile_actions" not in workflow:
      raise ConfigurationError(f"Mobile actions error")

    mobile_actions = workflow["mobile_actions"]
    if not isinstance(mobile_actions, list):
      raise ConfigurationError(f"Mobile actions value error")

    for action in mobile_actions:

      if not isinstance(action, dict):
        raise ConfigurationError(f"Action value error: {action}")

      if "action" not in action:
        raise ConfigurationError(f"Missing 'action' key in: {action}")

      if not isinstance(action["action"], str):
        raise ConfigurationError(f"Action name must be str: {action}")

      if "params" in action:

        if not isinstance(action["params"], dict):
          raise ConfigurationError(f"Params must be dict: {action}")

  return True #devuelvo True si todo está correctamente