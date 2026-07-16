
class RegistryError(Exception):
  pass


class UnknownTagError(RegistryError): #El error aparece al consultar registros
  pass


class WorkflowNotFoundError(RegistryError): #El error aparece al consultar registros
  pass