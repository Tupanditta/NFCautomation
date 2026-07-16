#NOTA: En python las excepciones son clases, donde la clase base es Exception

from .configuration_errors import ConfigurationError

from .registry_errors import (
  RegistryError,
  UnknownTagError,
  WorkflowNotFoundError
)