# Implementación de la acción Abrir Alarmas
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from android.provider import AlarmClock
from android.content import Intent
import logging

logger = logging.getLogger(__name__)

class OpenAlarm(Action):
    def execute(self):
        logger.info("Abriendo alarmas del reloj")
        try:
            context = get_context()
            # ACTION_SHOW_ALARMS abre la pestaña de alarmas
            intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
        except Exception as e:
            logger.error(f"Error al abrir las alarmas: {e}")
            raise e

    def get_summary(self):
        return "Abrir Alarma"
