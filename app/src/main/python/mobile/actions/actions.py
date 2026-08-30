# Implementación de la clase base para las acciones del sistema

from abc import ABC, abstractmethod

class Action(ABC):
  """
  Clase base abstracta para todas las acciones.
  Obliga a implementar execute() y permite definir un resumen para la UI.
  """
  @abstractmethod
  def execute(self):
    pass

  def get_summary(self):
    """Retorna un resumen amigable de la acción para la UI."""
    return None
