package com.graphics.juego.conduccion;

/**
 * Chata (remolque de un eje) enganchada al auto del jugador. No usa OpenGL.
 *
 * Geometria (vista desde arriba, el frente es -Z local como en el auto):
 *
 *     [ AUTO ]---o=====[ CHATA ]
 *        C     P   L     A
 *     C = centro del auto (punto sobre el que gira el auto en este juego)
 *     P = enganche, a ENGANCHE (h) unidades detras de C
 *     A = eje de la chata, a LANZA (L) unidades detras de P
 *
 * Modelo cinematico estandar de un remolque de un eje con enganche desplazado:
 *
 *     d(angChata)/dt = (v / L) * sin(angAuto - angChata) - (h * w / L) * cos(angAuto - angChata)
 *
 *   v = velocidad del auto (positiva hacia delante), w = velocidad de giro del auto (rad/s).
 * Se obtiene de exigir que la rueda de la chata no derrape: el eje A solo se mueve en la
 * direccion de la chata. Si el auto se desliza por una pared aparece ademas el termino
 * (vLateral / L) * cos(...), que tambien se incluye.
 *
 * Se integra con deltaTime en pasos de como maximo 1/240 s, por eso el resultado es
 * practicamente igual a 30 o a 144 FPS. Despues de integrar el angulo, la posicion del eje
 * se recalcula desde el enganche: la lanza siempre queda pegada al auto.
 */
public class Chata {

    /** h: distancia del centro del auto al enganche (detras del parachoques trasero). */
    public static final float ENGANCHE = 1.5f;
    /** L: distancia del enganche al eje de la chata (largo de la lanza + media caja). */
    public static final float LANZA = 1.6f;
    /** Angulo relativo maximo entre auto y chata (unos 75 grados). */
    public static final float ANGULO_MAXIMO = (float) Math.toRadians(75);
    /** Dos circulos de colision sobre el eje de la chata, a +-0.4 del eje, de radio 0.95. */
    public static final float RADIO = 0.95f;
    public static final float SEPARACION_CIRCULOS = 0.4f;
    private static final float PASO_MAXIMO = 1 / 240f;

    /** Estado de la chata: angulo (misma convencion que el auto) y posicion del eje. */
    public record Estado(float angulo, float ejeX, float ejeZ) {

        /** Centros de los dos circulos de colision: {x0, z0, x1, z1}. */
        public float[] circulos() {
            float fx = -(float) Math.sin(angulo);
            float fz = -(float) Math.cos(angulo);
            return new float[] {
                    ejeX + fx * SEPARACION_CIRCULOS, ejeZ + fz * SEPARACION_CIRCULOS,
                    ejeX - fx * SEPARACION_CIRCULOS, ejeZ - fz * SEPARACION_CIRCULOS };
        }
    }

    public float angulo;
    public float ejeX;
    public float ejeZ;
    private boolean enganchada = true;

    public Chata(Auto auto) {
        alinear(auto);
    }

    // ===================== Estado =====================

    public boolean enganchada() {
        return enganchada;
    }

    public void desenganchar() {
        enganchada = false;
    }

    /** Engancha la chata alineada detras del auto. */
    public void enganchar(Auto auto) {
        enganchada = true;
        alinear(auto);
    }

    /** Coloca la chata recta detras del auto (inicio, tecla R y al enganchar). */
    public void alinear(Auto auto) {
        aplicar(colocar(auto.x, auto.z, auto.angulo, auto.angulo));
    }

    public Estado estado() {
        return new Estado(angulo, ejeX, ejeZ);
    }

    public void aplicar(Estado e) {
        angulo = e.angulo();
        ejeX = e.ejeX();
        ejeZ = e.ejeZ();
    }

    /** Diferencia de angulos llevada al intervalo (-PI, PI]. */
    public static float diferencia(float a, float b) {
        return (float) Math.IEEEremainder(a - b, Math.PI * 2);
    }

    /** Angulo relativo actual auto - chata. */
    public float anguloRelativo(Auto auto) {
        return diferencia(auto.angulo, angulo);
    }

    /** Posicion del enganche en el mundo para un auto en (x, z) con angulo a: {x, z}. */
    public static float[] enganche(float x, float z, float a) {
        // detras del auto = -frente = (sin a, cos a)
        return new float[] { x + (float) Math.sin(a) * ENGANCHE, z + (float) Math.cos(a) * ENGANCHE };
    }

    /** Restriccion geometrica: el eje queda a L del enganche en la direccion de la chata. */
    private static Estado colocar(float autoX, float autoZ, float anguloAuto, float anguloChata) {
        float[] p = enganche(autoX, autoZ, anguloAuto);
        float ejeX = p[0] + (float) Math.sin(anguloChata) * LANZA;
        float ejeZ = p[1] + (float) Math.cos(anguloChata) * LANZA;
        return new Estado(anguloChata, ejeX, ejeZ);
    }

    // ===================== Fisica de remolque =====================

    /**
     * Calcula (sin modificar la chata) el estado que tendria si el auto pasa de
     * (xAntes, zAntes, anguloAntes) a (x, z, anguloAuto) durante deltaTime segundos.
     */
    public Estado simular(float deltaTime, float xAntes, float zAntes, float anguloAntes,
            float x, float z, float anguloAuto) {
        if (deltaTime <= 0) {
            return colocar(x, z, anguloAuto, angulo);
        }
        // Movimiento del auto en este cuadro (se supone uniforme dentro del cuadro)
        float velX = (x - xAntes) / deltaTime;
        float velZ = (z - zAntes) / deltaTime;
        float giroTotal = diferencia(anguloAuto, anguloAntes);
        float w = giroTotal / deltaTime; // velocidad de giro del auto (rad/s)

        int pasos = Math.max(1, (int) Math.ceil(deltaTime / PASO_MAXIMO));
        float paso = deltaTime / pasos;
        float anguloChata = angulo;
        for (int i = 0; i < pasos; i++) {
            float t = (i + 0.5f) / pasos;
            float a = anguloAntes + giroTotal * t;
            // Velocidad del auto en sus propios ejes: hacia delante (v) y hacia su izquierda
            float frenteX = -(float) Math.sin(a);
            float frenteZ = -(float) Math.cos(a);
            float izquierdaX = -(float) Math.cos(a);
            float izquierdaZ = (float) Math.sin(a);
            float v = velX * frenteX + velZ * frenteZ;
            float vLateral = velX * izquierdaX + velZ * izquierdaZ;

            float relativo = diferencia(a, anguloChata);
            float giroChata = (v / LANZA) * (float) Math.sin(relativo)
                    - (ENGANCHE * w / LANZA) * (float) Math.cos(relativo)
                    + (vLateral / LANZA) * (float) Math.cos(relativo);
            anguloChata += giroChata * paso;
            anguloChata = limitar(a, anguloChata);
        }
        anguloChata = limitar(anguloAuto, anguloChata);
        return colocar(x, z, anguloAuto, anguloChata);
    }

    /** Impide que la chata se cruce: |auto - chata| <= ANGULO_MAXIMO. */
    private static float limitar(float anguloAuto, float anguloChata) {
        float relativo = diferencia(anguloAuto, anguloChata);
        if (relativo > ANGULO_MAXIMO) {
            return anguloAuto - ANGULO_MAXIMO;
        }
        if (relativo < -ANGULO_MAXIMO) {
            return anguloAuto + ANGULO_MAXIMO;
        }
        return anguloChata;
    }

    // ===================== Conduccion del conjunto auto + chata =====================

    /** true si los dos circulos de la chata pueden pasar de "antes" a "despues". */
    public static boolean cabe(Estado antes, Estado despues, Obstaculos obstaculos) {
        float[] a = antes.circulos();
        float[] d = despues.circulos();
        return obstaculos.libre(a[0], a[1], d[0], d[1]) && obstaculos.libre(a[2], a[3], d[2], d[3]);
    }

    /**
     * Mueve el auto y arrastra la chata. Cada movimiento que prueba el auto (avance, o
     * deslizar por X o por Z) solo se acepta si tambien la chata cabe en su nueva posicion.
     * Si al final la chata no cabe (por ejemplo, el auto giro estando bloqueado), el conjunto
     * vuelve a la pose anterior y se detiene: nunca atraviesa nada.
     *
     * @param obstaculosAuto  colisiones del auto (circulo de radio Auto.RADIO)
     * @param obstaculosChata colisiones de cada circulo de la chata (radio Chata.RADIO)
     */
    public void conducir(float deltaTime, Auto auto, ControlesAuto controles,
            Obstaculos obstaculosAuto, Obstaculos obstaculosChata) {
        if (!enganchada) {
            auto.actualizar(deltaTime, controles, obstaculosAuto);
            return;
        }
        float xAntes = auto.x;
        float zAntes = auto.z;
        float anguloAntes = auto.angulo;
        Estado actual = estado();
        auto.actualizar(deltaTime, controles, (xa, za, x, z) -> obstaculosAuto.libre(xa, za, x, z)
                && cabe(actual, simular(deltaTime, xAntes, zAntes, anguloAntes, x, z, auto.angulo), obstaculosChata));
        Estado nuevo = simular(deltaTime, xAntes, zAntes, anguloAntes, auto.x, auto.z, auto.angulo);
        if (cabe(actual, nuevo, obstaculosChata)) {
            aplicar(nuevo);
        } else {
            auto.x = xAntes;
            auto.z = zAntes;
            auto.angulo = anguloAntes;
            auto.velocidad = 0;
        }
    }
}
