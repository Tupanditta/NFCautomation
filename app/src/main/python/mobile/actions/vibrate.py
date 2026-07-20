#Contiene la clase Vibrate

from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context
from android.os import VibrationEffect
import logging

logger = logging.getLogger(__name__)

class Vibrate(Action):

  def __init__(self, duration):
    self.duration = duration

  def execute(self):
    logger.info(f"Vibrando durante {self.duration} ms")
    try:
        vibrator = get_system_service(Context.VIBRATOR_SERVICE)
        if vibrator.hasVibrator():
            effect = VibrationEffect.createOneShot(self.duration, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
    except Exception as e:
        logger.error(f"Error al vibrar: {e}")
