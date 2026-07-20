# Implementación de la acción Punto de Acceso (Hotspot)
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from android.content import Intent
import logging

logger = logging.getLogger(__name__)

class ToggleHotspot(Action):
    def execute(self):
        # En Android 14, abrimos la pantalla de ajustes para que el usuario lo active
        logger.info("Abriendo ajustes de Punto de acceso (Hotspot)")
        try:
            context = get_context()
            intent = Intent()
            # Acción estándar para ajustes de tethering/hotspot
            intent.setAction("android.settings.TETHER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
        except Exception as e:
            logger.error(f"Error al abrir ajustes de Hotspot: {e}")
            # Fallback a los ajustes generales si el específico falla
            try:
                intent.setAction("android.settings.SETTINGS")
                context.startActivity(intent)
            except:
                raise e
