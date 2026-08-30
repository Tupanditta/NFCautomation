#Este módulo tiene el único trabajo de declarar e implementar la clase ActionRegistry

#importación de todas las clases (acciones-actions)
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
# Nuevas acciones
from mobile.actions.toggle_flashlight import ToggleFlashlight
from mobile.actions.set_timer import SetTimer
from mobile.actions.toggle_hotspot import ToggleHotspot
from mobile.actions.track_time import TrackTime

class ActionRegistry:
  """
  Esta clase se utiliza para mapear nombres de acciones (strings) a sus 
  respectivas clases de implementación.
  """

  def __init__(self):
    """
    Inicializa el diccionario que relaciona los nombres de las acciones 
    con las clases Action correspondientes.
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
      "text_clipboard": TextClipboard,
      "toggle_flashlight": ToggleFlashlight,
      "set_timer": SetTimer,
      "toggle_hotspot": ToggleHotspot,
      "track_time": TrackTime
    }
  
  def get_action(self, action_name): 
    """
    Busca y devuelve la clase de acción correspondiente al nombre proporcionado.
    """
    return self.actions.get(action_name)
