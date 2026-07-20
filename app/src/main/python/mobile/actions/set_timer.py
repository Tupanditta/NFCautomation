# Implementación de la acción Temporizador
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from android.provider import AlarmClock
from android.content import Intent
import logging

logger = logging.getLogger(__name__)

class SetTimer(Action):
    def __init__(self, seconds, message="NFC Automation"):
        self.seconds = int(seconds)
        self.message = message

    def execute(self):
        logger.info(f"Configurando temporizador de {self.seconds} segundos")
        try:
            context = get_context()
            intent = Intent(AlarmClock.ACTION_SET_TIMER)
            intent.putExtra(AlarmClock.EXTRA_LENGTH, self.seconds)
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, self.message)
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, True) # Intenta saltar la UI si es posible
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
        except Exception as e:
            logger.error(f"Error al iniciar el temporizador: {e}")
            raise e
