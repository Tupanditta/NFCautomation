#Contiene la clase TextClipboard
#NOTA: este consigue compartir texto al portapapeles

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class TextClipboard(Action): 
  def execute(self):
    logger.info(
      "Texto copiado al portapapeles"
    )