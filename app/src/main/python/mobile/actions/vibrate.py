# Implementación de la acción Vibrate compatible con Android 14
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context
from android.os import VibrationEffect, VibrationAttributes
from java import jlong
import logging

logger = logging.getLogger(__name__)

class Vibrate(Action):

  def __init__(self, duration):
    try:
        self.duration = int(duration)
    except (ValueError, TypeError):
        self.duration = 500

  def execute(self):
    logger.info(f"Vibrando durante {self.duration} ms")
    try:
        vibrator = get_system_service(Context.VIBRATOR_SERVICE)
        if vibrator.hasVibrator():
            # Atributos para que la vibración se considere Alarma y atraviese el No Molestar
            attrs = VibrationAttributes.Builder() \
                .setUsage(VibrationAttributes.USAGE_ALARM) \
                .build()
                
            effect = VibrationEffect.createOneShot(jlong(self.duration), VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect, attrs)
    except Exception as e:
        logger.error(f"Error al vibrar: {e}")

  def get_summary(self):
    return "Vibrar"
