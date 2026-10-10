package com.graphics.juego.conduccion;

/**
 * Lo que el conductor pide en este cuadro. Separarlo del teclado permite
 * probar la fisica del auto sin abrir una ventana.
 *
 * @param acelerador +1 avanzar, -1 frenar / reversa, 0 nada
 * @param direccion  +1 girar a la izquierda, -1 a la derecha, 0 recto
 * @param freno      freno fuerte (Espacio)
 */
public record ControlesAuto(float acelerador, float direccion, boolean freno) {
    public static final ControlesAuto NINGUNO = new ControlesAuto(0, 0, false);
}
