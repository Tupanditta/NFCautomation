#Contiene la clase ActivateVibration

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class ActivateVibration(Action): 
  def execute(self):
    logger.info(
      "Activando vibración continua"
    )