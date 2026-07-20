# Este módulo analiza la lista de resultados de ejecución y reporta mediante el logger
import logging

logger = logging.getLogger(__name__)

def analize_errors(errors_list):
    """
    Analiza la lista de excepciones devuelta por el Dispatcher.
    """
    if not isinstance(errors_list, list):
        return

    if not errors_list:
        logger.info("Ejecución finalizada sin errores en las acciones")
    else:
        for error in errors_list:
            # Obtenemos el nombre de la clase de excepción para un reporte más profesional
            error_type = type(error).__name__
            logger.warning(f"[{error_type}] {error}")
