#Implemento la acción de abrir aplicación mediante alias predefinidos

from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from mobile.exceptions.action_errors import AppNotFoundError
import logging

logger = logging.getLogger(__name__)

# Diccionario de alias comunes. Solo estas aplicaciones podrán ser abiertas.
APP_ALIASES = {
    "spotify": "com.spotify.music",
    "youtube": "com.google.android.youtube",
    "whatsapp": "com.whatsapp",
    "instagram": "com.instagram.android",
    "maps": "com.google.android.apps.maps",
    "gmail": "com.google.android.gm",
    "chrome": "com.android.chrome",
    "calculadora": "com.google.android.calculator",
    "reloj": "com.google.android.deskclock",
    "ajustes": "com.android.settings"
}

class OpenApp(Action): 

  def __init__(self, alias):
    # Ahora el parámetro se trata estrictamente como un alias
    self.alias = alias.lower()

  def execute(self):
    logger.info(f"Intentando abrir aplicación por alias: {self.alias}")
    try:
        context = get_context()
        pm = context.getPackageManager()
        
        # 1. Intentar resolver por Alias (estricto)
        target_package = APP_ALIASES.get(self.alias)
        
        if target_package is None:
            raise AppNotFoundError(f"El alias '{self.alias}' no está registrado en el sistema.")
        
        # 2. Obtener el Intent de lanzamiento para el paquete asociado al alias
        intent = pm.getLaunchIntentForPackage(target_package)
        
        # 3. Ejecutar si se encontró el intent de lanzamiento
        if intent is not None:
            intent.addFlags(268435456) # Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            logger.info(f"Aplicación '{self.alias}' ({target_package}) abierta con éxito")
        else:
            raise AppNotFoundError(f"La aplicación para el alias '{self.alias}' no parece estar instalada.")

    except AppNotFoundError as e:
        logger.warning(str(e))
        raise e
    except Exception as e:
        logger.error(f"Error al abrir aplicación: {e}")
        raise e

  def get_summary(self):
      return f"Abrir {self.alias.capitalize()}"
