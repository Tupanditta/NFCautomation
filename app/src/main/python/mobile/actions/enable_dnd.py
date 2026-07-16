#Implemento la acción de...

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class EnableDnd(Action):

  def execute(self):
    logger.info(
      "Modo No Molestar activado"
    )