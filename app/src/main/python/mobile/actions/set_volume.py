#Modulo con la clase SetVolume modularizado para diferentes canales de audio

from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context
from android.media import AudioManager
import logging

logger = logging.getLogger(__name__)

class SetVolume(Action):
  """
  Establece el volumen de un canal específico (música, notificación, etc).
  Nivel esperado: 0-100.
  """
  
  def __init__(self, level, stream="music"):
    self.level = int(level)
    self.stream_name = stream.lower()

  def execute(self):
    # Mapeo de nombres de stream a constantes de Android
    STREAM_MAP = {
        "music": AudioManager.STREAM_MUSIC,
        "notification": AudioManager.STREAM_NOTIFICATION,
        "ring": AudioManager.STREAM_RING,
        "alarm": AudioManager.STREAM_ALARM
    }
    
    stream_type = STREAM_MAP.get(self.stream_name, AudioManager.STREAM_MUSIC)
    logger.info(f"Estableciendo volumen de '{self.stream_name}' al {self.level}%")
    
    try:
        am = get_system_service(Context.AUDIO_SERVICE)
        max_vol = am.getStreamMaxVolume(stream_type)
        target_vol = (self.level * max_vol) // 100
        
        try:
            # Intentamos aplicar el volumen solicitado
            am.setStreamVolume(stream_type, target_vol, 0)
        except Exception:
            # Si falla al poner nivel 0 (común en notificaciones/llamadas sin permisos de No Molestar)
            # intentamos poner el nivel mínimo (1) para "silenciar" lo máximo posible.
            if target_vol == 0 and stream_type in [AudioManager.STREAM_RING, AudioManager.STREAM_NOTIFICATION]:
                logger.info("El sistema bloqueó el nivel 0. Aplicando nivel 1 (casi silencio).")
                am.setStreamVolume(stream_type, 1, 0)
            else:
                raise

    except Exception as e:
        logger.error(f"Error al establecer volumen de {self.stream_name}: {e}")
