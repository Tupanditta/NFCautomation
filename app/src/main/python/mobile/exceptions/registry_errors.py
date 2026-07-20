from .base_error import MobileBaseError

class RegistryError(MobileBaseError):
  """Clase base para errores relacionados con los registros (tags, workflows, acciones)."""
  pass


class UnknownTagError(RegistryError):
  """Se lanza cuando un UID NFC no está registrado en tags.json."""
  pass


class WorkflowNotFoundError(RegistryError):
  """Se lanza cuando un workflow id no se encuentra en workflows.json."""
  pass
