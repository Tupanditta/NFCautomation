# Este módulo analiza la lista de resultados de ejecución y reporta mediante el logger
import logging
from mobile.utils.translator import translate

logger = logging.getLogger(__name__)

def analize_errors(errors_list):
    """
    Analiza la lista de excepciones devuelta por el Dispatcher.
    """
    if not isinstance(errors_list, list):
        return

    if not errors_list:
        logger.info(translate("success", "logs"))
    else:
        for error in errors_list:
            # Obtenemos el nombre de la clase de excepción
            error_type = type(error).__name__
            friendly_type = translate(error_type, "errors")
            logger.warning(f"[{friendly_type}] {error}")
