# Implementación de la acción Abrir Temporizador
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from android.provider import AlarmClock
from android.content import Intent
import logging

logger = logging.getLogger(__name__)

class OpenTimer(Action):
    def execute(self):
        logger.info("Abriendo temporizador del reloj")
        try:
            context = get_context()
            # ACTION_SHOW_TIMERS abre la pestaña de temporizadores
            intent = Intent(AlarmClock.ACTION_SHOW_TIMERS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
        except Exception as e:
            logger.error(f"Error al abrir el temporizador: {e}")
            # Fallback a ACTION_SET_TIMER si el anterior no está disponible
            try:
                intent = Intent(AlarmClock.ACTION_SET_TIMER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            except:
                raise e

    def get_summary(self):
        return "Abrir Temporizador"
