#Creo una clase abstracta para obligar a toda acción contener una estructura específica

from abc import ABC, abstractmethod
#NOTA: ABC es una clase y sin embargo abstractmethod una función

class Action(ABC): #con esto indico que la clase Action es solo una plantilla (una clase abstracta)
  """
  Esta clase indica que cualquier clase 
  que herede de Action debe implementar
  una función llamada execute()
  """
  @abstractmethod
  def execute(self):
    pass