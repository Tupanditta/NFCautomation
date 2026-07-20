# Implementación de la acción para detener cualquier vibración activa
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context
import logging

logger = logging.getLogger(__name__)

class DesactivateVibration(Action): 
    def execute(self):
        logger.info("Deteniendo todos los efectos de vibración")
        try:
            vibrator = get_system_service(Context.VIBRATOR_SERVICE)
            # cancel() detiene cualquier vibración iniciada por la app (vibrate o waveform)
            vibrator.cancel()
        except Exception as e:
            logger.error(f"Fallo al desactivar la vibración: {e}")
            raise e
