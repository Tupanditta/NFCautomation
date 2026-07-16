#Creo una clase que simula la lectura de un NFC
from mobile.nfc.reader import NFCReader

class NFCSimulator(NFCReader):

  def read_uid(self):
    return "04AABBCCDD11"