package es.alimentarse.cuenta;

/** Una cuenta se ha dado de baja: se le confirma por correo tras el commit (D-020, D-064). */
public record CuentaEliminada(String nombre, String email) {}
