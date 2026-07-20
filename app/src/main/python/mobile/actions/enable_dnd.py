# Implementación de la acción No Molestar (DND)
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service, get_context
from mobile.exceptions.action_errors import PermissionRequiredError
from android.content import Context, Intent
from android.app import NotificationManager
from android.provider import Settings
import logging

logger = logging.getLogger(__name__)

class EnableDnd(Action):
    def __init__(self, enabled=True):
        self.enabled = bool(enabled)

    def execute(self):
        state_str = "activado" if self.enabled else "desactivado"
        logger.info(f"Estableciendo modo No Molestar: {state_str}")
        
        try:
            context = get_context()
            nm = get_system_service(Context.NOTIFICATION_SERVICE)
            
            # Verificar si tenemos el permiso especial de Acceso a No molestar
            if not nm.isNotificationPolicyAccessGranted():
                logger.info("Permiso denegado. Abriendo ajustes de 'Acceso a No molestar'...")
                
                # Intentar abrir la pantalla de ajustes para que el usuario conceda el permiso
                intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                raise PermissionRequiredError("Se requiere permiso para 'Acceso a No molestar'. Actívalo en la pantalla que se acaba de abrir.")
            
            # Filtros: PRIORITY (on) o ALL (off)
            new_filter = NotificationManager.INTERRUPTION_FILTER_PRIORITY if self.enabled else NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(new_filter)
            
        except PermissionRequiredError as e:
            logger.warning(str(e))
            raise e
        except Exception as e:
            logger.error(f"Error al cambiar modo No Molestar: {e}")
            raise e
