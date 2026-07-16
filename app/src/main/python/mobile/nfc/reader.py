#Creo una clase abstracta que lee un NFC, para ello contiene la función que lo lee
from abc import ABC, abstractmethod

class NFCReader(ABC):

  @abstractmethod
  def read_uid(self):
    pass