#Este módulo tiene el único trabajo de declarar e implementar la clase ActionRegistry

#importación de todas las clases (acciones-actions)
#Más adelante voy a usar las clases como valores, para ello debo importarlas
from mobile.actions.open_app import OpenApp
from mobile.actions.enable_dnd import EnableDnd
from mobile.actions.show_notification import ShowNotification
from mobile.actions.set_volume import SetVolume
from mobile.actions.activate_vibration import ActivateVibration
from mobile.actions.vibrate import Vibrate
from mobile.actions.log_event import LogEvent
from mobile.actions.desactivate_vibration import DesactivateVibration
from mobile.actions.text_clipboard import TextClipboard
from mobile.actions.open_url import OpenUrl

class ActionRegistry:
  """
  Esta clase se usa únicamente para pasar de un string 
  a una clase que podré usar para ejecutar una acción
  """

  def __init__(self): #Se ejecuta únicamente al llamar a la clase
    """
    Creo un diccionario que relaciona todas las clases 
    con sus respectivos strings
    """
    self.actions = { 
      "open_app" : OpenApp,
      "open_url": OpenUrl,
      "enable_dnd": EnableDnd,
      "set_volume": SetVolume,
      "show_notification": ShowNotification,
      "vibrate": Vibrate,
      "activate_vibration": ActivateVibration,
      "log_event": LogEvent,
      "desactivate_vibration": DesactivateVibration,
      "text_clipboard": TextClipboard
    }
  
  def get_action(self, action_name): 
    """
    Esta función interna de la clase se encarga de 
    hayar y devolver la clase correspondiente a 
    la acción (string) que introduzco
    """
    return self.actions.get(action_name) #me devuelve la clase