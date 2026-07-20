package com.example.nfcautomation.exceptions

/**
 * Clase base para todas las excepciones relacionadas con la lectura NFC 
 * en la capa de Android.
 */
open class NfcException(message: String) : Exception(message)

/**
 * Lanzada cuando el dispositivo físico no cuenta con hardware NFC.
 */
class NfcHardwareNotFoundException(message: String) : NfcException(message)

/**
 * Lanzada cuando el NFC está disponible pero desactivado en los ajustes del sistema.
 */
class NfcDisabledException(message: String) : NfcException(message)

/**
 * Lanzada cuando ocurre un error técnico durante la lectura física del Tag.
 */
class TagReadException(message: String) : NfcException(message)

/**
 * Lanzada cuando el puente de Chaquopy falla al ejecutar el motor Python.
 */
class PythonExecutionException(message: String) : NfcException(message)
