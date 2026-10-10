package com.graphics.juego.conduccion;

/** Decide si un vehiculo puede pasar de (xAntes, zAntes) a (x, z). */
@FunctionalInterface
public interface Obstaculos {
    boolean libre(float xAntes, float zAntes, float x, float z);
}
