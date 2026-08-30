# Implementación de la acción Linterna (Flashlight)
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context
import logging

logger = logging.getLogger(__name__)

# Variable global para persistir el estado entre ejecuciones del bridge (si el proceso sigue vivo)
_flashlight_on = False

class ToggleFlashlight(Action):
    def __init__(self, enabled=None):
        self.enabled = enabled

    def execute(self):
        global _flashlight_on
        try:
            camera_manager = get_system_service(Context.CAMERA_SERVICE)
            camera_id = camera_manager.getCameraIdList()[0]
            
            # Si no se especifica, conmutamos
            if self.enabled is None:
                new_state = not _flashlight_on
            else:
                new_state = bool(self.enabled)
            
            camera_manager.setTorchMode(camera_id, new_state)
            _flashlight_on = new_state
            
            state_str = "encendida" if new_state else "apagada"
            logger.info(f"Linterna {state_str}")
            
        except Exception as e:
            logger.error(f"Error al controlar la linterna: {e}")
            raise e

    def get_summary(self):
        if self.enabled is True: return "Encender Linterna"
        if self.enabled is False: return "Apagar Linterna"
        return "Conmutar Linterna"
