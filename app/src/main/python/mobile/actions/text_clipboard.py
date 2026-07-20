#Contiene la clase TextClipboard

from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context, ClipData
import logging

logger = logging.getLogger(__name__)

class TextClipboard(Action): 
  def __init__(self, text="NFC Automation"):
    self.text = text

  def execute(self):
    logger.info(f"Copiando texto al portapapeles: {self.text}")
    try:
        clipboard = get_system_service(Context.CLIPBOARD_SERVICE)
        clip = ClipData.newPlainText("NFC Automation", self.text)
        clipboard.setPrimaryClip(clip)
    except Exception as e:
        logger.error(f"Error al copiar al portapapeles: {e}")
