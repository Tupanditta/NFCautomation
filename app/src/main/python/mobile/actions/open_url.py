# Implementación de la acción de abrir URL de forma universal

from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from android.content import Intent
from android.net import Uri
import logging

logger = logging.getLogger(__name__)

class OpenUrl(Action): 

  def __init__(self, url):
    self.url = url

  def execute(self):
    logger.info(f"Abriendo recurso: {self.url}")
    try:
        context = get_context()
        # Intent.ACTION_VIEW es el estándar para abrir cualquier recurso (Web, Geo, Tel, etc)
        intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(self.url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        # Lanzamos directamente el Intent. Si no hay app que lo maneje, fallará en el bloque except.
        # Eliminamos resolveActivity para evitar bloqueos por visibilidad de paquetes en Android 11+.
        context.startActivity(intent)
            
    except Exception as e:
        logger.error(f"Error al abrir recurso: {e}")
        raise e
